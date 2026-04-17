package com.evidenceharbor.tools;

import com.evidenceharbor.persistence.DatabaseManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseDedupeTool {

    public static void main(String[] args) throws Exception {
        String dbFile = System.getProperty("user.home") + "/EvidenceHarbor/evidence_harbor.db";
        backupDatabase(dbFile);

        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            int beforeDuplicateGroups = duplicatePersonGroups(conn);
            int beforeDuplicateRows = duplicatePersonRows(conn);

            runPersonDedupe(conn);

            int afterDuplicateGroups = duplicatePersonGroups(conn);
            int afterDuplicateRows = duplicatePersonRows(conn);
            conn.commit();

            System.out.println("Person duplicate groups: " + beforeDuplicateGroups + " -> " + afterDuplicateGroups);
            System.out.println("Person duplicate rows:   " + beforeDuplicateRows + " -> " + afterDuplicateRows);
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void backupDatabase(String dbFile) throws Exception {
        Path source = Path.of(dbFile);
        if (!Files.exists(source)) {
            return;
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backup = source.resolveSibling("evidence_harbor_backup_" + stamp + ".db");
        Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
        System.out.println("Backup created: " + backup);
    }

    private static int duplicatePersonGroups(Connection conn) throws Exception {
        String sql = "SELECT COUNT(*) FROM (SELECT full_name FROM persons GROUP BY full_name HAVING COUNT(*) > 1) t";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static int duplicatePersonRows(Connection conn) throws Exception {
        String sql = "SELECT COALESCE(SUM(cnt - 1), 0) FROM (SELECT COUNT(*) AS cnt FROM persons GROUP BY full_name HAVING COUNT(*) > 1) t";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void runPersonDedupe(Connection conn) throws Exception {
        try (Statement s = conn.createStatement()) {
            s.execute("DROP TABLE IF EXISTS _person_dedupe");
            s.execute("CREATE TEMP TABLE _person_dedupe AS " +
                    "SELECT full_name, MIN(id) AS keep_id FROM persons GROUP BY full_name HAVING COUNT(*) > 1");

            // Remove case_person rows that would conflict after reassignment.
            s.execute("DELETE FROM case_persons WHERE id IN (" +
                    "SELECT cp.id FROM case_persons cp " +
                    "JOIN persons p ON p.id = cp.person_id " +
                    "JOIN _person_dedupe d ON d.full_name = p.full_name " +
                    "WHERE cp.person_id <> d.keep_id " +
                    "AND EXISTS (" +
                    "  SELECT 1 FROM case_persons cp2 " +
                    "  WHERE cp2.case_id = cp.case_id AND cp2.person_id = d.keep_id" +
                    ")" +
                    ")");

            // Reassign case_persons to canonical person ids.
            s.execute("UPDATE case_persons SET person_id = (" +
                    "SELECT d.keep_id FROM persons p JOIN _person_dedupe d ON d.full_name = p.full_name WHERE p.id = case_persons.person_id" +
                    ") WHERE person_id IN (" +
                    "SELECT p.id FROM persons p JOIN _person_dedupe d ON d.full_name = p.full_name WHERE p.id <> d.keep_id" +
                    ")");

            // Reassign evidence collected_from reference.
            s.execute("UPDATE evidence SET collected_from_person_id = (" +
                    "SELECT d.keep_id FROM persons p JOIN _person_dedupe d ON d.full_name = p.full_name WHERE p.id = evidence.collected_from_person_id" +
                    ") WHERE collected_from_person_id IN (" +
                    "SELECT p.id FROM persons p JOIN _person_dedupe d ON d.full_name = p.full_name WHERE p.id <> d.keep_id" +
                    ")");

            // Delete duplicate person records after references are moved.
            s.execute("DELETE FROM persons WHERE id IN (" +
                    "SELECT p.id FROM persons p JOIN _person_dedupe d ON d.full_name = p.full_name WHERE p.id <> d.keep_id" +
                    ")");

            s.execute("DROP TABLE IF EXISTS _person_dedupe");
        }
    }
}
