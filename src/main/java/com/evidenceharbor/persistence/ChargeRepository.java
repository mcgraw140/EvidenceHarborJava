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
        if (c.getId() == 0) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO charges (code, description) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getCode()); ps.setString(2, c.getDescription());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) c.setId(keys.getInt(1)); }
            }
        }
        return c;
    }

    public void addToCase(int caseId, int chargeId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT IGNORE INTO case_charges (case_id, charge_id) VALUES (?,?)")) {
            ps.setInt(1, caseId); ps.setInt(2, chargeId); ps.executeUpdate();
        }
    }

    public void removeFromCase(int caseId, int chargeId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM case_charges WHERE case_id=? AND charge_id=?")) {
            ps.setInt(1, caseId); ps.setInt(2, chargeId); ps.executeUpdate();
        }
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
