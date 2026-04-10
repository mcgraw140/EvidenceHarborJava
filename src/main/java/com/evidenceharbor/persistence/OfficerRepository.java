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
             ResultSet rs = s.executeQuery("SELECT id, name, badge FROM officers ORDER BY name")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Officer findById(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, name, badge FROM officers WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Officer save(Officer o) throws SQLException {
        if (o.getId() == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO officers (name, badge) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, o.getName());
                ps.setString(2, o.getBadge());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) o.setId(keys.getInt(1)); }
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE officers SET name=?, badge=? WHERE id=?")) {
                ps.setString(1, o.getName()); ps.setString(2, o.getBadge()); ps.setInt(3, o.getId());
                ps.executeUpdate();
            }
        }
        return o;
    }

    private Officer map(ResultSet rs) throws SQLException {
        return new Officer(rs.getInt("id"), rs.getString("name"), rs.getString("badge"));
    }
}
