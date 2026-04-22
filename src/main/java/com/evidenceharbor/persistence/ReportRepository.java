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

    // ── Drill-down detail queries ─────────────────────────────────────────
    /** Column headers for all evidence detail drill-down result sets. */
    public static final String[] EVIDENCE_DETAIL_HEADERS =
        {"Barcode", "Case #", "Type", "Status", "Location", "Officer", "Collected"};

    private static final String EVIDENCE_DETAIL_SELECT =
        "SELECT e.barcode, c.case_number, e.evidence_type, e.status, " +
        "COALESCE(e.storage_location,'') AS storage_location, " +
        "COALESCE(o.name,'') AS officer_name, COALESCE(e.collection_date,'') AS collection_date " +
        "FROM evidence e " +
        "JOIN cases c ON c.id = e.case_id " +
        "LEFT JOIN officers o ON o.id = e.collected_by_officer_id ";

    private List<String[]> evidenceDetail(String whereClause, java.util.function.Consumer<PreparedStatement> binder) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = EVIDENCE_DETAIL_SELECT + whereClause + " ORDER BY e.collection_date DESC, e.barcode";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (binder != null) binder.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7)
                });
            }
        }
        return rows;
    }

    /** All evidence rows. */
    public List<String[]> evidenceDetailsAll() throws SQLException {
        return evidenceDetail("", null);
    }

    /** Evidence with the given status (or 'Unknown' for null). */
    public List<String[]> evidenceDetailsByStatus(String status) throws SQLException {
        if ("Unknown".equalsIgnoreCase(status)) return evidenceDetail("WHERE e.status IS NULL ", null);
        return evidenceDetail("WHERE e.status = ? ", ps -> setString(ps, 1, status));
    }

    /** Evidence with a status in the "active / in custody" group. */
    public List<String[]> evidenceDetailsActive() throws SQLException {
        return evidenceDetail(
            "WHERE e.status IN ('In Custody','In Storage','Checked In','In Dropbox','Pending') ", null);
    }

    /** Evidence with a status in the "disposed" group. */
    public List<String[]> evidenceDetailsDisposed() throws SQLException {
        return evidenceDetail(
            "WHERE e.status IN ('Destroyed','Disbursed','Returned to Owner') ", null);
    }

    /** Evidence of the given type. */
    public List<String[]> evidenceDetailsByType(String type) throws SQLException {
        if ("Unknown".equalsIgnoreCase(type)) return evidenceDetail("WHERE e.evidence_type IS NULL ", null);
        return evidenceDetail("WHERE e.evidence_type = ? ", ps -> setString(ps, 1, type));
    }

    /** Evidence at the given storage location (pass 'Unassigned' for null/empty). */
    public List<String[]> evidenceDetailsByLocation(String location) throws SQLException {
        if ("Unassigned".equalsIgnoreCase(location)) {
            return evidenceDetail("WHERE e.storage_location IS NULL OR e.storage_location = '' ", null);
        }
        return evidenceDetail("WHERE e.storage_location = ? ", ps -> setString(ps, 1, location));
    }

    /** Evidence collected by the given officer (matched by name + badge). */
    public List<String[]> evidenceDetailsByOfficer(String officerName, String badge) throws SQLException {
        return evidenceDetail(
            "WHERE o.name = ? AND COALESCE(o.badge,'') = ? ",
            ps -> { setString(ps, 1, officerName); setString(ps, 2, badge == null ? "" : badge); });
    }

    /** Evidence belonging to the given case number. */
    public List<String[]> evidenceDetailsForCase(String caseNumber) throws SQLException {
        return evidenceDetail("WHERE c.case_number = ? ", ps -> setString(ps, 1, caseNumber));
    }

    /** Column headers for the cases drill-down result set. */
    public static final String[] CASE_DETAIL_HEADERS =
        {"Case #", "Incident Date", "Officer", "Badge", "Evidence Count"};

    /** Every case with evidence count. */
    public List<String[]> caseDetailsAll() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT c.case_number, c.incident_date, o.name, COALESCE(o.badge,'') AS badge, " +
                     "COUNT(e.id) AS cnt " +
                     "FROM cases c JOIN officers o ON o.id = c.officer_id " +
                     "LEFT JOIN evidence e ON e.case_id = c.id " +
                     "GROUP BY c.id, c.case_number, c.incident_date, o.name, o.badge " +
                     "ORDER BY c.incident_date DESC";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) rows.add(new String[]{
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                String.valueOf(rs.getInt(5))
            });
        }
        return rows;
    }

    private static void setString(PreparedStatement ps, int index, String value) {
        try { ps.setString(index, value); } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── Case Report ───────────────────────────────────────────────────────────

    /** All cases for the case report selector. Returns rows: [case_number, incident_date, officer_name, status, evidence_count, persons_count] */
    public List<String[]> allCasesForReport() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT c.case_number, COALESCE(c.incident_date,'') AS incident_date, " +
                     "COALESCE(o.name,'') AS officer_name, COALESCE(c.status,'') AS status, " +
                     "COUNT(DISTINCT e.id) AS evidence_count, COUNT(DISTINCT cp.id) AS persons_count " +
                     "FROM cases c LEFT JOIN officers o ON o.id = c.officer_id " +
                     "LEFT JOIN evidence e ON e.case_id = c.id " +
                     "LEFT JOIN case_persons cp ON cp.case_id = c.id " +
                     "GROUP BY c.id, c.case_number, c.incident_date, o.name, c.status " +
                     "ORDER BY c.incident_date DESC";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) rows.add(new String[]{
                rs.getString("case_number"), rs.getString("incident_date"),
                rs.getString("officer_name"), rs.getString("status"),
                String.valueOf(rs.getInt("evidence_count")), String.valueOf(rs.getInt("persons_count"))
            });
        }
        return rows;
    }

    public static final String[] CASE_REPORT_HEADERS =
        {"Case #", "Incident Date", "Officer", "Status", "Evidence Count", "Persons"};

    // Evidence for a specific case (drill-down)
    public static final String[] CASE_EVIDENCE_HEADERS =
        {"Barcode", "Type", "Status", "Storage Location", "Collection Date", "Collected By", "Description"};

    public List<String[]> caseEvidenceDetail(String caseNumber) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT e.barcode, COALESCE(e.evidence_type,'') AS evidence_type, COALESCE(e.status,'') AS status, " +
                     "COALESCE(e.storage_location,'') AS storage_location, COALESCE(e.collection_date,'') AS collection_date, " +
                     "COALESCE(o.name,'') AS officer_name, COALESCE(e.description,'') AS description " +
                     "FROM evidence e " +
                     "JOIN cases c ON c.id = e.case_id " +
                     "LEFT JOIN officers o ON o.id = e.collected_by_officer_id " +
                     "WHERE c.case_number = ? ORDER BY e.collection_date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)
                });
            }
        }
        return rows;
    }

    // Persons associated with a specific case
    public static final String[] CASE_PERSONS_HEADERS =
        {"Name", "Role", "DOB", "Address", "City", "State", "Phone"};

    public List<String[]> casePersonsDetail(String caseNumber) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT p.full_name, COALESCE(cp.role,'') AS role, COALESCE(p.dob,'') AS dob, " +
                     "COALESCE(p.address,'') AS address, COALESCE(p.city,'') AS city, " +
                     "COALESCE(p.state,'') AS state, COALESCE(p.phone,'') AS phone " +
                     "FROM persons p " +
                     "JOIN case_persons cp ON cp.person_id = p.id " +
                     "JOIN cases c ON c.id = cp.case_id " +
                     "WHERE c.case_number = ? ORDER BY cp.role, p.full_name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)
                });
            }
        }
        return rows;
    }

    // Vehicles (evidence of type Vehicle) for a specific case
    public static final String[] CASE_VEHICLES_HEADERS =
        {"Barcode", "Make", "Model", "Year", "Color", "VIN", "License", "State", "Status", "Impounded"};

    public List<String[]> caseVehiclesDetail(String caseNumber) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT e.barcode, COALESCE(e.vehicle_make,'') AS make, COALESCE(e.vehicle_model,'') AS model, " +
                     "COALESCE(e.vehicle_year,'') AS yr, COALESCE(e.vehicle_color,'') AS color, " +
                     "COALESCE(e.vehicle_vin,'') AS vin, COALESCE(e.vehicle_license_plate,'') AS plate, " +
                     "COALESCE(e.vehicle_license_state,'') AS plate_state, COALESCE(e.status,'') AS status, " +
                     "IF(e.vehicle_impounded,1,0) AS impounded " +
                     "FROM evidence e JOIN cases c ON c.id = e.case_id " +
                     "WHERE c.case_number = ? AND e.evidence_type = 'Vehicle' ORDER BY e.barcode";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                    rs.getString(9), rs.getInt("impounded") == 1 ? "Yes" : "No"
                });
            }
        }
        return rows;
    }
}
