package com.evidenceharbor.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;

public class DatabaseManager {

    private static final String DB_DIR = System.getProperty("user.home") + "/EvidenceHarbor";
    private static final String CONFIG_FILE = DB_DIR + "/db.properties";
    private static final String SEED_VERSION = "3";
    private static DatabaseManager instance;

    private final Connection connection;
    private final Properties config;

    private DatabaseManager() throws Exception {
        this(loadSavedConfigSnapshot(), true);
    }

    private DatabaseManager(Properties config, boolean createDatabase) throws Exception {
        Files.createDirectories(Paths.get(DB_DIR));
        this.config = config;
        writeConfig(config);
        this.connection = openMariaDbConnection(config, createDatabase);
        runScript("/sql/schema-mariadb.sql");
        runMigrations();
        ensureMetaTable();
        if (!isSeedApplied(SEED_VERSION)) {
            runScript("/sql/seed-mariadb.sql");
            markSeedApplied(SEED_VERSION);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            try {
                instance = new DatabaseManager();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    public static synchronized void configureAndConnect(String host, String port, String database,
                                                        String user, String password, boolean createDatabase) throws Exception {
        Properties props = defaultConfig();
        props.setProperty("mariadb.host", host == null ? "127.0.0.1" : host.trim());
        props.setProperty("mariadb.port", port == null ? "3306" : port.trim());
        props.setProperty("mariadb.database", database == null ? "evidence_harbor" : database.trim());
        props.setProperty("mariadb.user", user == null ? "root" : user.trim());
        props.setProperty("mariadb.password", password == null ? "" : password);

        if (instance != null) {
            instance.close();
            instance = null;
        }
        instance = new DatabaseManager(props, createDatabase);
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    public static Properties loadSavedConfigSnapshot() throws Exception {
        Files.createDirectories(Paths.get(DB_DIR));
        Properties props = defaultConfig();
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            }
        }
        props.setProperty("db.type", "mariadb");
        props.remove("sqlite.path");
        return props;
    }

    private static Properties defaultConfig() {
        Properties props = new Properties();
        props.setProperty("db.type", "mariadb");
        props.setProperty("mariadb.host", "127.0.0.1");
        props.setProperty("mariadb.port", "3306");
        props.setProperty("mariadb.database", "evidence_harbor");
        props.setProperty("mariadb.user", "root");
        props.setProperty("mariadb.password", "");
        return props;
    }

    private static void writeConfig(Properties props) throws Exception {
        Path path = Paths.get(CONFIG_FILE);
        try (OutputStream out = Files.newOutputStream(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Evidence Harbor database configuration");
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                // Reconnect by reinitialising from the saved config
                synchronized (DatabaseManager.class) {
                    instance = new DatabaseManager(config, false);
                    return instance.connection;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Database connection lost and could not reconnect: " + e.getMessage(), e);
        }
        return connection;
    }

    public static boolean isConnected() {
        try {
            if (instance == null) return false;
            Connection c = instance.connection;
            return c != null && !c.isClosed() && c.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private Connection openMariaDbConnection(Properties props, boolean createDatabase) throws SQLException {
        String host = props.getProperty("mariadb.host", "127.0.0.1");
        String port = props.getProperty("mariadb.port", "3306");
        String database = props.getProperty("mariadb.database", "evidence_harbor");
        String user = props.getProperty("mariadb.user", "root");
        String password = props.getProperty("mariadb.password", "");

        String serverUrl = "jdbc:mariadb://" + host + ":" + port + "/?useUnicode=true&characterEncoding=utf8";
        if (createDatabase) {
            try (Connection bootstrap = DriverManager.getConnection(serverUrl, user, password);
                 Statement stmt = bootstrap.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database.replace("`", "") + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        }

        String dbUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8"
                + "&autoReconnect=true&connectTimeout=10000&socketTimeout=60000";
        return DriverManager.getConnection(dbUrl, user, password);
    }

    private void runScript(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return;
            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            for (String stmt : sql.split(";")) {
                String trimmed = stmt.lines()
                        .filter(l -> !l.trim().startsWith("--"))
                        .collect(Collectors.joining("\n"))
                        .trim();
                if (!trimmed.isEmpty()) {
                    try (Statement s = connection.createStatement()) {
                        s.execute(trimmed);
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }

    private void runMigrations() {
        String[] alters = {
            "ALTER TABLE evidence ADD COLUMN elec_device_username TEXT",
            "ALTER TABLE evidence ADD COLUMN elec_device_password TEXT",
            "ALTER TABLE officers ADD COLUMN username VARCHAR(100)",
            "ALTER TABLE officers ADD COLUMN password_hash TEXT",
            "ALTER TABLE officers ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'officer'",
            "ALTER TABLE officers ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'Active'",
            "ALTER TABLE officers ADD COLUMN is_external INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE officers ADD COLUMN permissions TEXT",
            "ALTER TABLE qm_vehicle_impound ADD COLUMN case_id INTEGER"
        };
        for (String alter : alters) {
            try (Statement s = connection.createStatement()) {
                s.execute(alter);
            } catch (SQLException ignored) {
            }
        }

        try (Statement s = connection.createStatement()) {
            s.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_officers_username_unique ON officers(username)");
        } catch (SQLException ignored) {
        }
    }

    private void ensureMetaTable() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS app_meta (k VARCHAR(100) PRIMARY KEY, v TEXT NOT NULL)");
        }
    }

    private boolean isSeedApplied(String version) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT v FROM app_meta WHERE k='seed_version'")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && version.equals(rs.getString("v"));
            }
        }
    }

    private void markSeedApplied(String version) throws SQLException {
        int updated;
        try (PreparedStatement ps = connection.prepareStatement("UPDATE app_meta SET v=? WHERE k='seed_version'")) {
            ps.setString(1, version);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO app_meta (k, v) VALUES ('seed_version', ?)")) {
                ps.setString(1, version);
                ps.executeUpdate();
            }
        }
    }

    /** Returns true if a saved db.properties file already exists (i.e. not first-time launch). */
    /** Returns the config of the currently active connection, or the saved file if not connected. */
    public static synchronized Properties getLiveConfig() throws Exception {
        if (instance != null) {
            Properties copy = new Properties();
            copy.putAll(instance.config);
            return copy;
        }
        return loadSavedConfigSnapshot();
    }

    public static boolean hasConfigFile() {
        return Files.exists(Paths.get(CONFIG_FILE));
    }

    /** Persist a single key/value into the saved db.properties config file. */
    public static synchronized void setProperty(String key, String value) throws Exception {
        Files.createDirectories(Paths.get(DB_DIR));
        Path path = Paths.get(CONFIG_FILE);
        Properties props = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            }
        }
        if (value == null || value.isBlank()) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
        try (OutputStream out = Files.newOutputStream(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Evidence Harbor database configuration");
        }
    }

    /** Return a single property from the saved config, or defaultValue if missing. */
    public static String getSavedProperty(String key, String defaultValue) {
        try {
            Properties props = loadSavedConfigSnapshot();
            return props.getProperty(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
