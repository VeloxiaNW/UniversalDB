package net.veloxia.universaldb.drivers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.veloxia.universaldb.Repository;
import net.veloxia.universaldb.config.DatabaseConfig.MongoDBConfig;
import net.veloxia.universaldb.query.Condition;
import net.veloxia.universaldb.query.Query;
import net.veloxia.universaldb.query.Sort;
import net.veloxia.universaldb.query.SortDirection;
import net.veloxia.universaldb.util.ColumnMeta;
import net.veloxia.universaldb.util.EntityMeta;
import net.veloxia.universaldb.util.EntityReflection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * MongoDB driver - manages the MongoClient and creates repositories.
 *
 * <pre>{@code
 * MongoDriver driver = new MongoDriver(new MongoDBConfig("myapp"));
 * Repository<User, String> users = driver.repository(User.class);
 * }</pre>
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class MongoDriver implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MongoDriver.class);
    private final MongoDBConfig config;
    private MongoClient client;

    public MongoDriver(MongoDBConfig config) {
        this.config = config;
    }

    private MongoClient getClient() {
        if (client == null) {
            log.info("Connecting to MongoDB: {}/{}", config.getUri(), config.getDatabase());
            client = MongoClients.create(config.getUri());
        }
        return client;
    }

    public <T> MongoRepository<T> repository(Class<T> clazz) {
        EntityMeta meta = EntityReflection.metaFor(clazz);
        MongoCollection<Document> collection = getClient()
                .getDatabase(config.getDatabase())
                .getCollection(meta.getTableName());
        return new MongoRepository<>(clazz, collection);
    }

    @Override
    public void close() {
        if (client != null) client.close();
    }
}

/**
 * MongoDB implementation of {@link Repository}.
 * The ID type is always {@link String} (MongoDB ObjectId hex string).
 */
class MongoRepository<T> implements Repository<T, String> {

    private final Class<T> clazz;
    private final MongoCollection<Document> collection;
    private final EntityMeta meta;

    MongoRepository(Class<T> clazz, MongoCollection<Document> collection) {
        this.clazz      = clazz;
        this.collection = collection;
        this.meta       = EntityReflection.metaFor(clazz);
    }

    @Override
    public T insert(T entity) {
        Document doc = toDocument(entity);
        collection.insertOne(doc);
        String id = doc.getObjectId("_id").toHexString();
        EntityReflection.setValue(entity, meta.getIdField(), id);
        return entity;
    }

    @Override
    public List<T> insertAll(List<T> entities) {
        List<Document> docs = new ArrayList<>();
        for (T e : entities) docs.add(toDocument(e));
        collection.insertMany(docs);
        for (int i = 0; i < entities.size(); i++) {
            String id = docs.get(i).getObjectId("_id").toHexString();
            EntityReflection.setValue(entities.get(i), meta.getIdField(), id);
        }
        return entities;
    }

    @Override
    public Optional<T> findById(String id) {
        Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        return Optional.ofNullable(doc).map(this::fromDocument);
    }

