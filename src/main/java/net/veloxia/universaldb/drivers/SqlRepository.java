package net.veloxia.universaldb.drivers;

import net.veloxia.universaldb.Repository;
import net.veloxia.universaldb.query.Condition;
import net.veloxia.universaldb.query.Query;
import net.veloxia.universaldb.query.Sort;
import net.veloxia.universaldb.util.ColumnMeta;
import net.veloxia.universaldb.util.EntityMeta;
import net.veloxia.universaldb.util.EntityReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Base SQL repository - shared logic for SQLite and MySQL.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public abstract class SqlRepository<T, ID> implements Repository<T, ID> {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Class<T> clazz;
    protected final Connection connection;
    protected final EntityMeta meta;

    protected SqlRepository(Class<T> clazz, Connection connection) {
        this.clazz      = clazz;
        this.connection = connection;
        this.meta       = EntityReflection.metaFor(clazz);
    }

    public void createTableIfNotExists() {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(meta.tableName()).append(" (\n  ")
                .append(meta.idColumnName()).append(" ").append(idColumnDef());
        for (ColumnMeta col : meta.columns()) {
            sb.append(",\n  ").append(col.columnName())
              .append(" ").append(sqlTypeFor(col.field().getType()))
              .append(col.nullable() ? "" : " NOT NULL")
              .append(col.unique()   ? " UNIQUE" : "");
        }
        sb.append("\n)");
        String sql = sb.toString();
        log.debug("Creating table: {}", sql);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create table", e);
        }
    }

    /** Subclasses provide the PRIMARY KEY column definition. */
    protected abstract String idColumnDef();

    private String sqlTypeFor(Class<?> type) {
        if (type == int.class || type == Integer.class) return "INTEGER";
        if (type == long.class || type == Long.class)   return "BIGINT";
        if (type == double.class || type == Double.class
         || type == float.class  || type == Float.class) return "REAL";
        if (type == boolean.class || type == Boolean.class) return "BOOLEAN";
        return "TEXT";
    }

    @Override
    public T insert(T entity) {
        List<ColumnMeta> cols = meta.columns();
        String colNames = String.join(", ", cols.stream().map(ColumnMeta::columnName).toList());
        String placeholders = String.join(", ", cols.stream().map(c -> "?").toList());
        String sql = "INSERT INTO " + meta.tableName() + " (" + colNames + ") VALUES (" + placeholders + ")";
        log.debug("Insert: {}", sql);
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < cols.size(); i++)
                stmt.setObject(i + 1, EntityReflection.getValue(entity, cols.get(i).field()));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Object rawId = rs.getObject(1);
                    EntityReflection.setValue(entity, meta.idField(), coerceId(rawId));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
        return entity;
    }

    @Override
    public List<T> insertAll(List<T> entities) {
        List<T> result = new ArrayList<>();
        for (T e : entities) result.add(insert(e));
        return result;
    }

    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + meta.tableName() + " WHERE " + meta.idColumnName() + " = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    @Override
    public List<T> findAll() {
        try (ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + meta.tableName())) {
            return mapAll(rs);
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
    }

    @Override
    public Optional<T> findOne(Query<T> query) {
        query.limit(1);
        List<T> results = findMany(query);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<T> findMany(Query<T> query) {
        WhereClause where = buildWhere(query.getConditions());
        String order = buildOrderBy(query.getSorts());
        String limit  = query.getLimit()  != null ? " LIMIT "  + query.getLimit()  : "";
        String offset = query.getOffset() != null ? " OFFSET " + query.getOffset() : "";
        String sql = "SELECT * FROM " + meta.tableName() + where.sql + order + limit + offset;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < where.params.size(); i++) stmt.setObject(i + 1, where.params.get(i));
            try (ResultSet rs = stmt.executeQuery()) { return mapAll(rs); }
        } catch (SQLException e) {
            throw new RuntimeException("findMany failed", e);
        }
    }

    @Override
    public long count() {
        try (ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM " + meta.tableName())) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("count failed", e);
        }
    }

    @Override
    public long count(Query<T> query) {
        WhereClause where = buildWhere(query.getConditions());
        String sql = "SELECT COUNT(*) FROM " + meta.tableName() + where.sql;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < where.params.size(); i++) stmt.setObject(i + 1, where.params.get(i));
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
        } catch (SQLException e) {
            throw new RuntimeException("count with query failed", e);
        }
    }

    @Override
    public T save(T entity) {
        Object id = EntityReflection.getValue(entity, meta.idField());
        if (id == null || id.equals(0) || id.equals(0L)) return insert(entity);
        update(entity);
        return entity;
    }

    @Override
    public int update(T entity) {
        List<ColumnMeta> cols = meta.columns();
        String sets = String.join(", ", cols.stream().map(c -> c.columnName() + " = ?").toList());
        String sql  = "UPDATE " + meta.tableName() + " SET " + sets + " WHERE " + meta.idColumnName() + " = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < cols.size(); i++)
                stmt.setObject(i + 1, EntityReflection.getValue(entity, cols.get(i).field()));
            stmt.setObject(cols.size() + 1, EntityReflection.getValue(entity, meta.idField()));
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update failed", e);
        }
    }

    @Override
    @SafeVarargs
    public final int updateWhere(Query<T> query, Map.Entry<String, Object>... fields) {
        String sets = String.join(", ", Arrays.stream(fields).map(e -> e.getKey() + " = ?").toList());
        WhereClause where = buildWhere(query.getConditions());
        String sql = "UPDATE " + meta.tableName() + " SET " + sets + where.sql;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int i = 1;
            for (Map.Entry<String, Object> entry : fields) stmt.setObject(i++, entry.getValue());
            for (Object p : where.params) stmt.setObject(i++, p);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateWhere failed", e);
        }
    }

    @Override
    public boolean deleteById(ID id) {
        String sql = "DELETE FROM " + meta.tableName() + " WHERE " + meta.idColumnName() + " = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("deleteById failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean delete(T entity) {
        return deleteById((ID) EntityReflection.getValue(entity, meta.idField()));
    }

    @Override
    public int deleteWhere(Query<T> query) {
        WhereClause where = buildWhere(query.getConditions());
        String sql = "DELETE FROM " + meta.tableName() + where.sql;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < where.params.size(); i++) stmt.setObject(i + 1, where.params.get(i));
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteWhere failed", e);
        }
    }

    @Override
    public void clear() {
        try { connection.createStatement().execute("DELETE FROM " + meta.tableName()); }
        catch (SQLException e) { throw new RuntimeException("clear failed", e); }
    }

    private T mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(meta.idColumnName(), rs.getObject(meta.idColumnName()));
        for (ColumnMeta col : meta.columns())
            values.put(col.columnName(), rs.getObject(col.columnName()));
        return EntityReflection.fromMap(clazz, values);
    }

    private List<T> mapAll(ResultSet rs) throws SQLException {
        List<T> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private WhereClause buildWhere(List<Condition> conditions) {
        if (conditions.isEmpty()) return new WhereClause("", List.of());
        List<Object> params = new ArrayList<>();
        List<String> clauses = new ArrayList<>();
        for (Condition c : conditions) {
            switch (c.getOperator()) {
                case EQ         -> { clauses.add(c.getField() + " = ?");      params.add(c.getValue()); }
                case NEQ        -> { clauses.add(c.getField() + " != ?");     params.add(c.getValue()); }
                case GT         -> { clauses.add(c.getField() + " > ?");      params.add(c.getValue()); }
                case GTE        -> { clauses.add(c.getField() + " >= ?");     params.add(c.getValue()); }
                case LT         -> { clauses.add(c.getField() + " < ?");      params.add(c.getValue()); }
                case LTE        -> { clauses.add(c.getField() + " <= ?");     params.add(c.getValue()); }
                case LIKE       -> { clauses.add(c.getField() + " LIKE ?");   params.add(c.getValue()); }
                case IN         -> {
                    Collection<?> vals = (Collection<?>) c.getValue();
                    clauses.add(c.getField() + " IN (" + "?,".repeat(vals.size()).replaceAll(",$","") + ")");
                    params.addAll(vals);
                }
                case NOT_IN     -> {
                    Collection<?> vals = (Collection<?>) c.getValue();
                    clauses.add(c.getField() + " NOT IN (" + "?,".repeat(vals.size()).replaceAll(",$","") + ")");
                    params.addAll(vals);
                }
                case IS_NULL     -> clauses.add(c.getField() + " IS NULL");
                case IS_NOT_NULL -> clauses.add(c.getField() + " IS NOT NULL");
            }
        }
        return new WhereClause(" WHERE " + String.join(" AND ", clauses), params);
    }

    private String buildOrderBy(List<Sort> sorts) {
        if (sorts.isEmpty()) return "";
        return " ORDER BY " + String.join(", ",
                sorts.stream().map(s -> s.field() + " " + s.direction()).toList());
    }

    protected Object coerceId(Object raw) { return raw; }

    private record WhereClause(String sql, List<Object> params) {}
}
