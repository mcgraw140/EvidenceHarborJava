package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.AuditLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {
    private final Connection conn;

    public AuditLogRepository() { this.conn = DatabaseManager.getInstance().getConnection(); }

    public List<AuditLog> findAll(int limit) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<AuditLog> search(String query, String module, String action,
                                  String dateFrom, String dateTo) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM audit_logs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append("AND (user_name LIKE ? OR action LIKE ? OR module LIKE ? OR details LIKE ?) ");
            String q = "%" + query + "%";
            params.add(q); params.add(q); params.add(q); params.add(q);
        }
        if (module != null && !module.isBlank() && !"All".equals(module)) {
            sql.append("AND module = ? ");
            params.add(module);
        }
        if (action != null && !action.isBlank()) {
            sql.append("AND action = ? ");
            params.add(action);
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            sql.append("AND timestamp >= ? ");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isBlank()) {
            sql.append("AND timestamp <= ? ");
            params.add(dateTo + " 23:59:59");
        }
        sql.append("ORDER BY timestamp DESC LIMIT 500");
        List<AuditLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void insert(String userName, String action, String module,
                       String entityType, String entityId, String details) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_name,action,module,entity_type,entity_id,details) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userName);
            ps.setString(2, action);
            ps.setString(3, module);
            ps.setString(4, entityType);
            ps.setString(5, entityId);
            ps.setString(6, details);
            ps.executeUpdate();
        }
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getInt("id"));
        a.setTimestamp(rs.getString("timestamp"));
        a.setUserName(rs.getString("user_name"));
        a.setAction(rs.getString("action"));
        a.setModule(rs.getString("module"));
        a.setEntityType(rs.getString("entity_type"));
        a.setEntityId(rs.getString("entity_id"));
        a.setDetails(rs.getString("details"));
        return a;
    }
}
