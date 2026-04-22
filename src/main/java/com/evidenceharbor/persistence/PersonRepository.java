package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.Person;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PersonRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<Person> findAll() throws SQLException {
        List<Person> list = new ArrayList<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM persons ORDER BY full_name")) {
            while (rs.next()) list.add(mapPerson(rs));
        }
        return list;
    }

    /** Like {@link #findAll()} but sorted by most recently created first (id DESC). */
    public List<Person> findAllRecent() throws SQLException {
        List<Person> list = new ArrayList<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM persons ORDER BY id DESC")) {
            while (rs.next()) list.add(mapPerson(rs));
        }
        return list;
    }

    public Person findById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM persons WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPerson(rs) : null;
            }
        }
    }

    private Person mapPerson(ResultSet rs) throws SQLException {
        Person p = new Person(rs.getInt("id"), rs.getString("full_name"));
        // Optional columns — older DBs may not have them yet.
        try { p.setDob(rs.getString("dob")); } catch (SQLException ignore) {}
        try { p.setSsn(rs.getString("ssn")); } catch (SQLException ignore) {}
        try { p.setStreet(rs.getString("street")); } catch (SQLException ignore) {}
        try { p.setCity(rs.getString("city")); } catch (SQLException ignore) {}
        try { p.setState(rs.getString("state")); } catch (SQLException ignore) {}
        try { p.setZip(rs.getString("zip")); } catch (SQLException ignore) {}
        try { p.setContact(rs.getString("contact")); } catch (SQLException ignore) {}
        return p;
    }

    public Person save(Person p) throws SQLException {
        // Enforce uniqueness on SSN (when provided). Blanks are allowed to repeat.
        String ssn = emptyToNull(p.getSsn());
        if (ssn != null) {
            Person existing = findBySsn(ssn);
            if (existing != null && existing.getId() != p.getId()) {
                throw new SQLException(
                        "A person with SSN " + ssn + " already exists: \""
                                + (existing.getFullName() == null ? "(no name)" : existing.getFullName())
                                + "\" (id #" + existing.getId() + ").");
            }
        }
        if (p.getId() == 0) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO persons (full_name, dob, ssn, street, city, state, zip, contact) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                bindDetails(ps, p, 1);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) p.setId(keys.getInt(1)); }
            }
        } else {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE persons SET full_name=?, dob=?, ssn=?, street=?, city=?, state=?, zip=?, contact=? " +
                    "WHERE id=?")) {
                bindDetails(ps, p, 1);
                ps.setInt(9, p.getId());
                ps.executeUpdate();
            }
        }
        return p;
    }

    private void bindDetails(PreparedStatement ps, Person p, int startIdx) throws SQLException {
        ps.setString(startIdx,     p.getFullName());
        ps.setString(startIdx + 1, emptyToNull(p.getDob()));
        ps.setString(startIdx + 2, emptyToNull(p.getSsn()));
        ps.setString(startIdx + 3, emptyToNull(p.getStreet()));
        ps.setString(startIdx + 4, emptyToNull(p.getCity()));
        ps.setString(startIdx + 5, emptyToNull(p.getState()));
        ps.setString(startIdx + 6, emptyToNull(p.getZip()));
        ps.setString(startIdx + 7, emptyToNull(p.getContact()));
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Find a person by exact SSN match (trimmed). Returns null if none exists. */
    public Person findBySsn(String ssn) throws SQLException {
        String cleaned = emptyToNull(ssn);
        if (cleaned == null) return null;
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM persons WHERE ssn = ? LIMIT 1")) {
            ps.setString(1, cleaned);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPerson(rs) : null;
            }
        }
    }

    /** Returns a map of person_id -> number of distinct cases they are associated with. */
    public Map<Integer, Integer> caseCountByPerson() throws SQLException {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery(
                "SELECT person_id, COUNT(DISTINCT case_id) AS n FROM case_persons GROUP BY person_id")) {
            while (rs.next()) counts.put(rs.getInt("person_id"), rs.getInt("n"));
        }
        return counts;
    }

    /** Summary row describing a case that a person is associated with. */
    public static class PersonCaseRow {
        public final int caseId;
        public final String caseNumber;
        public final String incidentDate;
        public final String role;
        public final String officerName;
        public PersonCaseRow(int caseId, String caseNumber, String incidentDate, String role, String officerName) {
            this.caseId = caseId;
            this.caseNumber = caseNumber;
            this.incidentDate = incidentDate;
            this.role = role;
            this.officerName = officerName;
        }
    }

    public List<PersonCaseRow> findCasesForPerson(int personId) throws SQLException {
        List<PersonCaseRow> list = new ArrayList<>();
        String sql = "SELECT c.id, c.case_number, c.incident_date, cp.role, o.name AS officer_name " +
                     "FROM case_persons cp " +
                     "JOIN cases c ON c.id = cp.case_id " +
                     "JOIN officers o ON o.id = c.officer_id " +
                     "WHERE cp.person_id = ? " +
                     "ORDER BY c.incident_date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PersonCaseRow(
                            rs.getInt("id"),
                            rs.getString("case_number"),
                            rs.getString("incident_date"),
                            rs.getString("role"),
                            rs.getString("officer_name")));
                }
            }
        }
        return list;
    }

    /**
     * Merges {@code removeId} into {@code keepId}. All {@code case_persons} rows for the
     * removed person are re-pointed to the kept person; duplicates (same case_id/role)
     * are dropped, then the removed person row is deleted.
     */
    public void merge(int keepId, int removeId) throws SQLException {
        if (keepId == removeId) return;
        Connection c = conn();
        boolean prevAuto = c.getAutoCommit();
        try {
            c.setAutoCommit(false);
            // Re-point rows that don't collide with the unique key; IGNORE skips duplicates.
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE IGNORE case_persons SET person_id=? WHERE person_id=?")) {
                ps.setInt(1, keepId);
                ps.setInt(2, removeId);
                ps.executeUpdate();
            }
            // Clean up any leftover duplicate rows that UPDATE IGNORE skipped.
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM case_persons WHERE person_id=?")) {
                ps.setInt(1, removeId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM persons WHERE id=?")) {
                ps.setInt(1, removeId);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            try { c.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            try { c.setAutoCommit(prevAuto); } catch (SQLException ignore) {}
        }
    }
}
