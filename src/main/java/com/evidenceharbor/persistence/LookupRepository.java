package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.LookupItem;
import java.util.Arrays;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LookupRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ── Ammunition Calibers ───────────────────────────────────────────────────
    public List<LookupItem> getCaliberList()         throws SQLException { return getAll("ammunition_calibers"); }
    public List<String>     getCalibers()            throws SQLException { return getNames("ammunition_calibers"); }
    public LookupItem       addCaliber(String name)  throws SQLException { return addTo("ammunition_calibers", name); }
    public void             deleteCaliber(int id)    throws SQLException { deleteFrom("ammunition_calibers", id); }

    // ── Electronic Types ──────────────────────────────────────────────────────
    public List<LookupItem> getElectronicTypeItems()          throws SQLException { return getAll("electronic_types"); }
    public List<String>     getElectronicTypes()              throws SQLException { return getNames("electronic_types"); }
    public LookupItem       addElectronicType(String name)    throws SQLException { return addTo("electronic_types", name); }
    public void             deleteElectronicType(int id)      throws SQLException { deleteFrom("electronic_types", id); }

    // ── Narcotics Types ───────────────────────────────────────────────────────
    public List<LookupItem> getNarcoticsTypeItems()           throws SQLException { return getAll("narcotics_types"); }
    public List<String>     getNarcoticsTypes()               throws SQLException { return getNames("narcotics_types"); }
    public LookupItem       addNarcoticsType(String name)     throws SQLException { return addTo("narcotics_types", name); }
    public void             deleteNarcoticsType(int id)       throws SQLException { deleteFrom("narcotics_types", id); }

    // ── Weapon Types ──────────────────────────────────────────────────────────
    public List<LookupItem> getWeaponTypeItems()              throws SQLException { return getAll("weapon_types"); }
    public List<String>     getWeaponTypes()                  throws SQLException { return getNames("weapon_types"); }
    public LookupItem       addWeaponType(String name)        throws SQLException { return addTo("weapon_types", name); }
    public void             deleteWeaponType(int id)          throws SQLException { deleteFrom("weapon_types", id); }
    // ── Vehicle Types ──────────────────────────────────────────────────────────────
    public List<LookupItem> getVehicleTypeItems()             throws SQLException { return getAll("vehicle_types"); }
    public List<String>     getVehicleTypes()                 throws SQLException {
        return getNamesSeeded("vehicle_types", Arrays.asList(
                "Car (Sedan)",
                "Truck (Pickup)",
                "SUV",
                "Van (Passenger)",
                "Van (Cargo)",
                "Motorcycle",
                "Scooter / Moped",
                "ATV (4-Wheeler)",
                "UTV / SxS",
                "Bus",
                "Semi Truck (Tractor Trailer)",
                "Trailer",
                "RV / Motorhome",
                "Boat",
                "Bicycle",
                "Agricultural Tractor",
                "Heavy Equipment (Construction)",
                "Emergency Vehicle (Police/Fire/EMS)",
                "Tow Truck / Wrecker",
                "Utility Vehicle (Work Cart / Industrial)"
        ));
    }
    public LookupItem       addVehicleType(String name)       throws SQLException { return addTo("vehicle_types", name); }
    public void             deleteVehicleType(int id)         throws SQLException { deleteFrom("vehicle_types", id); }
    // ── Biological Sources ────────────────────────────────────────────────────
    public List<LookupItem> getBiologicalSourceItems()        throws SQLException { return getAll("biological_sources"); }
    public List<String>     getBiologicalSources()            throws SQLException { return getNames("biological_sources"); }
    public LookupItem       addBiologicalSource(String name)  throws SQLException { return addTo("biological_sources", name); }
    public void             deleteBiologicalSource(int id)    throws SQLException { deleteFrom("biological_sources", id); }

    // ── Narcotics Unit Types ──────────────────────────────────────────────────
    public List<LookupItem> getNarcoticsUnitTypeItems()           throws SQLException { return getAll("narcotics_unit_types"); }
    public List<String>     getNarcoticsUnitTypes()               throws SQLException { return getNames("narcotics_unit_types"); }
    public LookupItem       addNarcoticsUnitType(String name)     throws SQLException { return addTo("narcotics_unit_types", name); }
    public void             deleteNarcoticsUnitType(int id)       throws SQLException { deleteFrom("narcotics_unit_types", id); }

    // ── Person Roles ──────────────────────────────────────────────────────────
    public List<LookupItem> getPersonRoleItems()              throws SQLException { return getAll("person_roles"); }
    public List<String>     getPersonRoles()                  throws SQLException { return getNames("person_roles"); }
    public LookupItem       addPersonRole(String name)        throws SQLException { return addTo("person_roles", name); }
    public void             deletePersonRole(int id)          throws SQLException { deleteFrom("person_roles", id); }

    // ── Transfer Actions ──────────────────────────────────────────────────────
    public List<LookupItem> getTransferActionItems()          throws SQLException { return getAll("transfer_actions"); }
    public List<String>     getTransferActions()              throws SQLException { return getNames("transfer_actions"); }
    public LookupItem       addTransferAction(String name)    throws SQLException { return addTo("transfer_actions", name); }
    public void             deleteTransferAction(int id)      throws SQLException { deleteFrom("transfer_actions", id); }

    // ── Analysis Labs ─────────────────────────────────────────────────────────
    public List<LookupItem> getAnalysisLabItems()             throws SQLException { return getAll("analysis_labs"); }
    public List<String>     getAnalysisLabs()                 throws SQLException { return getNames("analysis_labs"); }
    public LookupItem       addAnalysisLab(String name)       throws SQLException { return addTo("analysis_labs", name); }
    public void             deleteAnalysisLab(int id)         throws SQLException { deleteFrom("analysis_labs", id); }

    // ── Other Agencies ────────────────────────────────────────────────────────
    public List<LookupItem> getOtherAgencyItems()             throws SQLException { return getAll("other_agencies"); }
    public List<String>     getOtherAgencies()                throws SQLException { return getNames("other_agencies"); }
    public LookupItem       addOtherAgency(String name)       throws SQLException { return addTo("other_agencies", name); }
    public void             deleteOtherAgency(int id)         throws SQLException { deleteFrom("other_agencies", id); }

    // ── Case Statuses ─────────────────────────────────────────────────────────
    public List<LookupItem> getCaseStatusItems()              throws SQLException { return getAll("case_statuses"); }
    public List<String>     getCaseStatuses()                 throws SQLException { return getNames("case_statuses"); }
    public LookupItem       addCaseStatus(String name)        throws SQLException { return addTo("case_statuses", name); }
    public void             deleteCaseStatus(int id)          throws SQLException { deleteFrom("case_statuses", id); }

    // ── Evidence Storage Locations ────────────────────────────────────────────
    public List<LookupItem> getStorageLocationItems()                 throws SQLException { return getAll("evidence_storage_locations"); }
    public List<String>     getStorageLocations()                     throws SQLException { return getNames("evidence_storage_locations"); }
    public LookupItem       addStorageLocation(String name)           throws SQLException { return addTo("evidence_storage_locations", name); }
    public void             deleteStorageLocation(int id)             throws SQLException { deleteFrom("evidence_storage_locations", id); }

    // ── Intake Locations ──────────────────────────────────────────────────────
    public List<LookupItem> getIntakeLocationItems()          throws SQLException { return getAll("intake_locations"); }
    public List<String>     getIntakeLocations()              throws SQLException { return getNames("intake_locations"); }
    public LookupItem       addIntakeLocation(String name)    throws SQLException { return addTo("intake_locations", name); }
    public void             deleteIntakeLocation(int id)      throws SQLException { deleteFrom("intake_locations", id); }
    // ── Impound Locations ────────────────────────────────────────────────────────────
    public List<LookupItem> getImpoundLocationItems()         throws SQLException { return getAll("impound_locations"); }
    public List<String>     getImpoundLocations()             throws SQLException {
        return getNamesSeeded("impound_locations", Arrays.asList(
                "Main Impound Lot",
                "Secure Impound Yard",
                "Outside Tow Yard"
        ));
    }
    public LookupItem       addImpoundLocation(String name)   throws SQLException { return addTo("impound_locations", name); }
    public void             deleteImpoundLocation(int id)     throws SQLException { deleteFrom("impound_locations", id); }
    // ── Evidence Types / Statuses ────────────────────────────────────────────
    public List<String> getEvidenceTypes() throws SQLException {
        return getNamesSeeded("evidence_types", Arrays.asList(
                "Ammunition", "Biological / DNA", "Currency", "Electronics",
                "Firearm", "Jewelry", "Narcotic Equipment", "Narcotics", "Weapon"
        ));
    }

    public List<String> getEvidenceStatuses() throws SQLException {
        return getNamesSeeded("evidence_statuses", Arrays.asList(
                "In Dropbox", "In Custody", "In Storage", "Checked In", "Checked Out",
                "Deposited", "Missing", "Destroyed", "Disbursed", "Returned to Owner", "Pending"
        ));
    }

    // ── Audit lookups ────────────────────────────────────────────────────────
    public List<String> getAuditModules() throws SQLException {
        return getNamesSeeded("audit_modules", Arrays.asList(
                "Evidence", "Cases", "Users", "Narcotics", "System"
        ));
    }

    public List<String> getAuditActions() throws SQLException {
        return getNamesSeeded("audit_actions_lookup", Arrays.asList(
                "CREATE", "UPDATE", "DELETE", "PRINT", "SAVE", "LOGIN", "LOGOUT"
        ));
    }

    public List<String> getAuditTypes() throws SQLException {
        return getNamesSeeded("audit_types", Arrays.asList("Full", "Random", "Location"));
    }

    // ── User roles / statuses ────────────────────────────────────────────────
    public List<String> getUserRoles() throws SQLException {
        return getNamesSeeded("user_roles_lookup", Arrays.asList("officer", "supervisor", "agency_admin"));
    }

    public List<String> getUserStatuses() throws SQLException {
        return getNamesSeeded("user_statuses_lookup", Arrays.asList("Active", "Inactive"));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<LookupItem> getAll(String table) throws SQLException {
        List<LookupItem> list = new ArrayList<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name FROM " + table + " ORDER BY name")) {
            while (rs.next()) list.add(new LookupItem(rs.getInt("id"), rs.getString("name")));
        }
        return list;
    }

    private List<String> getNames(String table) throws SQLException {
        List<String> list = new ArrayList<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT name FROM " + table + " ORDER BY name")) {
            while (rs.next()) list.add(rs.getString("name"));
        }
        return list;
    }

    private List<String> getNamesSeeded(String table, List<String> defaults) throws SQLException {
        if (isTablePresent(table)) {
            ensureDefaults(table, defaults);
        }
        return getNames(table);
    }

    private boolean isTablePresent(String table) throws SQLException {
        DatabaseMetaData meta = conn().getMetaData();
        try (ResultSet rs = meta.getTables(null, null, table, new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, table.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void ensureDefaults(String table, List<String> defaults) throws SQLException {
        for (String value : defaults) {
            if (value == null || value.isBlank()) continue;
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO " + table + " (name) VALUES (?)")) {
                ps.setString(1, value);
                ps.executeUpdate();
            } catch (SQLException ignored) {
                // duplicate rows are fine
            }
        }
    }

    private LookupItem addTo(String table, String name) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO " + table + " (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return new LookupItem(keys.getInt(1), name);
            }
        } catch (SQLException ignored) {
            // duplicate rows are fine; fetch existing below
        }
        try (PreparedStatement ps = conn().prepareStatement("SELECT id, name FROM " + table + " WHERE name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new LookupItem(rs.getInt("id"), rs.getString("name"));
            }
        }
        return null;
    }

    private void deleteFrom(String table, int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── Generic public API used by LookupAdminController ─────────────────────
    public List<LookupItem> getAllFromTable(String table) throws SQLException { return getAll(table); }
    public LookupItem       addToTable(String table, String name) throws SQLException { return addTo(table, name); }
    public void             deleteFromTable(String table, int id) throws SQLException { deleteFrom(table, id); }
}


