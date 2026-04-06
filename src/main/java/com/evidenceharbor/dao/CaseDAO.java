package com.evidenceharbor.dao;

import com.evidenceharbor.model.Case;
import com.evidenceharbor.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CaseDAO {

    public void insertCase(Case caseItem) throws SQLException {
        String sql = "INSERT INTO Cases (caseNumber, description, officer, date, location, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, caseItem.getCaseNumber());
            pstmt.setString(2, caseItem.getDescription());
            pstmt.setString(3, caseItem.getOfficer());
            pstmt.setString(4, caseItem.getDate().toString());
            pstmt.setString(5, caseItem.getLocation());
            pstmt.setString(6, caseItem.getNotes());
            pstmt.executeUpdate();
        }
    }

    public Case getCaseByNumber(String caseNumber) throws SQLException {
        String sql = "SELECT * FROM Cases WHERE caseNumber = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, caseNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Case(
                    rs.getString("caseNumber"),
                    rs.getString("description"),
                    rs.getString("officer"),
                    LocalDate.parse(rs.getString("date")),
                    rs.getString("location"),
                    rs.getString("notes")
                );
            }
        }
        return null;
    }

    public List<Case> getAllCases() throws SQLException {
        List<Case> cases = new ArrayList<>();
        String sql = "SELECT * FROM Cases";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cases.add(new Case(
                    rs.getString("caseNumber"),
                    rs.getString("description"),
                    rs.getString("officer"),
                    LocalDate.parse(rs.getString("date")),
                    rs.getString("location"),
                    rs.getString("notes")
                ));
            }
        }
        return cases;
    }

    public void updateCase(Case caseItem) throws SQLException {
        String sql = "UPDATE Cases SET description = ?, officer = ?, date = ?, location = ?, notes = ? WHERE caseNumber = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, caseItem.getDescription());
            pstmt.setString(2, caseItem.getOfficer());
            pstmt.setString(3, caseItem.getDate().toString());
            pstmt.setString(4, caseItem.getLocation());
            pstmt.setString(5, caseItem.getNotes());
            pstmt.setString(6, caseItem.getCaseNumber());
            pstmt.executeUpdate();
        }
    }

    public void deleteCase(String caseNumber) throws SQLException {
        String sql = "DELETE FROM Cases WHERE caseNumber = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, caseNumber);
            pstmt.executeUpdate();
        }
    }
}