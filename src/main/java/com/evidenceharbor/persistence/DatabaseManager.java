package com.evidenceharbor.persistence;

import java.io.BufferedReader;
import java.io.IOException;
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

    private static final Path LEGACY_CONFIG_DIR = Paths.get(System.getProperty("user.home"), "EvidenceHarbor");
    private static final Path LEGACY_CONFIG_FILE = LEGACY_CONFIG_DIR.resolve("db.properties");
    private static final Path INSTALLED_CONFIG_FILE = Paths.get(System.getProperty("user.dir"), "db.properties");
    private static final String SEED_VERSION = "4";
    private static DatabaseManager instance;

    private final Connection connection;
    private final Properties config;

    private DatabaseManager() throws Exception {
        this(loadSavedConfigSnapshot(), true);
    }

    private DatabaseManager(Properties config, boolean createDatabase) throws Exception {
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
        Properties props = defaultConfig();

        // Prefer config saved in the installed app location.
        // If absent, fall back to legacy user-profile location.
        Path path = resolveReadConfigFile();
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
        
        // Try loading bundled db.properties from resources (lower priority than user's home config)
        try {
            InputStream bundledConfig = DatabaseManager.class.getResourceAsStream("/db.properties");
            if (bundledConfig != null) {
                Properties bundled = new Properties();
                bundled.load(bundledConfig);
                // Merge bundled config, but don't override properties already set
                if (bundled.getProperty("mariadb.host") != null && !bundled.getProperty("mariadb.host").isEmpty()) {
                    props.setProperty("mariadb.host", bundled.getProperty("mariadb.host"));
                }
                if (bundled.getProperty("mariadb.port") != null && !bundled.getProperty("mariadb.port").isEmpty()) {
                    props.setProperty("mariadb.port", bundled.getProperty("mariadb.port"));
                }
                if (bundled.getProperty("mariadb.database") != null && !bundled.getProperty("mariadb.database").isEmpty()) {
                    props.setProperty("mariadb.database", bundled.getProperty("mariadb.database"));
                }
                if (bundled.getProperty("mariadb.user") != null && !bundled.getProperty("mariadb.user").isEmpty()) {
                    props.setProperty("mariadb.user", bundled.getProperty("mariadb.user"));
                }
                if (bundled.getProperty("mariadb.password") != null) {
                    props.setProperty("mariadb.password", bundled.getProperty("mariadb.password"));
                }
                bundledConfig.close();
            }
        } catch (Exception ignored) {}
        
        return props;
    }

    private static void writeConfig(Properties props) throws Exception {
        Path path = resolveWriteConfigFile();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
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
            "ALTER TABLE evidence ADD COLUMN vehicle_body_type VARCHAR(100)",
            "CREATE TABLE IF NOT EXISTS vehicle_types (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL UNIQUE)",
            "CREATE TABLE IF NOT EXISTS impound_locations (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL UNIQUE)",
            "ALTER TABLE officers ADD COLUMN username VARCHAR(100)",
            "ALTER TABLE officers ADD COLUMN password_hash TEXT",
            "ALTER TABLE officers ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'officer'",
            "ALTER TABLE officers ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'Active'",
            "ALTER TABLE officers ADD COLUMN is_external INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE officers ADD COLUMN permissions TEXT",
            "ALTER TABLE bank_account_transactions ADD COLUMN voided TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE bank_account_transactions ADD COLUMN voided_reason TEXT",
            "ALTER TABLE bank_account_transactions ADD COLUMN voided_by VARCHAR(255)",
            "ALTER TABLE bank_account_transactions ADD COLUMN voided_at DATETIME",
            "ALTER TABLE bank_account_transactions ADD COLUMN source_ref VARCHAR(255)",
            "ALTER TABLE persons ADD COLUMN dob VARCHAR(20)",
            "ALTER TABLE persons ADD COLUMN ssn VARCHAR(20)",
            "ALTER TABLE persons ADD COLUMN street VARCHAR(255)",
            "ALTER TABLE persons ADD COLUMN city VARCHAR(100)",
            "ALTER TABLE persons ADD COLUMN state VARCHAR(10)",
            "ALTER TABLE persons ADD COLUMN zip VARCHAR(20)",
            "ALTER TABLE persons ADD COLUMN contact VARCHAR(255)",
            "ALTER TABLE evidence ADD COLUMN bank_account_id INT",
            "UPDATE evidence SET bank_account_id = (SELECT id FROM bank_accounts ORDER BY id LIMIT 1) WHERE status = 'Deposited' AND bank_account_id IS NULL",
            "ALTER TABLE evidence ADD COLUMN scan_code VARCHAR(20)",
            "CREATE UNIQUE INDEX idx_evidence_scan_code_unique ON evidence(scan_code)"
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

        backfillScanCodes();
    }

    /** Assign a short YY-NNNNN scan code to any evidence row that doesn't have one yet. */
    private void backfillScanCodes() {
        // Find the highest existing suffix per year so we continue the sequence
        java.util.Map<String,Integer> nextByYear = new java.util.HashMap<>();
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT scan_code FROM evidence WHERE scan_code IS NOT NULL")) {
            while (rs.next()) {
                String code = rs.getString(1);
                if (code == null) continue;
                String[] parts = code.split("-");
                if (parts.length != 2) continue;
                try {
                    int seq = Integer.parseInt(parts[1]);
                    nextByYear.merge(parts[0], seq, Math::max);
                } catch (NumberFormatException ignored) {}
            }
        } catch (SQLException ignored) { return; }

        try (PreparedStatement sel = connection.prepareStatement(
                     "SELECT id, barcode FROM evidence WHERE scan_code IS NULL ORDER BY id");
             PreparedStatement upd = connection.prepareStatement(
                     "UPDATE evidence SET scan_code=? WHERE id=?");
             ResultSet rs = sel.executeQuery()) {
            int currentYearDefault = java.time.LocalDate.now().getYear() % 100;
            while (rs.next()) {
                int id = rs.getInt("id");
                String barcode = rs.getString("barcode");
                // Pull the year from the existing barcode prefix if it looks like YYYY-...; else use current year
                int yy = currentYearDefault;
                if (barcode != null && barcode.length() >= 4) {
                    try { yy = Integer.parseInt(barcode.substring(0, 4)) % 100; } catch (NumberFormatException ignored) {}
                }
                String yyKey = String.format("%02d", yy);
                int next = nextByYear.getOrDefault(yyKey, 0) + 1;
                nextByYear.put(yyKey, next);
                String scan = yyKey + "-" + String.format("%05d", next);
                upd.setString(1, scan);
                upd.setInt(2, id);
                upd.executeUpdate();
            }
        } catch (SQLException ignored) {}
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
        return Files.exists(INSTALLED_CONFIG_FILE) || Files.exists(LEGACY_CONFIG_FILE);
    }

    /** Persist a single key/value into the saved db.properties config file. */
    public static synchronized void setProperty(String key, String value) throws Exception {
        Path path = resolveWriteConfigFile();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
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

    private static Path resolveReadConfigFile() {
        if (Files.exists(INSTALLED_CONFIG_FILE)) {
            return INSTALLED_CONFIG_FILE;
        }
        if (Files.exists(LEGACY_CONFIG_FILE)) {
            return LEGACY_CONFIG_FILE;
        }
        return INSTALLED_CONFIG_FILE;
    }

    private static Path resolveWriteConfigFile() {
        if (isPathWritable(INSTALLED_CONFIG_FILE)) {
            return INSTALLED_CONFIG_FILE;
        }
        return LEGACY_CONFIG_FILE;
    }

    private static boolean isPathWritable(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(path)) {
                return Files.isWritable(path);
            }
            try (OutputStream out = Files.newOutputStream(path,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                // probe write access
            }
            Files.deleteIfExists(path);
            return true;
        } catch (IOException ignored) {
            return false;
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
