package net.veloxia.universaldb;

import net.veloxia.universaldb.config.DatabaseConfig;
import net.veloxia.universaldb.config.DatabaseConfig.*;
import net.veloxia.universaldb.drivers.MongoDriver;
import net.veloxia.universaldb.drivers.MySQLDriver;
import net.veloxia.universaldb.drivers.SQLiteDriver;

/**
 * Head start of UniversalDB.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class UniversalDB implements AutoCloseable {

    private final Object driver;
    private final DbType type;

    private enum DbType { SQLITE, MYSQL, MONGO }

    private UniversalDB(Object driver, DbType type) {
        this.driver = driver;
        this.type   = type;
    }

    /**
     * Connect to a database using the given configuration.
     */
    public static UniversalDB connect(DatabaseConfig config) {
        return switch (config) {
            case SQLiteConfig  c -> new UniversalDB(new SQLiteDriver(c),  DbType.SQLITE);
            case MySQLConfig   c -> new UniversalDB(new MySQLDriver(c),   DbType.MYSQL);
            case MongoDBConfig c -> new UniversalDB(new MongoDriver(c),   DbType.MONGO);
            default -> throw new IllegalArgumentException("Unknown config type: " + config.getClass());
        };
    }

    /**
     * Returns a {@link Repository} for the given entity class.
     *
     * <ul>
     *   <li>SQLite / MySQL → ID type is {@code Long}</li>
     *   <li>MongoDB → ID type is {@code String}</li>
     * </ul>
     *
     * <pre>{@code
     * Repository<User, Long>   users    = db.repository(User.class);    // SQL
     * Repository<User, String> users    = db.repository(User.class);    // Mongo
     * }</pre>
     */
    public <T> Repository<T, ?> repository(Class<T> clazz) {
        return switch (type) {
            case SQLITE -> ((SQLiteDriver) driver).repository(clazz);
            case MYSQL  -> ((MySQLDriver)  driver).repository(clazz);
            case MONGO  -> ((MongoDriver)  driver).repository(clazz);
        };
    }

    @Override
    public void close() {
        try {
            switch (type) {
                case SQLITE -> ((SQLiteDriver) driver).close();
                case MYSQL  -> ((MySQLDriver)  driver).close();
                case MONGO  -> ((MongoDriver)  driver).close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error closing database", e);
        }
    }
}
