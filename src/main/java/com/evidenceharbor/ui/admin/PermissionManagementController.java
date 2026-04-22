package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.OfficerRepository;
import com.evidenceharbor.util.Dialogs;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;

public class PermissionManagementController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;

    @FXML private Label pageTitle;
    @FXML private VBox  permissionsContainer;

    private Officer officer;
    private final OfficerRepository repo = new OfficerRepository();

    // â”€â”€ Permission flag definitions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    record PermFlag(String flag, String label, String description) {}

    private static final List<PermFlag> ALL_FLAGS = List.of(
        new PermFlag("can_create_evidence",        "Create Evidence",          "Submit new evidence items"),
        new PermFlag("can_edit_own_evidence",       "Edit Own Evidence",        "Edit own evidence before it is submitted"),
        new PermFlag("can_edit_all_evidence",       "Edit All Evidence",        "Edit any evidence record"),
        new PermFlag("can_view_all_evidence",       "View All Evidence",        "View evidence from all officers"),
        new PermFlag("can_change_status",           "Change Status",            "Change evidence status (e.g. Booked, Released)"),
        new PermFlag("can_change_location",         "Move Storage Locations",   "Move evidence between storage locations"),
        new PermFlag("can_manage_chain_of_custody", "Manage Chain of Custody",  "Add, edit, and transfer chain of custody entries"),
        new PermFlag("can_upload_files",            "Upload Files / Photos",    "Attach files and photos to evidence"),
        new PermFlag("can_delete_evidence",         "Delete Evidence",          "Soft-delete evidence records (no role gets this by default)"),
        new PermFlag("can_manage_users",            "Manage Users",             "Create, edit, and manage user accounts and permissions"),
        new PermFlag("can_view_audit_logs",         "View Audit Logs",          "Access the audit trail and system logs"),
        new PermFlag("can_manage_settings",         "Manage Settings",          "Configure system settings, dropboxes, and lookup lists")
    );

    // â”€â”€ Role default presets â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public static final Set<String> OFFICER_DEFAULTS = Set.of(
        "can_create_evidence",
        "can_edit_own_evidence",
        "can_upload_files"
    );

    public static final Set<String> EVIDENCE_TECH_DEFAULTS = Set.of(
        "can_create_evidence",
        "can_edit_own_evidence",
        "can_edit_all_evidence",
        "can_view_all_evidence",
        "can_change_status",
        "can_change_location",
        "can_manage_chain_of_custody",
        "can_upload_files",
        "can_view_audit_logs"
    );

    public static final Set<String> ADMIN_DEFAULTS = Set.of(
        "can_create_evidence",
        "can_edit_own_evidence",
        "can_edit_all_evidence",
        "can_view_all_evidence",
        "can_change_status",
        "can_change_location",
        "can_manage_chain_of_custody",
        "can_upload_files",
        "can_view_audit_logs",
        "can_manage_users",
        "can_manage_settings"
    );

    /** Returns the role default set for the given role string. */
    public static Set<String> defaultsForRole(String role) {
        if (role == null) return OFFICER_DEFAULTS;
        return switch (role.toLowerCase()) {
            case "evidence_tech" -> EVIDENCE_TECH_DEFAULTS;
            case "admin"         -> ADMIN_DEFAULTS;
            default              -> OFFICER_DEFAULTS;
        };
    }

    /**
     * Computes the effective permission set for an officer:
     *   Base   = role defaults
     *   +flag  â†’ added over role default
     *   -flag  â†’ removed from role default
     */
    public static Set<String> computeEffective(Officer officer) {
        Set<String> effective = new HashSet<>(defaultsForRole(officer.getRole()));
        String raw = officer.getPermissions();
        if (raw != null && !raw.isBlank()) {
            for (String token : raw.split(",")) {
                String t = token.trim();
                if (t.startsWith("+"))      effective.add(t.substring(1));
                else if (t.startsWith("-")) effective.remove(t.substring(1));
            }
        }
        return effective;
    }

    /** Map from flag -> CheckBox */
    private final Map<String, CheckBox> checkboxMap = new LinkedHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildUI();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, null);
    }

    public void setOfficer(Officer officer) {
        this.officer = officer;
        pageTitle.setText("Permissions â€” " + officer.getName());
        Set<String> effective = computeEffective(officer);
        checkboxMap.forEach((flag, cb) -> cb.setSelected(effective.contains(flag)));
    }

    private void buildUI() {
        checkboxMap.clear();
        permissionsContainer.getChildren().clear();

        for (PermFlag pf : ALL_FLAGS) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.setStyle("-fx-background-color:#1e293b; -fx-background-radius:6; -fx-border-color:#2d3748; -fx-border-radius:6;");

            CheckBox cb = new CheckBox();
            cb.setStyle("-fx-scale-x:1.2; -fx-scale-y:1.2;");
            checkboxMap.put(pf.flag(), cb);

            VBox labels = new VBox(2);
            Label name = new Label(pf.label());
            name.setStyle("-fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:13;");
            Label desc = new Label(pf.description());
            desc.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:11;");
            Label pill = new Label(pf.flag());
            pill.setStyle("-fx-background-color:#0f172a; -fx-text-fill:#475569; -fx-font-size:10; " +
                    "-fx-padding:2 6 2 6; -fx-background-radius:4; -fx-font-family:monospace;");
            labels.getChildren().addAll(name, desc, pill);

            row.getChildren().addAll(cb, labels);
            permissionsContainer.getChildren().add(row);
        }
    }

    // â”€â”€ Preset buttons â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private void onOfficerPreset()      { applyPreset(OFFICER_DEFAULTS); }
    @FXML private void onEvidenceTechPreset() { applyPreset(EVIDENCE_TECH_DEFAULTS); }
    @FXML private void onAdminPreset()        { applyPreset(ADMIN_DEFAULTS); }
    @FXML private void onSelectAll()          { checkboxMap.values().forEach(cb -> cb.setSelected(true)); }
    @FXML private void onClearAll()           { checkboxMap.values().forEach(cb -> cb.setSelected(false)); }

    private void applyPreset(Set<String> preset) {
        checkboxMap.forEach((flag, cb) -> cb.setSelected(preset.contains(flag)));
    }

    // â”€â”€ Save â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML
    private void onSave() {
        if (officer == null) return;
        Set<String> roleDefaults = defaultsForRole(officer.getRole());
        List<String> overrides = new ArrayList<>();
        checkboxMap.forEach((flag, cb) -> {
            boolean checked   = cb.isSelected();
            boolean inDefault = roleDefaults.contains(flag);
            if (checked && !inDefault)  overrides.add("+" + flag);
            if (!checked && inDefault)  overrides.add("-" + flag);
        });
        try {
            String stored = String.join(",", overrides);
            repo.savePermissions(officer.getId(), stored);
            officer.setPermissions(stored);
            Dialogs.info("Permissions saved", "Permissions saved for " + officer.getName());
        } catch (Exception e) {
            Dialogs.error(e);
        }
    }

    // â”€â”€ Nav â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onBack()          { Navigator.get().showUserManagement(); }
    @FXML private void onDashboard()     { Navigator.get().showAdminDashboard(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onAuditTrail()    { Navigator.get().showAuditTrail(); }
    @FXML private void onUserManagement()       { Navigator.get().showUserManagement(); }
    @FXML private void onLookupAdministration() { Navigator.get().showLookupAdmin(); }
    @FXML private void onEvidenceAudit()         { Navigator.get().showEvidenceAudit(); }
    @FXML private void onBankAccountLedger()     { Navigator.get().showBankAccountLedger(); }
    @FXML private void onReports()       { Navigator.get().showReports(); }
}
