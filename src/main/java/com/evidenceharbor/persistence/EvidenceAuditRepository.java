package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.EvidenceAudit;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvidenceAuditRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<EvidenceAudit> findInProgress() throws SQLException { return findByStatus("In Progress"); }
    public List<EvidenceAudit> findCompleted()   throws SQLException { return findByStatus("Completed"); }

    public EvidenceAudit create(String auditType, String scope, String createdBy) throws SQLException {
        String sql = "INSERT INTO evidence_audits (audit_type, scope, created_by) VALUES (?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, auditType);
            ps.setString(2, scope);
            ps.setString(3, createdBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getInt(1));
            }
        }
        return null;
    }

    public EvidenceAudit findById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM evidence_audits WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public void complete(int id) throws SQLException {
        String sql = "UPDATE evidence_audits SET status='Completed', completed_at=CURRENT_TIMESTAMP WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void reopen(int id) throws SQLException {
        String sql = "UPDATE evidence_audits SET status='In Progress', completed_at=NULL WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void updateItems(int id, String itemsJson) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE evidence_audits SET items_json=? WHERE id=?")) {
            ps.setString(1, itemsJson);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private List<EvidenceAudit> findByStatus(String status) throws SQLException {
        List<EvidenceAudit> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM evidence_audits WHERE status=? ORDER BY created_at DESC")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private EvidenceAudit map(ResultSet rs) throws SQLException {
        EvidenceAudit a = new EvidenceAudit();
        a.setId(rs.getInt("id"));
        a.setCreatedAt(rs.getString("created_at"));
        a.setCompletedAt(rs.getString("completed_at"));
        a.setAuditType(rs.getString("audit_type"));
        a.setScope(rs.getString("scope"));
        a.setCreatedBy(rs.getString("created_by"));
        a.setStatus(rs.getString("status"));
        a.setItemsJson(rs.getString("items_json"));
        return a;
    }
}
