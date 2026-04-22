package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.EvidenceAudit;
import com.evidenceharbor.persistence.EvidenceAuditRepository;
import com.evidenceharbor.persistence.LookupRepository;
import com.evidenceharbor.util.Dialogs;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;


import java.net.URL;
import java.util.ResourceBundle;

public class EvidenceAuditController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;

    @FXML private TableView<EvidenceAudit> inProgressTable;
    @FXML private TableColumn<EvidenceAudit, String> colIPDate, colIPType, colIPScope, colIPBy, colIPStatus;
    @FXML private TableColumn<EvidenceAudit, String> colIPAction;

    @FXML private TableView<EvidenceAudit> completedTable;
    @FXML private TableColumn<EvidenceAudit, String> colCDate, colCType, colCScope, colCBy, colCStatus;
    @FXML private TableColumn<EvidenceAudit, String> colCAction;

    private final EvidenceAuditRepository repo = new EvidenceAuditRepository();
    private final LookupRepository lookupRepo = new LookupRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable(inProgressTable, colIPDate, colIPType, colIPScope, colIPBy, colIPStatus, colIPAction, false);
        setupTable(completedTable, colCDate, colCType, colCScope, colCBy, colCStatus, colCAction, true);
        loadData();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, null);
    }

    private void setupTable(TableView<EvidenceAudit> table,
                             TableColumn<EvidenceAudit, String> dateCol,
                             TableColumn<EvidenceAudit, String> typeCol,
                             TableColumn<EvidenceAudit, String> scopeCol,
                             TableColumn<EvidenceAudit, String> byCol,
                             TableColumn<EvidenceAudit, String> statusCol,
                             TableColumn<EvidenceAudit, String> actionCol,
                             boolean completed) {
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(
                completed ? cd.getValue().getCompletedAt() : cd.getValue().getCreatedAt()));
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAuditType()));
        scopeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getScope() != null ? cd.getValue().getScope() : "â€”"));
        byCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCreatedBy() != null ? cd.getValue().getCreatedBy() : "â€”"));
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label lbl = new Label(s);
                String color = "Completed".equals(s) ? "#27ae60" : "#e67e22";
                lbl.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; " +
                        "-fx-padding:3 8 3 8; -fx-background-radius:4; -fx-font-size:11;");
                setGraphic(lbl);
                setText(null);
            }
        });
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button(completed ? "View" : "Resume");
            {
                openBtn.setStyle("-fx-font-size:11; -fx-padding:2 8 2 8;");
                openBtn.setOnAction(e -> {
                    EvidenceAudit a = getTableView().getItems().get(getIndex());
                    Navigator.get().showEvidenceAuditSession(a);
                });
            }
            @Override protected void updateItem(String i, boolean empty) {
                super.updateItem(i, empty);
                setGraphic(empty ? null : openBtn);
            }
        });
    }

    @FXML
    private void onPrint() {
        java.util.List<com.evidenceharbor.util.PrintSheetUtil.Section> sections = new java.util.ArrayList<>();
        if (inProgressTable.getItems() != null && !inProgressTable.getItems().isEmpty()) {
            sections.add(new com.evidenceharbor.util.PrintSheetUtil.TableSection(
                    "In Progress",
                    com.evidenceharbor.util.PrintSheetUtil.tableHeaders(inProgressTable),
                    com.evidenceharbor.util.PrintSheetUtil.tableRows(inProgressTable)));
        }
        if (completedTable.getItems() != null && !completedTable.getItems().isEmpty()) {
            sections.add(new com.evidenceharbor.util.PrintSheetUtil.TableSection(
                    "Completed",
                    com.evidenceharbor.util.PrintSheetUtil.tableHeaders(completedTable),
                    com.evidenceharbor.util.PrintSheetUtil.tableRows(completedTable)));
        }
        if (sections.isEmpty()) {
            com.evidenceharbor.util.Dialogs.info("Nothing to print.");
            return;
        }
        javafx.stage.Window w = completedTable.getScene() != null ? completedTable.getScene().getWindow() : null;
        com.evidenceharbor.util.PrintSheetUtil.print(w, "Evidence Storage Audits", sections);
    }

    @FXML
    private void onNewAudit() {
        Dialog<EvidenceAudit> dlg = new Dialog<>();
        dlg.setTitle("New Evidence Audit");
        dlg.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());

        var grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));

        ComboBox<String> typeCombo = new ComboBox<>();
        try {
            typeCombo.setItems(FXCollections.observableArrayList(lookupRepo.getAuditTypes()));
        } catch (Exception e) {
            showError(e);
        }
        if (typeCombo.getItems().contains("Full")) {
            typeCombo.setValue("Full");
        } else if (!typeCombo.getItems().isEmpty()) {
            typeCombo.setValue(typeCombo.getItems().get(0));
        }
        TextField scopeField = new TextField();
        scopeField.setPromptText("e.g. 25% or Room A");
        TextField createdByField = new TextField();
        createdByField.setPromptText("Officer name");

        Label scopeLabel = new Label("Scope");

        typeCombo.valueProperty().addListener((obs, old, val) -> {
            boolean showScope = "Random".equals(val) || "Location".equals(val);
            scopeField.setDisable(!showScope);
            scopeLabel.setDisable(!showScope);
            if ("Full".equals(val)) scopeField.clear();
        });
        scopeField.setDisable(true); scopeLabel.setDisable(true);

        grid.add(new Label("Audit Type *"), 0, 0); grid.add(typeCombo, 1, 0);
        grid.add(scopeLabel, 0, 1);                grid.add(scopeField, 1, 1);
        grid.add(new Label("Created By"),  0, 2);  grid.add(createdByField, 1, 2);

        dlg.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType("Create Audit", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dlg.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try {
                String scope = scopeField.getText().trim();
                return repo.create(typeCombo.getValue(), scope.isBlank() ? "Full" : scope, createdByField.getText().trim());
            } catch (Exception e) { e.printStackTrace(); return null; }
        });

        dlg.showAndWait().ifPresent(a -> {
            if (a != null) loadData();
        });
    }

    private void loadData() {
        try {
            inProgressTable.setItems(FXCollections.observableArrayList(repo.findInProgress()));
            completedTable.setItems(FXCollections.observableArrayList(repo.findCompleted()));
        } catch (Exception e) { showError(e); }
    }

    // â”€â”€ Nav â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onBack()           { Navigator.get().showAdminDashboard(); }
    @FXML private void onDashboard()      { Navigator.get().showAdminDashboard(); }
    @FXML private void onAuditTrail()     { Navigator.get().showAuditTrail(); }
    @FXML private void onUserManagement()       { Navigator.get().showUserManagement(); }
    @FXML private void onLookupAdministration() { Navigator.get().showLookupAdmin(); }
    @FXML private void onEvidenceAudit()         { }
    @FXML private void onBankAccountLedger()     { Navigator.get().showBankAccountLedger(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    private void showError(Exception e) {
        e.printStackTrace();
        Dialogs.error(e);
    }
}
