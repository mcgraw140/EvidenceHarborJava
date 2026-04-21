package com.evidenceharbor.persistence;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    /** Evidence count grouped by status, descending. Returns rows: [status, count] */
    public List<String[]> evidenceByStatus() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT COALESCE(status, 'Unknown') AS status, COUNT(*) AS cnt " +
                     "FROM evidence GROUP BY status ORDER BY cnt DESC";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) rows.add(new String[]{rs.getString("status"), String.valueOf(rs.getInt("cnt"))});
        }
        return rows;
    }

    /** Evidence count grouped by type, descending. Returns rows: [evidence_type, count] */
    public List<String[]> evidenceByType() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT COALESCE(evidence_type, 'Unknown') AS evidence_type, COUNT(*) AS cnt " +
                     "FROM evidence GROUP BY evidence_type ORDER BY cnt DESC";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) rows.add(new String[]{rs.getString("evidence_type"), String.valueOf(rs.getInt("cnt"))});
        }
        return rows;
    }

    /** Evidence count per officer (all officers, including those with zero). Returns rows: [name, badge, count] */
    public List<String[]> evidenceByOfficer() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT o.name, COALESCE(o.badge, '') AS badge, COUNT(e.id) AS cnt " +
                     "FROM officers o LEFT JOIN evidence e ON e.collected_by_officer_id = o.id " +
                     "GROUP BY o.id, o.name, o.badge ORDER BY cnt DESC";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) rows.add(new String[]{
                rs.getString("name"), rs.getString("badge"), String.valueOf(rs.getInt("cnt"))
            });
        }
        return rows;
    }

    /** Evidence count grouped by storage location, descending. Returns rows: [location, count] */
    public List<String[]> evidenceByLocation() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT COALESCE(storage_location, 'Unassigned') AS location, COUNT(*) AS cnt " +
                     "FROM evidence GROUP BY storage_location ORDER BY cnt DESC";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) rows.add(new String[]{rs.getString("location"), String.valueOf(rs.getInt("cnt"))});
        }
        return rows;
    }

    /**
     * Cases with evidence and person counts within a date range.
     * Returns rows: [case_number, incident_date, officer_name, badge, evidence_count, persons_count]
     */
    public List<String[]> casesSummary(LocalDate from, LocalDate to) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT c.case_number, c.incident_date, o.name, COALESCE(o.badge, '') AS badge, " +
                     "COUNT(DISTINCT e.id) AS evidence_count, COUNT(DISTINCT cp.id) AS persons_count " +
                     "FROM cases c JOIN officers o ON o.id = c.officer_id " +
                     "LEFT JOIN evidence e ON e.case_id = c.id " +
                     "LEFT JOIN case_persons cp ON cp.case_id = c.id " +
                     "WHERE c.incident_date BETWEEN ? AND ? " +
                     "GROUP BY c.id, c.case_number, c.incident_date, o.name, o.badge " +
                     "ORDER BY c.incident_date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new String[]{
                    rs.getString("case_number"),
                    rs.getString("incident_date"),
                    rs.getString("name"),
                    rs.getString("badge"),
                    String.valueOf(rs.getInt("evidence_count")),
                    String.valueOf(rs.getInt("persons_count"))
                });
            }
        }
        return rows;
    }

    // ── Detail drill-down queries ──────────────────────────────────────────────

    private static final String DETAIL_SELECT =
        "SELECT e.barcode, COALESCE(c.case_number,'—') AS case_number, " +
        "COALESCE(e.evidence_type,'Unknown') AS evidence_type, " +
        "COALESCE(e.status,'Unknown') AS status, " +
        "COALESCE(e.storage_location,'Unassigned') AS storage_location, " +
        "COALESCE(o.name,'—') AS officer_name, " +
        "COALESCE(e.collection_date,'') AS collection_date, " +
        "COALESCE(e.description,'') AS description " +
        "FROM evidence e " +
        "LEFT JOIN cases c ON c.id = e.case_id " +
        "LEFT JOIN officers o ON o.id = e.collected_by_officer_id ";

    public static final String[] DETAIL_HEADERS =
        {"Barcode", "Case #", "Type", "Status", "Location", "Officer", "Date", "Description"};

    private List<String[]> mapDetailRows(ResultSet rs) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            rows.add(new String[]{
                rs.getString("barcode"),
                rs.getString("case_number"),
                rs.getString("evidence_type"),
                rs.getString("status"),
                rs.getString("storage_location"),
                rs.getString("officer_name"),
                rs.getString("collection_date"),
                rs.getString("description")
            });
        }
        return rows;
    }

    /** All evidence items with a given status. */
    public List<String[]> evidenceDetailByStatus(String status) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE e.status = ? ORDER BY e.id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { return mapDetailRows(rs); }
        }
    }

    /** All evidence items belonging to a given type. */
    public List<String[]> evidenceDetailByType(String type) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE COALESCE(e.evidence_type,'Unknown') = ? ORDER BY e.id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) { return mapDetailRows(rs); }
        }
    }

    /** All evidence items collected by a given officer (matched by name). */
    public List<String[]> evidenceDetailByOfficer(String officerName) throws SQLException {
        String sql = DETAIL_SELECT + "WHERE o.name = ? ORDER BY e.id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, officerName);
            try (ResultSet rs = ps.executeQuery()) { return mapDetailRows(rs); }
        }
    }

    /** All evidence items in a given storage location. */
    public List<String[]> evidenceDetailByLocation(String location) throws SQLException {
        String sql = DETAIL_SELECT +
            "WHERE COALESCE(e.storage_location,'Unassigned') = ? ORDER BY e.id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, location);
            try (ResultSet rs = ps.executeQuery()) { return mapDetailRows(rs); }
        }
    }

    /** All evidence items matching any of the given statuses (for stat-card drill-down). */
    public List<String[]> evidenceDetailByStatuses(List<String> statuses) throws SQLException {
        if (statuses == null || statuses.isEmpty()) return new ArrayList<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(statuses.size(), "?"));
        String sql = DETAIL_SELECT + "WHERE e.status IN (" + placeholders + ") ORDER BY e.id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            for (int i = 0; i < statuses.size(); i++) ps.setString(i + 1, statuses.get(i));
            try (ResultSet rs = ps.executeQuery()) { return mapDetailRows(rs); }
        }
    }

    /** All evidence items regardless of status. */
    public List<String[]> evidenceDetailAll() throws SQLException {
        String sql = DETAIL_SELECT + "ORDER BY e.id DESC";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return mapDetailRows(rs);
        }
    }

    /**
     * Returns [totalEvidence, activeEvidence, totalCases, disposedEvidence]
     */
    public int[] getSummaryStats() throws SQLException {
        int total = 0, active = 0, totalCases = 0, disposed = 0;
        try (Statement s = conn().createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM evidence")) {
                if (rs.next()) total = rs.getInt(1);
            }
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM evidence WHERE status IN ('In Custody','In Storage','Checked In','In Dropbox','Pending')")) {
                if (rs.next()) active = rs.getInt(1);
            }
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM cases")) {
                if (rs.next()) totalCases = rs.getInt(1);
            }
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM evidence WHERE status IN ('Destroyed','Disbursed','Returned to Owner')")) {
                if (rs.next()) disposed = rs.getInt(1);
            }
        }
        return new int[]{total, active, totalCases, disposed};
    }
}
