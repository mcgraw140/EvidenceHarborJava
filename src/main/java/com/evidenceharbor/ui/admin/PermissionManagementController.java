package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.OfficerRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class PermissionManagementController implements Initializable {

    @FXML private Label   pageTitle;
    @FXML private VBox    permissionsContainer;

    private Officer officer;
    private final OfficerRepository repo = new OfficerRepository();

    /** Permission catalog — group -> resource -> actions */
    private static final LinkedHashMap<String, LinkedHashMap<String, String[]>> CATALOG = new LinkedHashMap<>();
    static {
        LinkedHashMap<String, String[]> evidence = new LinkedHashMap<>();
        evidence.put("cases",           new String[]{"view","create","edit","delete"});
        evidence.put("evidence",        new String[]{"view","create","edit","delete"});
        evidence.put("custody",         new String[]{"view","create","edit","transfer"});
        evidence.put("dropbox",         new String[]{"view","create","edit","process"});
        evidence.put("reports",         new String[]{"view","create","edit"});
        CATALOG.put("Evidence Module", evidence);

        LinkedHashMap<String, String[]> admin = new LinkedHashMap<>();
        admin.put("admin_dashboard",    new String[]{"view"});
        admin.put("users",              new String[]{"view","create","edit","delete"});
        admin.put("people",             new String[]{"view","create","edit","delete"});
        admin.put("permissions",        new String[]{"view","create","edit"});
        admin.put("audit_evidence",     new String[]{"view","create","edit","perform"});
        admin.put("bank_accounts",      new String[]{"view","create","edit"});
        admin.put("audit_trail",        new String[]{"view"});
        admin.put("lookups",            new String[]{"view","create","edit"});
        admin.put("custom_fields",      new String[]{"view","create","edit"});
        admin.put("settings",           new String[]{"view","create","edit"});
        CATALOG.put("Administration", admin);

        LinkedHashMap<String, String[]> narc = new LinkedHashMap<>();
        narc.put("narcotics",           new String[]{"view","create","edit"});
        narc.put("narcotics_buys",      new String[]{"view","create","edit"});
        CATALOG.put("Narcotics", narc);

        LinkedHashMap<String, String[]> qm = new LinkedHashMap<>();
        qm.put("qm_dashboard",          new String[]{"view"});
        qm.put("qm_equipment",          new String[]{"view","create","edit"});
        qm.put("qm_weapons",            new String[]{"view","create","edit"});
        qm.put("qm_uniforms",           new String[]{"view","create","edit"});
        CATALOG.put("Quartermaster", qm);
    }

    private static final List<String> OFFICER_PRESET = List.of(
            "cases:view","cases:create","cases:edit",
            "evidence:view","evidence:create","evidence:edit",
            "custody:view","custody:create","custody:edit","custody:transfer",
            "dropbox:view","dropbox:create","dropbox:edit","dropbox:process",
            "reports:view",
            "people:view","people:create","people:edit",
            "narcotics:view","narcotics:create","narcotics:edit",
            "narcotics_buys:view","narcotics_buys:create","narcotics_buys:edit",
            "qm_dashboard:view","qm_equipment:view","qm_weapons:view","qm_uniforms:view"
    );

    /** Map from permission key -> CheckBox */
    private final Map<String, CheckBox> checkboxMap = new LinkedHashMap<>();

    public void setOfficer(Officer officer) {
        this.officer = officer;
        pageTitle.setText("Permissions — " + officer.getName());
        // Parse stored permissions
        Set<String> stored = parsePermissions(officer.getPermissions());
        checkboxMap.forEach((key, cb) -> cb.setSelected(stored.contains(key)));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildPermissionUI();
    }

    private void buildPermissionUI() {
        checkboxMap.clear();
        permissionsContainer.getChildren().clear();

        CATALOG.forEach((groupName, resources2) -> {
            // Group header
            TitledPane pane = new TitledPane();
            pane.setText(groupName);
            pane.setStyle("-fx-text-fill: white;");

            VBox groupBox = new VBox(8);
            groupBox.setPadding(new Insets(8, 0, 8, 16));

            resources2.forEach((resource, actions) -> {
                VBox resourceBox = new VBox(4);
                resourceBox.setPadding(new Insets(4, 0, 4, 0));

                // Resource header + "Select all for resource" checkbox
                HBox headerRow = new HBox(12);
                headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label resourceLabel = new Label(resource.replace("_", " ").toUpperCase());
                resourceLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#ccc; -fx-font-size:12;");
                CheckBox selectAllCb = new CheckBox("All");
                selectAllCb.setStyle("-fx-text-fill:#888; -fx-font-size:11;");
                headerRow.getChildren().addAll(resourceLabel, selectAllCb);
                resourceBox.getChildren().add(headerRow);

                // Action checkboxes
                HBox actionsRow = new HBox(16);
                actionsRow.setPadding(new Insets(0, 0, 0, 8));
                List<CheckBox> rowCbs = new ArrayList<>();
                for (String action : actions) {
                    String key = resource + ":" + action;
                    CheckBox cb = new CheckBox(action);
                    cb.setStyle("-fx-text-fill: white;");
                    checkboxMap.put(key, cb);
                    actionsRow.getChildren().add(cb);
                    rowCbs.add(cb);
                }
                resourceBox.getChildren().add(actionsRow);

                // Wire select-all checkbox
                selectAllCb.setOnAction(e -> rowCbs.forEach(cb -> cb.setSelected(selectAllCb.isSelected())));
                rowCbs.forEach(cb -> cb.setOnAction(e ->
                        selectAllCb.setSelected(rowCbs.stream().allMatch(CheckBox::isSelected))));

                groupBox.getChildren().add(resourceBox);
                groupBox.getChildren().add(new Separator());
            });

            pane.setContent(groupBox);
            permissionsContainer.getChildren().add(pane);
        });
    }

    @FXML private void onOfficerPreset() {
        checkboxMap.forEach((key, cb) -> cb.setSelected(OFFICER_PRESET.contains(key)));
    }

    @FXML private void onSelectAll() {
        checkboxMap.values().forEach(cb -> cb.setSelected(true));
    }

    @FXML private void onClearAll() {
        checkboxMap.values().forEach(cb -> cb.setSelected(false));
    }

    @FXML
    private void onSave() {
        if (officer == null) return;
        List<String> perms = checkboxMap.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        // Store as comma-separated string in the role field for now
        // (a dedicated permissions column can be added in a future migration)
        try {
            repo.savePermissions(officer.getId(), String.join(",", perms));
            new Alert(Alert.AlertType.INFORMATION, "Permissions saved for " + officer.getName()).showAndWait();
        } catch (Exception e) { showError(e); }
    }

    private Set<String> parsePermissions(String permissionsField) {
        if (permissionsField == null || permissionsField.isBlank()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(permissionsField.split(",")));
    }

    // ── Nav ───────────────────────────────────────────────────────────────────
    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onQuartermaster()  { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    private void showError(Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
    }
}
