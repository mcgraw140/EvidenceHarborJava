package com.evidenceharbor.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseManager {

    private static final String DB_DIR  = System.getProperty("user.home") + "/EvidenceHarbor";
    private static final String DB_FILE = DB_DIR + "/evidence_harbor.db";
    private static final String SEED_VERSION = "2";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws Exception {
        Files.createDirectories(Paths.get(DB_DIR));
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
        }
        runScript("/sql/schema.sql");
        runMigrations();
        ensureMetaTable();
        if (!isSeedApplied(SEED_VERSION)) {
            runScript("/sql/seed.sql");
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

    public Connection getConnection() { return connection; }

    private void runScript(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return;
            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            for (String stmt : sql.split(";")) {
                // Strip comment lines then check if anything executable remains
                String trimmed = stmt.lines()
                        .filter(l -> !l.trim().startsWith("--"))
                        .collect(java.util.stream.Collectors.joining("\n"))
                        .trim();
                if (!trimmed.isEmpty()) {
                    try (Statement s = connection.createStatement()) {
                        s.execute(trimmed);
                    } catch (SQLException ignored) {}
                }
            }
        }
    }

    private void runMigrations() {
        String[] alters = {
            "ALTER TABLE evidence ADD COLUMN elec_device_username TEXT",
            "ALTER TABLE evidence ADD COLUMN elec_device_password TEXT",
            "ALTER TABLE officers ADD COLUMN username TEXT UNIQUE",
            "ALTER TABLE officers ADD COLUMN password_hash TEXT",
            "ALTER TABLE officers ADD COLUMN role TEXT NOT NULL DEFAULT 'officer'",
            "ALTER TABLE officers ADD COLUMN status TEXT NOT NULL DEFAULT 'Active'",
            "ALTER TABLE officers ADD COLUMN is_external INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE officers ADD COLUMN permissions TEXT",
            "ALTER TABLE qm_vehicle_impound ADD COLUMN case_id INTEGER"
        };
        for (String alter : alters) {
            try (Statement s = connection.createStatement()) { s.execute(alter); }
            catch (SQLException ignored) {}
        }
    }

    private void ensureMetaTable() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS app_meta (k TEXT PRIMARY KEY, v TEXT NOT NULL)");
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
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO app_meta (k, v) VALUES ('seed_version', ?) " +
                "ON CONFLICT(k) DO UPDATE SET v=excluded.v")) {
            ps.setString(1, version);
            ps.executeUpdate();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
