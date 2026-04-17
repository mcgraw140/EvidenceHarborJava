package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.Officer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OfficerRepository {
    private final Connection conn;

    public OfficerRepository() { this.conn = DatabaseManager.getInstance().getConnection(); }

    public List<Officer> findAll() throws SQLException {
        List<Officer> list = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM officers ORDER BY name")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Officer> search(String query) throws SQLException {
        List<Officer> list = new ArrayList<>();
        String q = "%" + query.toLowerCase() + "%";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM officers WHERE lower(name) LIKE ? OR lower(badge) LIKE ? OR lower(username) LIKE ? ORDER BY name")) {
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public Officer findById(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM officers WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public Officer findByName(String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM officers WHERE lower(name)=lower(?) LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public boolean usernameExists(String username, int excludeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM officers WHERE lower(username)=lower(?) AND id != ?")) {
            ps.setString(1, username); ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public Officer save(Officer o) throws SQLException {
        if (o.getId() == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO officers (name, badge, username, password_hash, role, status, is_external) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, o.getName());
                ps.setString(2, o.getBadge());
                ps.setString(3, o.getUsername());
                ps.setString(4, o.getPasswordHash());
                ps.setString(5, o.getRole() != null ? o.getRole() : "officer");
                ps.setString(6, o.getStatus() != null ? o.getStatus() : "Active");
                ps.setInt(7, o.isExternal() ? 1 : 0);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) o.setId(keys.getInt(1)); }
            }
        } else {
            String sql = o.getPasswordHash() != null && !o.getPasswordHash().isBlank()
                    ? "UPDATE officers SET name=?, badge=?, username=?, password_hash=?, role=?, status=?, is_external=? WHERE id=?"
                    : "UPDATE officers SET name=?, badge=?, username=?, role=?, status=?, is_external=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, o.getName());
                ps.setString(2, o.getBadge());
                ps.setString(3, o.getUsername());
                if (o.getPasswordHash() != null && !o.getPasswordHash().isBlank()) {
                    ps.setString(4, o.getPasswordHash());
                    ps.setString(5, o.getRole());
                    ps.setString(6, o.getStatus());
                    ps.setInt(7, o.isExternal() ? 1 : 0);
                    ps.setInt(8, o.getId());
                } else {
                    ps.setString(4, o.getRole());
                    ps.setString(5, o.getStatus());
                    ps.setInt(6, o.isExternal() ? 1 : 0);
                    ps.setInt(7, o.getId());
                }
                ps.executeUpdate();
            }
        }
        return o;
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM officers WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public void savePermissions(int officerId, String permissions) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE officers SET permissions=? WHERE id=?")) {
            ps.setString(1, permissions);
            ps.setInt(2, officerId);
            ps.executeUpdate();
        }
    }

    private Officer map(ResultSet rs) throws SQLException {
        Officer o = new Officer();
        o.setId(rs.getInt("id"));
        o.setName(rs.getString("name"));
        o.setBadge(rs.getString("badge"));
        try { o.setUsername(rs.getString("username")); } catch (SQLException ignored) {}
        try { o.setPasswordHash(rs.getString("password_hash")); } catch (SQLException ignored) {}
        try { o.setRole(rs.getString("role")); } catch (SQLException ignored) {}
        try { o.setStatus(rs.getString("status")); } catch (SQLException ignored) {}
        try { o.setExternal(rs.getInt("is_external") == 1); } catch (SQLException ignored) {}
        try { o.setPermissions(rs.getString("permissions")); } catch (SQLException ignored) {}
        if (o.getRole() == null) o.setRole("officer");
        if (o.getStatus() == null) o.setStatus("Active");
        return o;
    }
}
