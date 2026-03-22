package net.veloxia.universaldb.drivers;

import net.veloxia.universaldb.config.DatabaseConfig.MySQLConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * MySQL driver - manages the JDBC connection and creates repositories.
 *
 * <pre>{@code
 * MySQLConfig cfg = new MySQLConfig.Builder("myapp", "root", "secret")
 *     .host("localhost").port(3306).build();
 * MySQLDriver driver = new MySQLDriver(cfg);
 * Repository<User, Long> users = driver.repository(User.class);
 * }</pre>
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class MySQLDriver implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MySQLDriver.class);
    private final MySQLConfig config;
    private Connection connection;

    public MySQLDriver(MySQLConfig config) {
        this.config = config;
    }

    private Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + config.getHost() + ":" + config.getPort()
                        + "/" + config.getDatabase()
                        + "?useSSL=" + config.isUseSSL()
                        + "&allowPublicKeyRetrieval=" + config.isAllowPublicKeyRetrieval()
                        + "&connectTimeout=" + config.getConnectionTimeout()
                        + "&serverTimezone=UTC";
                log.info("Connecting to MySQL: {}:{}/{}", config.getHost(), config.getPort(), config.getDatabase());
                Properties props = new Properties();
                props.setProperty("user", config.getUsername());
                props.setProperty("password", config.getPassword());
                connection = DriverManager.getConnection(url, props);
            } catch (Exception e) {
                throw new RuntimeException("MySQL connection failed", e);
            }
        }
        return connection;
    }

    public <T> MySQLRepository<T, Long> repository(Class<T> clazz) {
        MySQLRepository<T, Long> repo = new MySQLRepository<>(clazz, getConnection());
        repo.createTableIfNotExists();
        return repo;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            log.warn("Error closing MySQL connection", e);
        }
    }
}

class MySQLRepository<T, ID> extends SqlRepository<T, ID> {

    MySQLRepository(Class<T> clazz, Connection connection) {
        super(clazz, connection);
    }

    @Override
    protected String idColumnDef() {
        return meta.idColumnName() + " BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY";
    }
}
