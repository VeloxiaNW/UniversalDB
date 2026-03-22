package net.veloxia.universaldb.config;

/**
 * Base class for all database configurations.
 * Use one of the concrete subclasses: {@link SQLiteConfig}, {@link MySQLConfig}, {@link MongoDBConfig}.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public abstract class DatabaseConfig {
    private DatabaseConfig() {}

    /**
     * SQLite configuration.
     * <pre>{@code
     * DatabaseConfig cfg = new SQLiteConfig("myapp.db");
     * DatabaseConfig mem = new SQLiteConfig(":memory:");
     * }</pre>
     */
    public static final class SQLiteConfig extends DatabaseConfig {
        private final String path;

        public SQLiteConfig(String path) {
            this.path = path;
        }

        public SQLiteConfig() {
            this("database.db");
        }

        public String getPath() { return path; }

        @Override public String toString() { return "SQLite(" + path + ")"; }
    }

    /**
     * MySQL configuration.
     * <pre>{@code
     * DatabaseConfig cfg = new MySQLConfig.Builder("myapp", "root", "secret").build();
     * }</pre>
     */
    public static final class MySQLConfig extends DatabaseConfig {
        private final String host;
        private final int port;
        private final String database;
        private final String username;
        private final String password;
        private final boolean useSSL;
        private final boolean allowPublicKeyRetrieval;
        private final int connectionTimeout;

        private MySQLConfig(Builder b) {
            this.host = b.host;
            this.port = b.port;
            this.database = b.database;
            this.username = b.username;
            this.password = b.password;
            this.useSSL = b.useSSL;
            this.allowPublicKeyRetrieval = b.allowPublicKeyRetrieval;
            this.connectionTimeout = b.connectionTimeout;
        }

        public String getHost()                  { return host; }
        public int    getPort()                  { return port; }
        public String getDatabase()              { return database; }
        public String getUsername()              { return username; }
        public String getPassword()              { return password; }
        public boolean isUseSSL()                { return useSSL; }
        public boolean isAllowPublicKeyRetrieval(){ return allowPublicKeyRetrieval; }
        public int    getConnectionTimeout()     { return connectionTimeout; }

        @Override public String toString() {
            return "MySQL(" + host + ":" + port + "/" + database + ")";
        }

        public static class Builder {
            private String host = "localhost";
            private int port = 3306;
            private final String database;
            private final String username;
            private final String password;
            private boolean useSSL = false;
            private boolean allowPublicKeyRetrieval = true;
            private int connectionTimeout = 30_000;

            public Builder(String database, String username, String password) {
                this.database = database;
                this.username = username;
                this.password = password;
            }

            public Builder host(String host)               { this.host = host; return this; }
            public Builder port(int port)                  { this.port = port; return this; }
            public Builder useSSL(boolean v)               { this.useSSL = v; return this; }
            public Builder allowPublicKeyRetrieval(boolean v){ this.allowPublicKeyRetrieval = v; return this; }
            public Builder connectionTimeout(int ms)       { this.connectionTimeout = ms; return this; }

            public MySQLConfig build() { return new MySQLConfig(this); }
        }
    }

    /**
     * MongoDB configuration.
     * <pre>{@code
     * DatabaseConfig cfg = new MongoDBConfig("myapp");
     * DatabaseConfig cfg = new MongoDBConfig("mongodb://user:pass@host:27017", "myapp");
     * }</pre>
     */
    public static final class MongoDBConfig extends DatabaseConfig {
        private final String uri;
        private final String database;
        private final int connectTimeoutMs;
        private final int socketTimeoutMs;

        public MongoDBConfig(String database) {
            this("mongodb://localhost:27017", database, 10_000, 30_000);
        }

        public MongoDBConfig(String uri, String database) {
            this(uri, database, 10_000, 30_000);
        }

        public MongoDBConfig(String uri, String database, int connectTimeoutMs, int socketTimeoutMs) {
            this.uri = uri;
            this.database = database;
            this.connectTimeoutMs = connectTimeoutMs;
            this.socketTimeoutMs = socketTimeoutMs;
        }

        public String getUri()            { return uri; }
        public String getDatabase()       { return database; }
        public int    getConnectTimeout() { return connectTimeoutMs; }
        public int    getSocketTimeout()  { return socketTimeoutMs; }

        @Override public String toString() { return "MongoDB(" + uri + "/" + database + ")"; }
    }
}