    @Override
    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        collection.find().forEach(doc -> list.add(fromDocument(doc)));
        return list;
    }

    @Override
    public Optional<T> findOne(Query<T> query) {
        Document doc = collection.find(buildFilter(query.getConditions())).limit(1).first();
        return Optional.ofNullable(doc).map(this::fromDocument);
    }

    @Override
    public List<T> findMany(Query<T> query) {
        var cursor = collection.find(buildFilter(query.getConditions()));
        if (!query.getSorts().isEmpty()) {
            Document sortDoc = new Document();
            for (Sort s : query.getSorts())
                sortDoc.append(s.field(), s.direction() == SortDirection.ASC ? 1 : -1);
            cursor = cursor.sort(sortDoc);
        }
        if (query.getLimit()  != null) cursor = cursor.limit(query.getLimit());
        if (query.getOffset() != null) cursor = cursor.skip(query.getOffset());
        List<T> list = new ArrayList<>();
        cursor.forEach(doc -> list.add(fromDocument(doc)));
        return list;
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public long count(Query<T> query) {
        return collection.countDocuments(buildFilter(query.getConditions()));
    }

    @Override
    public T save(T entity) {
        Object id = EntityReflection.getValue(entity, meta.getIdField());
        if (id == null || id.toString().isBlank()) return insert(entity);
        update(entity);
        return entity;
    }

    @Override
    public int update(T entity) {
        String id = Objects.toString(EntityReflection.getValue(entity, meta.getIdField()), null);
        if (id == null) throw new IllegalArgumentException("Cannot update entity without an ID");
        List<Bson> updates = new ArrayList<>();
        for (ColumnMeta col : meta.getColumns())
            updates.add(Updates.set(col.columnName(), EntityReflection.getValue(entity, col.field())));
        var result = collection.updateOne(Filters.eq("_id", new ObjectId(id)), Updates.combine(updates));
        return (int) result.getModifiedCount();
    }

    @Override
    @SafeVarargs
    public final int updateWhere(Query<T> query, Map.Entry<String, Object>... fields) {
        List<Bson> updates = new ArrayList<>();
        for (Map.Entry<String, Object> entry : fields)
            updates.add(Updates.set(entry.getKey(), entry.getValue()));
        var result = collection.updateMany(buildFilter(query.getConditions()), Updates.combine(updates));
        return (int) result.getModifiedCount();
    }

    @Override
    public boolean deleteById(String id) {
        return collection.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
    }

    @Override
    public boolean delete(T entity) {
        Object id = EntityReflection.getValue(entity, meta.getIdField());
        return id != null && deleteById(id.toString());
    }

    @Override
    public int deleteWhere(Query<T> query) {
        return (int) collection.deleteMany(buildFilter(query.getConditions())).getDeletedCount();
    }

    @Override
    public void clear() {
        collection.deleteMany(new Document());
    }

    private Document toDocument(T entity) {
        Document doc = new Document();
        for (ColumnMeta col : meta.getColumns())
            doc.append(col.columnName(), EntityReflection.getValue(entity, col.field()));
        return doc;
    }

    private T fromDocument(Document doc) {
        Map<String, Object> values = new LinkedHashMap<>();
        ObjectId oid = doc.getObjectId("_id");
        values.put(meta.getIdColumnName(), oid != null ? oid.toHexString() : null);
        for (ColumnMeta col : meta.getColumns())
            values.put(col.columnName(), doc.get(col.columnName()));
        return EntityReflection.fromMap(clazz, values);
    }

    @SuppressWarnings("unchecked")
    private Bson buildFilter(List<Condition> conditions) {
        if (conditions.isEmpty()) return new Document();
        List<Bson> filters = new ArrayList<>();
        for (Condition c : conditions) {
            Bson f = switch (c.getOperator()) {
                case EQ         -> Filters.eq(c.getField(), c.getValue());
                case NEQ        -> Filters.ne(c.getField(), c.getValue());
                case GT         -> Filters.gt(c.getField(), (Comparable<?>) c.getValue());
                case GTE        -> Filters.gte(c.getField(), (Comparable<?>) c.getValue());
                case LT         -> Filters.lt(c.getField(), (Comparable<?>) c.getValue());
                case LTE        -> Filters.lte(c.getField(), (Comparable<?>) c.getValue());
                case LIKE       -> Filters.regex(c.getField(),
                                        c.getValue().toString().replace("%", ".*"));
                case IN         -> Filters.in(c.getField(), (Iterable<?>) c.getValue());
                case NOT_IN     -> Filters.nin(c.getField(), (Iterable<?>) c.getValue());
                case IS_NULL    -> Filters.eq(c.getField(), (Object) null);
                case IS_NOT_NULL -> Filters.ne(c.getField(), (Object) null);
            };
            filters.add(f);
        }
        return filters.size() == 1 ? filters.get(0) : Filters.and(filters);
    }
}
