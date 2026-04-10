package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.LookupItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LookupRepository {
    private final Connection conn;

    public LookupRepository() { this.conn = DatabaseManager.getInstance().getConnection(); }

    public List<LookupItem> getCaliberList() throws SQLException { return getAll("ammunition_calibers"); }
    public List<String> getCalibers() throws SQLException { return getNames("ammunition_calibers"); }
    public List<String> getElectronicTypes() throws SQLException { return getNames("electronic_types"); }
    public List<String> getNarcoticsTypes() throws SQLException { return getNames("narcotics_types"); }
    public List<LookupItem> getElectronicTypeItems() throws SQLException { return getAll("electronic_types"); }
    public List<LookupItem> getNarcoticsTypeItems() throws SQLException { return getAll("narcotics_types"); }

    public LookupItem addCaliber(String name) throws SQLException { return addTo("ammunition_calibers", name); }
    public LookupItem addElectronicType(String name) throws SQLException { return addTo("electronic_types", name); }
    public LookupItem addNarcoticsType(String name) throws SQLException { return addTo("narcotics_types", name); }

    private List<LookupItem> getAll(String table) throws SQLException {
        List<LookupItem> list = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name FROM " + table + " ORDER BY name")) {
            while (rs.next()) list.add(new LookupItem(rs.getInt("id"), rs.getString("name")));
        }
        return list;
    }

    private List<String> getNames(String table) throws SQLException {
        List<String> list = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT name FROM " + table + " ORDER BY name")) {
            while (rs.next()) list.add(rs.getString("name"));
        }
        return list;
    }

    private LookupItem addTo(String table, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO " + table + " (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return new LookupItem(keys.getInt(1), name);
            }
        }
        // already existed — fetch it
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM " + table + " WHERE name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new LookupItem(rs.getInt("id"), rs.getString("name"));
            }
        }
        return null;
    }
}
