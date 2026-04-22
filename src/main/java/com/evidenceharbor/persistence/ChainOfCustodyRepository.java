package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.ChainOfCustody;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChainOfCustodyRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<ChainOfCustody> findByEvidence(int evidenceId) throws SQLException {
        List<ChainOfCustody> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM chain_of_custody WHERE evidence_id=? ORDER BY timestamp ASC")) {
            ps.setInt(1, evidenceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public ChainOfCustody addEntry(ChainOfCustody c) throws SQLException {
        String sql = "INSERT INTO chain_of_custody (evidence_id,action,performed_by,performed_by_name," +
                "from_location,to_location,to_person,reason,notes,signature_data) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getEvidenceId());
            ps.setString(2, c.getAction());
            ps.setString(3, c.getPerformedBy());
            ps.setString(4, c.getPerformedByName());
            ps.setString(5, c.getFromLocation());
            ps.setString(6, c.getToLocation());
            ps.setString(7, c.getToPerson());
            ps.setString(8, c.getReason());
            ps.setString(9, c.getNotes());
            ps.setString(10, c.getSignatureData());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
        }
        StringBuilder d = new StringBuilder();
        d.append("Chain of custody: ").append(c.getAction() == null ? "entry" : c.getAction());
        if (c.getFromLocation() != null && !c.getFromLocation().isBlank())
            d.append("; from ").append(c.getFromLocation());
        if (c.getToLocation() != null && !c.getToLocation().isBlank())
            d.append("; to ").append(c.getToLocation());
        if (c.getToPerson() != null && !c.getToPerson().isBlank())
            d.append("; to person ").append(c.getToPerson());
        if (c.getPerformedByName() != null && !c.getPerformedByName().isBlank())
            d.append("; performed by ").append(c.getPerformedByName());
        if (c.getNotes() != null && !c.getNotes().isBlank())
            d.append("; notes: ").append(c.getNotes());
        AuditLogger.log("Evidence", "UPDATE", "Evidence",
                String.valueOf(c.getEvidenceId()), d.toString());
        return c;
    }

    private ChainOfCustody map(ResultSet rs) throws SQLException {
        ChainOfCustody c = new ChainOfCustody();
        c.setId(rs.getInt("id"));
        c.setEvidenceId(rs.getInt("evidence_id"));
        c.setAction(rs.getString("action"));
        c.setPerformedBy(rs.getString("performed_by"));
        c.setPerformedByName(rs.getString("performed_by_name"));
        c.setFromLocation(rs.getString("from_location"));
        c.setToLocation(rs.getString("to_location"));
        c.setToPerson(rs.getString("to_person"));
        c.setReason(rs.getString("reason"));
        c.setNotes(rs.getString("notes"));
        c.setSignatureData(rs.getString("signature_data"));
        c.setTimestamp(rs.getString("timestamp"));
        return c;
    }
}
