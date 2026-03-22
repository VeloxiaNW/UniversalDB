package net.veloxia.universaldb.drivers;

import net.veloxia.universaldb.config.DatabaseConfig.SQLiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite driver - manages the JDBC connection and creates repositories.
 *
 * <pre>{@code
 * SQLiteDriver driver = new SQLiteDriver(new SQLiteConfig("myapp.db"));
 * Repository<User, Long> users = driver.repository(User.class);
 * }</pre>
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class SQLiteDriver implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SQLiteDriver.class);
    private final SQLiteConfig config;
    private Connection connection;

    public SQLiteDriver(SQLiteConfig config) {
        this.config = config;
    }

    private Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:" + config.getPath();
                log.info("Connecting to SQLite: {}", url);
                connection = DriverManager.getConnection(url);
                connection.createStatement().execute("PRAGMA journal_mode=WAL");
                connection.createStatement().execute("PRAGMA foreign_keys=ON");
            } catch (Exception e) {
                throw new RuntimeException("SQLite connection failed", e);
            }
        }
        return connection;
    }

    public <T> SQLiteRepository<T, Long> repository(Class<T> clazz) {
        SQLiteRepository<T, Long> repo = new SQLiteRepository<>(clazz, getConnection());
        repo.createTableIfNotExists();
        return repo;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            log.warn("Error closing SQLite connection", e);
        }
    }
}

class SQLiteRepository<T, ID> extends SqlRepository<T, ID> {

    SQLiteRepository(Class<T> clazz, Connection connection) {
        super(clazz, connection);
    }

    @Override
    protected String idColumnDef() {
        return meta.idColumnName() + " INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    protected Object coerceId(Object raw) {
        return raw instanceof Number n ? n.longValue() : raw;
    }
}
