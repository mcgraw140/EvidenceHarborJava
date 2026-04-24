package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.Charge;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChargeRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<Charge> findAll() throws SQLException {
        List<Charge> list = new ArrayList<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT id, code, description FROM charges ORDER BY code")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Charge save(Charge c) throws SQLException {
        boolean isCreate = c.getId() == 0;
        if (isCreate) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO charges (code, description) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getCode()); ps.setString(2, c.getDescription());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) c.setId(keys.getInt(1)); }
            }
            AuditLogger.log("Cases", "CREATE", "Charge", String.valueOf(c.getId()),
                    "Created charge " + c.getCode() + " — " + c.getDescription());
        }
        return c;
    }

    public void addToCase(int caseId, int chargeId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT IGNORE INTO case_charges (case_id, charge_id) VALUES (?,?)")) {
            ps.setInt(1, caseId); ps.setInt(2, chargeId); ps.executeUpdate();
        }
        AuditLogger.log("Cases", "UPDATE", "Case", String.valueOf(caseId),
                "Added charge " + describeCharge(chargeId) + " to case " + describeCase(caseId));
    }

    public void removeFromCase(int caseId, int chargeId) throws SQLException {
        String chargeDesc = describeCharge(chargeId);
        String caseDesc = describeCase(caseId);
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM case_charges WHERE case_id=? AND charge_id=?")) {
            ps.setInt(1, caseId); ps.setInt(2, chargeId); ps.executeUpdate();
        }
        AuditLogger.log("Cases", "UPDATE", "Case", String.valueOf(caseId),
                "Removed charge " + chargeDesc + " from case " + caseDesc);
    }

    private String describeCharge(int chargeId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT code, description FROM charges WHERE id=?")) {
            ps.setInt(1, chargeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String code = rs.getString(1);
                    String desc = rs.getString(2);
                    return (code == null ? "" : code) + " — " + (desc == null ? "" : desc)
                            + " (#" + chargeId + ")";
                }
            }
        } catch (SQLException ignore) {}
        return "#" + chargeId;
    }

    private String describeCase(int caseId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT case_number FROM cases WHERE id=?")) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1) + " (#" + caseId + ")";
            }
        } catch (SQLException ignore) {}
        return "#" + caseId;
    }

    public List<Charge> findByCase(int caseId) throws SQLException {
        List<Charge> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT c.id, c.code, c.description FROM charges c " +
                "JOIN case_charges cc ON cc.charge_id=c.id WHERE cc.case_id=? ORDER BY c.code")) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    private Charge map(ResultSet rs) throws SQLException {
        return new Charge(rs.getInt("id"), rs.getString("code"), rs.getString("description"));
    }
}
