package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.AgencySettings;

import java.sql.*;

public class SettingsRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public AgencySettings load() throws SQLException {
        AgencySettings s = new AgencySettings();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT k, v FROM agency_settings")) {
            while (rs.next()) {
                String key = rs.getString("k");
                String val = rs.getString("v");
                switch (key) {
                    case "agency_name"              -> s.setAgencyName(val);
                    case "agency_address"           -> s.setAgencyAddress(val);
                    case "agency_city"              -> s.setAgencyCity(val);
                    case "agency_state"             -> s.setAgencyState(val);
                    case "agency_zip"               -> s.setAgencyZip(val);
                    case "case_number_pattern"      -> s.setCaseNumberPattern(val);
                    case "case_number_example"      -> s.setCaseNumberExample(val);
                    case "evidence_number_pattern"  -> s.setEvidenceNumberPattern(val);
                    case "evidence_number_example"  -> s.setEvidenceNumberExample(val);
                    case "barcode_prefix"           -> s.setBarcodePrefix(val);
                }
            }
        }
        return s;
    }

    public void save(AgencySettings s) throws SQLException {
        set("agency_name",              s.getAgencyName());
        set("agency_address",           s.getAgencyAddress());
        set("agency_city",              s.getAgencyCity());
        set("agency_state",             s.getAgencyState());
        set("agency_zip",               s.getAgencyZip());
        set("case_number_pattern",      s.getCaseNumberPattern());
        set("case_number_example",      s.getCaseNumberExample());
        set("evidence_number_pattern",  s.getEvidenceNumberPattern());
        set("evidence_number_example",  s.getEvidenceNumberExample());
        set("barcode_prefix",           s.getBarcodePrefix());
        AuditLogger.log("System", "UPDATE", "Settings", "agency",
                "Agency settings saved: " + s.getAgencyName());
    }

    private void set(String key, String value) throws SQLException {
        int updated;
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE agency_settings SET v=? WHERE k=?")) {
            ps.setString(1, value == null ? "" : value);
            ps.setString(2, key);
            updated = ps.executeUpdate();
        }

        if (updated == 0) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO agency_settings (k, v) VALUES (?, ?)")) {
                ps.setString(1, key);
                ps.setString(2, value == null ? "" : value);
                ps.executeUpdate();
            }
        }
    }

    /** Convenience: get a single setting value, returns "" if not set. */
    public String get(String key) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT v FROM agency_settings WHERE k=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("v") : "";
            }
        } catch (SQLException e) {
            return "";
        }
    }
}
