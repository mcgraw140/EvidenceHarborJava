package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.Evidence;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EvidenceRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<Evidence> findByCase(int caseId) throws SQLException {
        List<Evidence> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM evidence WHERE case_id=? ORDER BY collection_date DESC")) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Evidence> findAll() throws SQLException {
        List<Evidence> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM evidence ORDER BY id DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public String generateBarcode(String evidenceType) throws SQLException {
        // Title-case the type, strip non-alphanumeric, cap at 12 chars
        String cleaned = evidenceType == null ? "Unknown" : evidenceType.replaceAll("[^A-Za-z0-9]", "");
        String typeCode = cleaned.isEmpty() ? "Unknown"
                : Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1).toLowerCase();
        if (typeCode.length() > 12) typeCode = typeCode.substring(0, 12);
        int year = LocalDate.now().getYear();
        String prefix = year + "-" + typeCode + "-";
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT barcode FROM evidence WHERE barcode LIKE ? ORDER BY barcode DESC LIMIT 1")) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int next = 1;
                if (rs.next()) {
                    String[] parts = rs.getString("barcode").split("-");
                    try { next = Integer.parseInt(parts[parts.length - 1]) + 1; } catch (NumberFormatException ignored) {}
                }
                return prefix + String.format("%03d", next);
            }
        }
    }

    public Evidence save(Evidence e) throws SQLException {
        if (e.getBarcode() == null || e.getBarcode().isBlank()) {
            e.setBarcode(generateBarcode(e.getEvidenceType()));
        }
        String sql = "INSERT INTO evidence (barcode,case_id,collected_by_officer_id,collected_from_person_id," +
                "collection_date,specific_location,address,city,state,zip,evidence_type,description,status,storage_location," +
                "ammo_caliber,ammo_quantity,ammo_grain_weight,ammo_bullet_type,ammo_brand," +
                "bio_sample_type,bio_collection_method,bio_storage_temp,bio_suspect_name,bio_dna_analysis_requested," +
                "currency_amount,currency_denominations,currency_serial_numbers,currency_suspected_counterfeit," +
                "elec_device_type,elec_make,elec_model,elec_serial_number,elec_password_protected,elec_data_extraction_requested,elec_device_username,elec_device_password," +
                "firearm_make,firearm_model,firearm_serial_number,firearm_type,firearm_caliber,firearm_reported_stolen,firearm_loaded_when_recovered," +
                "jewelry_type,jewelry_material,jewelry_estimated_value,jewelry_engraving_or_id," +
                "narc_equip_type,narc_equip_description,narc_equip_suspected_residue,narc_equip_field_test_kit_used," +
                "narc_drug_type,narc_net_weight,narc_form,narc_packaging,narc_field_test_performed,narc_field_test_result," +
                "vehicle_body_type,vehicle_make,vehicle_model,vehicle_year,vehicle_color,vehicle_vin,vehicle_license_plate,vehicle_license_state,vehicle_reported_stolen,vehicle_impounded," +
                "weapon_type,weapon_make,weapon_model,weapon_serial_number,weapon_length,weapon_reported_stolen) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?," +
                "?,?,?,?,?,?,?,?," +
                "?,?,?,?,?,?,?," +
                "?,?,?,?," +
                "?,?,?,?," +
                "?,?,?,?,?,?," +
                "?,?,?,?,?,?,?,?,?,?," +
                "?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, e.getBarcode());
            ps.setInt(i++, e.getCaseId());
            if (e.getCollectedByOfficerId() > 0) ps.setInt(i++, e.getCollectedByOfficerId()); else ps.setNull(i++, Types.INTEGER);
            if (e.getCollectedFromPersonId() > 0) ps.setInt(i++, e.getCollectedFromPersonId()); else ps.setNull(i++, Types.INTEGER);
            ps.setString(i++, e.getCollectionDate());
            ps.setString(i++, e.getSpecificLocation());
            ps.setString(i++, e.getAddress());
            ps.setString(i++, e.getCity());
            ps.setString(i++, e.getState());
            ps.setString(i++, e.getZip());
            ps.setString(i++, e.getEvidenceType());
            ps.setString(i++, e.getDescription());
            ps.setString(i++, e.getStatus() != null ? e.getStatus() : "In Custody");
            ps.setString(i++, e.getStorageLocation());
            // Ammo
            ps.setString(i++, e.getAmmoCallber());
            ps.setString(i++, e.getAmmoQuantity());
            ps.setString(i++, e.getAmmoGrainWeight());
            ps.setString(i++, e.getAmmoBulletType());
            ps.setString(i++, e.getAmmoBrand());
            // Bio
            ps.setString(i++, e.getBioSampleType());
            ps.setString(i++, e.getBioCollectionMethod());
            ps.setString(i++, e.getBioStorageTemp());
            ps.setString(i++, e.getBioSuspectName());
            ps.setInt(i++, e.isBioDnaAnalysisRequested() ? 1 : 0);
            // Currency
            ps.setString(i++, e.getCurrencyAmount());
            ps.setString(i++, e.getCurrencyDenominations());
            ps.setString(i++, e.getCurrencySerialNumbers());
            ps.setInt(i++, e.isCurrencySuspectedCounterfeit() ? 1 : 0);
            // Electronics
            ps.setString(i++, e.getElecDeviceType());
            ps.setString(i++, e.getElecMake());
            ps.setString(i++, e.getElecModel());
            ps.setString(i++, e.getElecSerialNumber());
            ps.setInt(i++, e.isElecPasswordProtected() ? 1 : 0);
            ps.setInt(i++, e.isElecDataExtractionRequested() ? 1 : 0);
            ps.setString(i++, e.getElecDeviceUsername());
            ps.setString(i++, e.getElecDevicePassword());
            // Firearm
            ps.setString(i++, e.getFirearmMake());
            ps.setString(i++, e.getFirearmModel());
            ps.setString(i++, e.getFirearmSerialNumber());
            ps.setString(i++, e.getFirearmType());
            ps.setString(i++, e.getFirearmCaliber());
            ps.setInt(i++, e.isFirearmReportedStolen() ? 1 : 0);
            ps.setInt(i++, e.isFirearmLoadedWhenRecovered() ? 1 : 0);
            // Jewelry
            ps.setString(i++, e.getJewelryType());
            ps.setString(i++, e.getJewelryMaterial());
            ps.setString(i++, e.getJewelryEstimatedValue());
            ps.setString(i++, e.getJewelryEngravingOrId());
            // Narcotic Equipment
            ps.setString(i++, e.getNarcEquipType());
            ps.setString(i++, e.getNarcEquipDescription());
            ps.setString(i++, e.getNarcEquipSuspectedResidue());
            ps.setInt(i++, e.isNarcEquipFieldTestKitUsed() ? 1 : 0);
            // Narcotics
            ps.setString(i++, e.getNarcDrugType());
            ps.setString(i++, e.getNarcNetWeight());
            ps.setString(i++, e.getNarcForm());
            ps.setString(i++, e.getNarcPackaging());
            ps.setInt(i++, e.isNarcFieldTestPerformed() ? 1 : 0);
            ps.setString(i++, e.getNarcFieldTestResult());
            // Vehicle
            ps.setString(i++, e.getVehicleBodyType());
            ps.setString(i++, e.getVehicleMake());
            ps.setString(i++, e.getVehicleModel());
            ps.setString(i++, e.getVehicleYear());
            ps.setString(i++, e.getVehicleColor());
            ps.setString(i++, e.getVehicleVin());
            ps.setString(i++, e.getVehicleLicensePlate());
            ps.setString(i++, e.getVehicleLicenseState());
            ps.setInt(i++, e.isVehicleReportedStolen() ? 1 : 0);
            ps.setInt(i++, e.isVehicleImpounded() ? 1 : 0);
            // Weapon
            ps.setString(i++, e.getWeaponType());
            ps.setString(i++, e.getWeaponMake());
            ps.setString(i++, e.getWeaponModel());
            ps.setString(i++, e.getWeaponSerialNumber());
            ps.setString(i++, e.getWeaponLength());
            ps.setInt(i, e.isWeaponReportedStolen() ? 1 : 0);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) e.setId(keys.getInt(1)); }
        }
        return e;
    }

    private Evidence map(ResultSet rs) throws SQLException {
        Evidence e = new Evidence();
        e.setId(rs.getInt("id"));
        e.setBarcode(rs.getString("barcode"));
        e.setCaseId(rs.getInt("case_id"));
        int collectedBy = rs.getInt("collected_by_officer_id");
        if (!rs.wasNull()) e.setCollectedByOfficerId(collectedBy);
        int collectedFrom = rs.getInt("collected_from_person_id");
        if (!rs.wasNull()) e.setCollectedFromPersonId(collectedFrom);
        e.setCollectionDate(rs.getString("collection_date"));
        e.setSpecificLocation(rs.getString("specific_location"));
        e.setAddress(rs.getString("address"));
        e.setCity(rs.getString("city"));
        e.setState(rs.getString("state"));
        e.setZip(rs.getString("zip"));
        e.setEvidenceType(rs.getString("evidence_type"));
        e.setDescription(rs.getString("description"));
        e.setStatus(rs.getString("status"));
        e.setStorageLocation(rs.getString("storage_location"));

        e.setAmmoCallber(rs.getString("ammo_caliber"));
        e.setAmmoQuantity(rs.getString("ammo_quantity"));
        e.setAmmoGrainWeight(rs.getString("ammo_grain_weight"));
        e.setAmmoBulletType(rs.getString("ammo_bullet_type"));
        e.setAmmoBrand(rs.getString("ammo_brand"));

        e.setBioSampleType(rs.getString("bio_sample_type"));
        e.setBioCollectionMethod(rs.getString("bio_collection_method"));
        e.setBioStorageTemp(rs.getString("bio_storage_temp"));
        e.setBioSuspectName(rs.getString("bio_suspect_name"));
        e.setBioDnaAnalysisRequested(rs.getInt("bio_dna_analysis_requested") == 1);

        e.setCurrencyAmount(rs.getString("currency_amount"));
        e.setCurrencyDenominations(rs.getString("currency_denominations"));
        e.setCurrencySerialNumbers(rs.getString("currency_serial_numbers"));
        e.setCurrencySuspectedCounterfeit(rs.getInt("currency_suspected_counterfeit") == 1);

        e.setElecDeviceType(rs.getString("elec_device_type"));
        e.setElecMake(rs.getString("elec_make"));
        e.setElecModel(rs.getString("elec_model"));
        e.setElecSerialNumber(rs.getString("elec_serial_number"));
        e.setElecPasswordProtected(rs.getInt("elec_password_protected") == 1);
        e.setElecDataExtractionRequested(rs.getInt("elec_data_extraction_requested") == 1);
        e.setElecDeviceUsername(rs.getString("elec_device_username"));
        e.setElecDevicePassword(rs.getString("elec_device_password"));

        e.setFirearmMake(rs.getString("firearm_make"));
        e.setFirearmModel(rs.getString("firearm_model"));
        e.setFirearmSerialNumber(rs.getString("firearm_serial_number"));
        e.setFirearmType(rs.getString("firearm_type"));
        e.setFirearmCaliber(rs.getString("firearm_caliber"));
        e.setFirearmReportedStolen(rs.getInt("firearm_reported_stolen") == 1);
        e.setFirearmLoadedWhenRecovered(rs.getInt("firearm_loaded_when_recovered") == 1);

        e.setJewelryType(rs.getString("jewelry_type"));
        e.setJewelryMaterial(rs.getString("jewelry_material"));
        e.setJewelryEstimatedValue(rs.getString("jewelry_estimated_value"));
        e.setJewelryEngravingOrId(rs.getString("jewelry_engraving_or_id"));

        e.setNarcEquipType(rs.getString("narc_equip_type"));
        e.setNarcEquipDescription(rs.getString("narc_equip_description"));
        e.setNarcEquipSuspectedResidue(rs.getString("narc_equip_suspected_residue"));
        e.setNarcEquipFieldTestKitUsed(rs.getInt("narc_equip_field_test_kit_used") == 1);

        e.setNarcDrugType(rs.getString("narc_drug_type"));
        e.setNarcNetWeight(rs.getString("narc_net_weight"));
        e.setNarcForm(rs.getString("narc_form"));
        e.setNarcPackaging(rs.getString("narc_packaging"));
        e.setNarcFieldTestPerformed(rs.getInt("narc_field_test_performed") == 1);
        e.setNarcFieldTestResult(rs.getString("narc_field_test_result"));

        try { e.setVehicleBodyType(rs.getString("vehicle_body_type")); } catch (SQLException ignored) {}
        e.setVehicleMake(rs.getString("vehicle_make"));
        e.setVehicleModel(rs.getString("vehicle_model"));
        e.setVehicleYear(rs.getString("vehicle_year"));
        e.setVehicleColor(rs.getString("vehicle_color"));
        e.setVehicleVin(rs.getString("vehicle_vin"));
        e.setVehicleLicensePlate(rs.getString("vehicle_license_plate"));
        e.setVehicleLicenseState(rs.getString("vehicle_license_state"));
        e.setVehicleReportedStolen(rs.getInt("vehicle_reported_stolen") == 1);
        e.setVehicleImpounded(rs.getInt("vehicle_impounded") == 1);

        e.setWeaponType(rs.getString("weapon_type"));
        e.setWeaponMake(rs.getString("weapon_make"));
        e.setWeaponModel(rs.getString("weapon_model"));
        e.setWeaponSerialNumber(rs.getString("weapon_serial_number"));
        e.setWeaponLength(rs.getString("weapon_length"));
        e.setWeaponReportedStolen(rs.getInt("weapon_reported_stolen") == 1);
        return e;
    }

    public Evidence findById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM evidence WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public void updateStatus(int id, String status, String storageLocation) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE evidence SET status=?, storage_location=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setString(2, storageLocation);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateVehicleStatus(int id, String status, String storageLocation, boolean impounded) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE evidence SET status=?, storage_location=?, vehicle_impounded=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setString(2, storageLocation);
            ps.setInt(3, impounded ? 1 : 0);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public List<Evidence> findByStatus(String status) throws SQLException {
        List<Evidence> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM evidence WHERE status=? ORDER BY id DESC")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }
}
