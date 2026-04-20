package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.Person;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<Person> findAll() throws SQLException {
        List<Person> list = new ArrayList<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT id, full_name FROM persons ORDER BY full_name")) {
            while (rs.next()) list.add(new Person(rs.getInt("id"), rs.getString("full_name")));
        }
        return list;
    }

    public Person findById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT id, full_name FROM persons WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Person(rs.getInt("id"), rs.getString("full_name")) : null;
            }
        }
    }

    public Person save(Person p) throws SQLException {
        if (p.getId() == 0) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO persons (full_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getFullName());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) p.setId(keys.getInt(1)); }
            }
        }
        return p;
    }
}
