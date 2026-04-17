package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QmRepository {

    private Connection conn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Equipment
    // ══════════════════════════════════════════════════════════════════════════

    public List<QmEquipment> findAllEquipment() {
        return findEquipmentWhere(null, null);
    }

    public List<QmEquipment> findEquipmentByCategory(String category) {
        return findEquipmentWhere("category = ?", category);
    }

    public List<QmEquipment> findEquipmentByStatus(String status) {
        return findEquipmentWhere("status = ?", status);
    }

    private List<QmEquipment> findEquipmentWhere(String clause, String param) {
        List<QmEquipment> result = new ArrayList<>();
        String sql = "SELECT * FROM qm_equipment" + (clause != null ? " WHERE " + clause : "") + " ORDER BY name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (param != null) ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapEquipment(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public QmEquipment createEquipment(String name, String category, String make, String model, String serial, String notes) {
        String sql = "INSERT INTO qm_equipment (name, category, make, model, serial, notes) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, category);
            ps.setString(3, make); ps.setString(4, model);
            ps.setString(5, serial); ps.setString(6, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findEquipmentById(keys.getInt(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void updateEquipment(int id, String name, String category, String make, String model, String serial, String status, String notes) {
        String sql = "UPDATE qm_equipment SET name=?, category=?, make=?, model=?, serial=?, status=?, notes=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, category);
            ps.setString(3, make); ps.setString(4, model);
            ps.setString(5, serial); ps.setString(6, status);
            ps.setString(7, notes); ps.setInt(8, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteEquipment(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM qm_equipment WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public QmEquipment findEquipmentById(int id) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM qm_equipment WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapEquipment(rs); }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Assignments
    // ══════════════════════════════════════════════════════════════════════════

    public List<QmAssignment> findActiveAssignments() {
        List<QmAssignment> result = new ArrayList<>();
        String sql = "SELECT a.*, e.name AS eq_name, e.category AS eq_cat, e.serial AS eq_serial " +
                     "FROM qm_assignments a JOIN qm_equipment e ON e.id = a.equipment_id " +
                     "WHERE a.returned_date IS NULL ORDER BY a.assigned_date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapAssignment(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public List<QmAssignment> findAssignmentsByOfficer(String officerName) {
        List<QmAssignment> result = new ArrayList<>();
        String sql = "SELECT a.*, e.name AS eq_name, e.category AS eq_cat, e.serial AS eq_serial " +
                     "FROM qm_assignments a JOIN qm_equipment e ON e.id = a.equipment_id " +
                     "WHERE a.officer_name = ? AND a.returned_date IS NULL ORDER BY a.assigned_date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, officerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapAssignment(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public void assignEquipment(int equipmentId, String officerName, String notes) {
        String sql = "INSERT INTO qm_assignments (equipment_id, officer_name, notes) VALUES (?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, equipmentId); ps.setString(2, officerName); ps.setString(3, notes);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        updateEquipmentStatus(equipmentId, "Assigned");
    }

    public void returnEquipment(int assignmentId, int equipmentId) {
        String sql = "UPDATE qm_assignments SET returned_date = datetime('now') WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, assignmentId); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        // Check if any active assignments remain
        String check = "SELECT COUNT(*) FROM qm_assignments WHERE equipment_id=? AND returned_date IS NULL";
        try (PreparedStatement ps = conn().prepareStatement(check)) {
            ps.setInt(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) updateEquipmentStatus(equipmentId, "Available");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateEquipmentStatus(int equipmentId, String status) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE qm_equipment SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setInt(2, equipmentId); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ammunition
    // ══════════════════════════════════════════════════════════════════════════

    public List<QmAmmunition> findAllAmmunition() {
        List<QmAmmunition> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM qm_ammunition ORDER BY caliber");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapAmmo(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public void createAmmunition(String caliber, int quantity, String location, String notes) {
        String sql = "INSERT INTO qm_ammunition (caliber, quantity, location, notes) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caliber); ps.setInt(2, quantity);
            ps.setString(3, location); ps.setString(4, notes);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateAmmunition(int id, String caliber, int quantity, String location, String notes) {
        String sql = "UPDATE qm_ammunition SET caliber=?, quantity=?, location=?, notes=?, updated_at=datetime('now') WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caliber); ps.setInt(2, quantity);
            ps.setString(3, location); ps.setString(4, notes); ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteAmmunition(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM qm_ammunition WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Vehicle Impound
    // ══════════════════════════════════════════════════════════════════════════

    public List<QmVehicleImpound> findAllVehicles() {
        List<QmVehicleImpound> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM qm_vehicle_impound ORDER BY impound_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapVehicle(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public List<QmVehicleImpound> findVehiclesByCase(int caseId) {
        List<QmVehicleImpound> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM qm_vehicle_impound WHERE case_id=? ORDER BY impound_date DESC")) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapVehicle(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public void createVehicle(String make, String model, String year, String vin, String plate,
                               String color, String impoundDate, String reason, String notes) {
        String sql = "INSERT INTO qm_vehicle_impound (make, model, year, vin, plate, color, impound_date, reason, notes) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, make); ps.setString(2, model); ps.setString(3, year);
            ps.setString(4, vin); ps.setString(5, plate); ps.setString(6, color);
            ps.setString(7, impoundDate); ps.setString(8, reason); ps.setString(9, notes);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void createVehicleForCase(int caseId, String make, String model, String year, String vin, String plate,
                                     String color, String impoundDate, String reason, String notes) {
        String sql = "INSERT INTO qm_vehicle_impound (case_id, make, model, year, vin, plate, color, impound_date, reason, notes) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, caseId);
            ps.setString(2, make); ps.setString(3, model); ps.setString(4, year);
            ps.setString(5, vin); ps.setString(6, plate); ps.setString(7, color);
            ps.setString(8, impoundDate); ps.setString(9, reason); ps.setString(10, notes);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void releaseVehicle(int id) {
        String sql = "UPDATE qm_vehicle_impound SET status='Released', release_date=date('now') WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteVehicle(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM qm_vehicle_impound WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inventory Audit
    // ══════════════════════════════════════════════════════════════════════════

    public List<QmInventoryAudit> findAllAudits() {
        List<QmInventoryAudit> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM qm_inventory_audits ORDER BY created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapAudit(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public void createAudit(String createdBy, String notes) {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO qm_inventory_audits (created_by, notes) VALUES (?,?)")) {
            ps.setString(1, createdBy); ps.setString(2, notes); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void completeAudit(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE qm_inventory_audits SET status='Completed', completed_at=datetime('now') WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inventory summary (for Inventory Levels screen)
    // ══════════════════════════════════════════════════════════════════════════

    public List<String[]> getInventorySummary() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT category, status, COUNT(*) AS cnt FROM qm_equipment GROUP BY category, status ORDER BY category, status";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new String[]{ rs.getString("category"), rs.getString("status"), String.valueOf(rs.getInt("cnt")) });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Mappers
    // ══════════════════════════════════════════════════════════════════════════

    private QmEquipment mapEquipment(ResultSet rs) throws SQLException {
        QmEquipment e = new QmEquipment();
        e.setId(rs.getInt("id"));
        e.setName(rs.getString("name"));
        e.setCategory(rs.getString("category"));
        e.setMake(rs.getString("make"));
        e.setModel(rs.getString("model"));
        e.setSerial(rs.getString("serial"));
        e.setStatus(rs.getString("status"));
        e.setNotes(rs.getString("notes"));
        e.setCreatedAt(rs.getString("created_at"));
        return e;
    }

    private QmAssignment mapAssignment(ResultSet rs) throws SQLException {
        QmAssignment a = new QmAssignment();
        a.setId(rs.getInt("id"));
        a.setEquipmentId(rs.getInt("equipment_id"));
        a.setOfficerName(rs.getString("officer_name"));
        a.setAssignedDate(rs.getString("assigned_date"));
        a.setReturnedDate(rs.getString("returned_date"));
        a.setNotes(rs.getString("notes"));
        try { a.setEquipmentName(rs.getString("eq_name")); } catch (SQLException ignored) {}
        try { a.setEquipmentCategory(rs.getString("eq_cat")); } catch (SQLException ignored) {}
        try { a.setEquipmentSerial(rs.getString("eq_serial")); } catch (SQLException ignored) {}
        return a;
    }

    private QmAmmunition mapAmmo(ResultSet rs) throws SQLException {
        QmAmmunition a = new QmAmmunition();
        a.setId(rs.getInt("id"));
        a.setCaliber(rs.getString("caliber"));
        a.setQuantity(rs.getInt("quantity"));
        a.setLocation(rs.getString("location"));
        a.setUpdatedAt(rs.getString("updated_at"));
        a.setNotes(rs.getString("notes"));
        return a;
    }

    private QmVehicleImpound mapVehicle(ResultSet rs) throws SQLException {
        QmVehicleImpound v = new QmVehicleImpound();
        v.setId(rs.getInt("id"));
        try { v.setCaseId(rs.getInt("case_id")); } catch (SQLException ignored) {}
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setYear(rs.getString("year"));
        v.setVin(rs.getString("vin"));
        v.setPlate(rs.getString("plate"));
        v.setColor(rs.getString("color"));
        v.setImpoundDate(rs.getString("impound_date"));
        v.setReleaseDate(rs.getString("release_date"));
        v.setStatus(rs.getString("status"));
        v.setReason(rs.getString("reason"));
        v.setNotes(rs.getString("notes"));
        v.setCreatedAt(rs.getString("created_at"));
        return v;
    }

    private QmInventoryAudit mapAudit(ResultSet rs) throws SQLException {
        QmInventoryAudit a = new QmInventoryAudit();
        a.setId(rs.getInt("id"));
        a.setCreatedAt(rs.getString("created_at"));
        a.setCompletedAt(rs.getString("completed_at"));
        a.setCreatedBy(rs.getString("created_by"));
        a.setStatus(rs.getString("status"));
        a.setNotes(rs.getString("notes"));
        return a;
    }
}
