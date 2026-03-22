package net.veloxia.universaldb;

import net.veloxia.universaldb.query.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unified CRUD + query interface for any entity type.
 *
 * @param <T>  The entity type. Must have a no-arg constructor and an {@code @Id} field.
 * @param <ID> The primary key type ({@code Long} for SQL, {@code String} for MongoDB).
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public interface Repository<T, ID> {

    /** Inserts a new entity and returns it with the generated ID set. */
    T insert(T entity);

    /** Inserts multiple entities in a batch. */
    List<T> insertAll(List<T> entities);

    /** Finds an entity by its primary key. */
    Optional<T> findById(ID id);

    /** Returns all entities in the table/collection. */
    List<T> findAll();

    /** Returns the first entity matching the query, or empty. */
    Optional<T> findOne(Query<T> query);

    /** Returns all entities matching the query. */
    List<T> findMany(Query<T> query);

    /** Counts all entities (no filter). */
    long count();

    /** Counts entities matching the query. */
    long count(Query<T> query);

    /** Returns true if at least one entity matches the query. */
    default boolean exists(Query<T> query) { return count(query) > 0; }

    /**
     * Saves an entity - inserts if ID is null/0, updates otherwise.
     * Returns the entity with a populated ID after insert.
     */
    T save(T entity);

    /**
     * Updates an existing entity. The entity must have its ID set.
     * Returns the number of rows/documents affected.
     */
    int update(T entity);

    /**
     * Updates matching entities with the given field→value pairs.
     *
     * <pre>{@code
     * users.updateWhere(
     *     new Query<User>().where("active").eq(false),
     *     Map.entry("status", "inactive")
     * );
     * }</pre>
     *
     * @return Number of rows/documents affected.
     */
    int updateWhere(Query<T> query, Map.Entry<String, Object>... fields);

    /** Deletes the entity with the given ID. Returns {@code true} if deleted. */
    boolean deleteById(ID id);

    /** Deletes the given entity (uses its ID). Returns {@code true} if deleted. */
    boolean delete(T entity);

    /** Deletes all entities matching the query. Returns count of deleted records. */
    int deleteWhere(Query<T> query);

    /** Deletes all rows/documents from the table/collection. */
    void clear();
}
