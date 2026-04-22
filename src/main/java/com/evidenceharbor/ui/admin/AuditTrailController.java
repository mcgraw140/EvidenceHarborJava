package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.AuditLog;
import com.evidenceharbor.persistence.AuditLogRepository;
import com.evidenceharbor.persistence.LookupRepository;
import com.evidenceharbor.util.TableExportUtil;
import com.evidenceharbor.util.Dialogs;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;


import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AuditTrailController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;

    @FXML private TextField   searchField;
    @FXML private ComboBox<String> moduleFilter;
    @FXML private ComboBox<String> actionFilter;
    @FXML private DatePicker  dateFrom;
    @FXML private DatePicker  dateTo;

    @FXML private ToggleButton tabAll, tabEvidence, tabCases, tabUsers, tabSystem;

    @FXML private TableView<AuditLog> logTable;
    @FXML private TableColumn<AuditLog, String> colTimestamp, colUser, colAction, colModule, colRecord, colDetails;

    private final AuditLogRepository repo = new AuditLogRepository();
    private final LookupRepository lookupRepo = new LookupRepository();
    private String activeModule = null;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            List<String> modules = new ArrayList<>();
            modules.add("All");
            modules.addAll(lookupRepo.getAuditModules());
            moduleFilter.setItems(FXCollections.observableArrayList(modules));
            actionFilter.setItems(FXCollections.observableArrayList(lookupRepo.getAuditActions()));
        } catch (Exception e) {
            showError(e);
            moduleFilter.setItems(FXCollections.observableArrayList("All"));
            actionFilter.setItems(FXCollections.observableArrayList());
        }

        colTimestamp.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTimestamp()));
        colUser.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getUserName() != null ? cd.getValue().getUserName() : "—"));
        colAction.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAction()));
        colModule.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getModule() != null ? cd.getValue().getModule() : "—"));
        colRecord.setCellValueFactory(cd -> {
            String entityId = cd.getValue().getEntityId();
            String type     = cd.getValue().getEntityType();
            String label = (type != null ? type : "") + (entityId != null ? " " + entityId : "");
            return new SimpleStringProperty(label.trim().isEmpty() ? "—" : label.trim());
        });
        colDetails.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDetails() != null ? cd.getValue().getDetails() : ""));

        // Row click -> detail dialog
        logTable.setRowFactory(tv -> {
            TableRow<AuditLog> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) showDetail(row.getItem());
            });
            return row;
        });

        loadLogs();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, null);
    }

    @FXML private void onSearch()       { loadLogs(); }
    @FXML private void onClearFilters() {
        searchField.clear(); moduleFilter.setValue(null); actionFilter.setValue(null);
        dateFrom.setValue(null); dateTo.setValue(null); activeModule = null;
        clearTabSelection(); tabAll.setSelected(true);
        loadLogs();
    }

    @FXML private void onTabAll()      { activeModule = null;          clearTabSelection(); tabAll.setSelected(true);      loadLogs(); }
    @FXML private void onTabEvidence() { activeModule = "Evidence";    clearTabSelection(); tabEvidence.setSelected(true); loadLogs(); }
    @FXML private void onTabCases()    { activeModule = "Cases";       clearTabSelection(); tabCases.setSelected(true);    loadLogs(); }
    @FXML private void onTabUsers()    { activeModule = "Users";       clearTabSelection(); tabUsers.setSelected(true);    loadLogs(); }
    @FXML private void onTabSystem()   { activeModule = "System";      clearTabSelection(); tabSystem.setSelected(true);   loadLogs(); }

    private void clearTabSelection() {
        for (ToggleButton t : new ToggleButton[]{tabAll, tabEvidence, tabCases, tabUsers, tabSystem})
            t.setSelected(false);
    }

    private void loadLogs() {
        try {
            String q = searchField.getText();
            String mod = activeModule != null ? activeModule :
                    (moduleFilter.getValue() != null && !"All".equals(moduleFilter.getValue()) ? moduleFilter.getValue() : null);
            String act = actionFilter.getValue();
            String from = dateFrom.getValue() != null ? dateFrom.getValue().format(FMT) : null;
            String to   = dateTo.getValue()   != null ? dateTo.getValue().format(FMT)   : null;
            List<AuditLog> logs = repo.search(q, mod, act, from, to);
            logTable.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void onExportCsv() {
        try {
            List<AuditLog> logs = logTable.getItems();
            StringBuilder sb = new StringBuilder();
            sb.append("Timestamp,User,Action,Module,Record,Details\n");
            for (AuditLog l : logs) {
                sb.append(csvEscape(l.getTimestamp())).append(",")
                  .append(csvEscape(l.getUserName())).append(",")
                  .append(csvEscape(l.getAction())).append(",")
                  .append(csvEscape(l.getModule())).append(",")
                  .append(csvEscape(l.getEntityType() != null ?
                          l.getEntityType() + " " + l.getEntityId() : "")).append(",")
                  .append(csvEscape(l.getDetails())).append("\n");
            }
            Path out = Path.of(System.getProperty("user.home"), "EvidenceHarbor",
                    "audit_trail_" + LocalDate.now() + ".csv");
            Files.createDirectories(out.getParent());
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
            Dialogs.info("Export complete", "Exported to:\n" + out);
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void onPrint() {
        javafx.stage.Window w = logTable.getScene() != null ? logTable.getScene().getWindow() : null;
        TableExportUtil.printTable(w, "Audit Trail", logTable);
    }

    private void showDetail(AuditLog log) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Audit Log Detail #" + log.getId());
        dlg.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.getChildren().addAll(
                detailRow("Timestamp:", log.getTimestamp()),
                detailRow("User:",      log.getUserName()),
                detailRow("Action:",    log.getAction()),
                detailRow("Module:",    log.getModule()),
                detailRow("Entity Type:", log.getEntityType()),
                detailRow("Entity ID:", log.getEntityId())
        );
        if (log.getDetails() != null && !log.getDetails().isBlank()) {
            Label lbl = new Label("Details:");
            lbl.setStyle("-fx-text-fill:#aaa; -fx-font-size:12;");
            TextArea ta = new TextArea(log.getDetails());
            ta.setEditable(false); ta.setWrapText(true); ta.setPrefHeight(160);
            box.getChildren().addAll(lbl, ta);
        }
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true); sp.setPrefSize(560, 420);
        dlg.getDialogPane().setContent(sp);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private javafx.scene.layout.HBox detailRow(String label, String value) {
        Label lbl = new Label(label);
        lbl.setMinWidth(120);
        lbl.setStyle("-fx-text-fill:#aaa; -fx-font-size:12;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-text-fill:white; -fx-font-size:12;");
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12, lbl, val);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // ── Nav ───────────────────────────────────────────────────────────────────
    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onBack()           { Navigator.get().showAdminDashboard(); }
    @FXML private void onDashboard()      { Navigator.get().showAdminDashboard(); }
    @FXML private void onAuditTrail()     { }
    @FXML private void onUserManagement()       { Navigator.get().showUserManagement(); }
    @FXML private void onLookupAdministration() { Navigator.get().showLookupAdmin(); }
    @FXML private void onEvidenceAudit()         { Navigator.get().showEvidenceAudit(); }
    @FXML private void onBankAccountLedger()     { Navigator.get().showBankAccountLedger(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    private void showError(Exception e) {
        e.printStackTrace();
        Dialogs.error(e);
    }
}
