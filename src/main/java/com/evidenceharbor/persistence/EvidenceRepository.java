package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.Evidence;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EvidenceRepository {
    private final Connection conn;

    public EvidenceRepository() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public List<Evidence> findByCase(int caseId) throws SQLException {
        List<Evidence> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM evidence WHERE case_id=? ORDER BY collection_date DESC")) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Evidence> findAll() throws SQLException {
        List<Evidence> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM evidence ORDER BY id DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public String generateBarcode(String evidenceType) throws SQLException {
        String typeCode = evidenceType.replaceAll("[^A-Z0-9a-z]", "").toUpperCase();
        if (typeCode.length() > 10) typeCode = typeCode.substring(0, 10);
        int year = LocalDate.now().getYear();
        String prefix = year + "-" + typeCode + "-";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT barcode FROM evidence WHERE barcode LIKE ? ORDER BY barcode DESC LIMIT 1")) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int next = 1;
                if (rs.next()) {
                    String[] parts = rs.getString("barcode").split("-");
                    try { next = Integer.parseInt(parts[parts.length - 1]) + 1; } catch (NumberFormatException ignored) {}
                }
                return prefix + String.format("%04d", next);
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
                "elec_device_type,elec_make,elec_model,elec_serial_number,elec_password_protected,elec_data_extraction_requested," +
                "firearm_make,firearm_model,firearm_serial_number,firearm_type,firearm_caliber,firearm_reported_stolen,firearm_loaded_when_recovered," +
                "jewelry_type,jewelry_material,jewelry_estimated_value,jewelry_engraving_or_id," +
                "narc_equip_type,narc_equip_description,narc_equip_suspected_residue,narc_equip_field_test_kit_used," +
                "narc_drug_type,narc_net_weight,narc_form,narc_packaging,narc_field_test_performed,narc_field_test_result," +
                "vehicle_make,vehicle_model,vehicle_year,vehicle_color,vehicle_vin,vehicle_license_plate,vehicle_license_state,vehicle_reported_stolen,vehicle_impounded," +
                "weapon_type,weapon_make,weapon_model,weapon_serial_number,weapon_length,weapon_reported_stolen) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?," +
                "?,?,?,?,?,?," +
                "?,?,?,?,?,?,?," +
                "?,?,?,?," +
                "?,?,?,?," +
                "?,?,?,?,?,?," +
                "?,?,?,?,?,?,?,?,?," +
                "?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            ps.setString(i++, e.getStatus() != null ? e.getStatus() : "Active");
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
        e.setCaseId(rs.getInt("case_id"));
        e.setItemMake(rs.getString("item_make")); e.setItemModel(rs.getString("item_model")); e.setItemSerial(rs.getString("item_serial"));
        int fcalId = rs.getInt("firearm_caliber_id"); if (!rs.wasNull()) { var cals = lookupRepo.getCaliberList(); cals.stream().filter(l->l.getId()==fcalId).findFirst().ifPresent(e::setFirearmCaliber); }
        int narcId = rs.getInt("narc_type_id"); if (!rs.wasNull()) { var types = lookupRepo.getNarcoticsTypes(); types.stream().filter(l->l.getId()==narcId).findFirst().ifPresent(e::setNarcType); }
        e.setNarcUnit(rs.getString("narc_unit")); double nq = rs.getDouble("narc_quantity"); if (!rs.wasNull()) e.setNarcQuantity(nq);
        e.setVehYear(rs.getString("veh_year")); e.setVehPlate(rs.getString("veh_plate")); e.setVehVin(rs.getString("veh_vin"));
        e.setWeaponType(rs.getString("weapon_type"));
        return e;
    }
}
