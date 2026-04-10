package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CaseRepository {
    private final Connection conn;
    private final OfficerRepository officerRepo;
    private final PersonRepository personRepo;
    private final ChargeRepository chargeRepo;

    public CaseRepository() {
        this.conn = DatabaseManager.getInstance().getConnection();
        this.officerRepo = new OfficerRepository();
        this.personRepo = new PersonRepository();
        this.chargeRepo = new ChargeRepository();
    }

    public List<Case> findAll() throws SQLException {
        List<Case> list = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(
                "SELECT c.id, c.case_number, c.incident_date, c.officer_id, o.name, o.badge " +
                "FROM cases c JOIN officers o ON o.id = c.officer_id " +
                "ORDER BY c.incident_date DESC")) {
            while (rs.next()) list.add(mapBasic(rs));
        }
        for (Case c : list) populateRelations(c);
        return list;
    }

    public List<Case> search(String query) throws SQLException {
        String q = "%" + query.toLowerCase() + "%";
        List<Case> list = new ArrayList<>();
        String sql = "SELECT DISTINCT c.id, c.case_number, c.incident_date, c.officer_id, o.name, o.badge " +
                     "FROM cases c JOIN officers o ON o.id = c.officer_id " +
                     "LEFT JOIN case_persons cp ON cp.case_id = c.id " +
                     "LEFT JOIN persons p ON p.id = cp.person_id " +
                     "WHERE lower(c.case_number) LIKE ? OR lower(o.name) LIKE ? OR lower(p.full_name) LIKE ? " +
                     "ORDER BY c.incident_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBasic(rs)); }
        }
        for (Case c : list) populateRelations(c);
        return list;
    }

    public Case findById(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.id, c.case_number, c.incident_date, c.officer_id, o.name, o.badge " +
                "FROM cases c JOIN officers o ON o.id=c.officer_id WHERE c.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Case c = mapBasic(rs);
                populateRelations(c);
                return c;
            }
        }
    }

    public Case save(Case c) throws SQLException {
        if (c.getId() == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO cases (case_number, incident_date, officer_id) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getCaseNumber());
                ps.setString(2, c.getIncidentDate().toString());
                ps.setInt(3, c.getOfficer().getId());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) c.setId(keys.getInt(1)); }
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE cases SET case_number=?, incident_date=?, officer_id=? WHERE id=?")) {
                ps.setString(1, c.getCaseNumber());
                ps.setString(2, c.getIncidentDate().toString());
                ps.setInt(3, c.getOfficer().getId());
                ps.setInt(4, c.getId());
                ps.executeUpdate();
            }
        }
        return c;
    }

    public void associatePerson(int caseId, int personId, String role) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO case_persons (case_id, person_id, role) VALUES (?,?,?)")) {
            ps.setInt(1, caseId); ps.setInt(2, personId); ps.setString(3, role); ps.executeUpdate();
        }
    }

    public void removePerson(int casePersonId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM case_persons WHERE id=?")) {
            ps.setInt(1, casePersonId); ps.executeUpdate();
        }
    }

    private void populateRelations(Case c) throws SQLException {
        // persons
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT cp.id, cp.role, p.id as pid, p.full_name FROM case_persons cp " +
                "JOIN persons p ON p.id=cp.person_id WHERE cp.case_id=?")) {
            ps.setInt(1, c.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Person p = new Person(rs.getInt("pid"), rs.getString("full_name"));
                    c.getPersons().add(new CasePerson(rs.getInt("id"), c.getId(), p, rs.getString("role")));
                }
            }
        }
        c.setCharges(chargeRepo.findByCase(c.getId()));
    }

    private Case mapBasic(ResultSet rs) throws SQLException {
        Officer o = new Officer(rs.getInt("officer_id"), rs.getString("name"), rs.getString("badge"));
        return new Case(rs.getInt("id"), rs.getString("case_number"),
                LocalDate.parse(rs.getString("incident_date")), o);
    }
}
