package com.evidenceharbor.ui.inventory;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Case;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryController implements Initializable {

    private static final Set<String> HISTORICAL_STATUSES = Set.of("Destroyed", "Disbursed", "Returned to Owner");

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navDropboxBtn;

    @FXML private TextField searchField;
    @FXML private TableView<Evidence> inventoryTable;
    @FXML private TableColumn<Evidence, String> colScan;
    @FXML private TableColumn<Evidence, String> colBarcode;
    @FXML private TableColumn<Evidence, String> colCase;
    @FXML private TableColumn<Evidence, String> colDescription;
    @FXML private TableColumn<Evidence, String> colType;
    @FXML private TableColumn<Evidence, String> colStatus;
    @FXML private TableColumn<Evidence, String> colLocation;
    @FXML private Button btnViewCurrent;
    @FXML private Button btnViewAll;
    @FXML private Button btnViewHistorical;
    @FXML private Label recordCountLabel;

    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final CaseRepository caseRepo = new CaseRepository();
    private final Map<Integer, String> caseNumberById = new HashMap<>();
    private List<Evidence> allEvidence = List.of();
    private String viewMode = "current"; // current, all, historical

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colScan.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getScanCode())));
        colBarcode.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getBarcode())));
        colCase.setCellValueFactory(cd -> new SimpleStringProperty(caseNumberById.getOrDefault(cd.getValue().getCaseId(), String.valueOf(cd.getValue().getCaseId()))));
        colDescription.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getDescription())));
        colType.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getEvidenceType())));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getStatus())));
        colLocation.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getStorageLocation())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                String color = statusColor(status);
                setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; " +
                         "-fx-font-weight: bold; -fx-background-radius: 4;");
            }
        });

        inventoryTable.setRowFactory(tv -> {
            TableRow<Evidence> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openDetailDialog(row.getItem());
                }
            });
            return row;
        });

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        loadData();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);
    }

    private String statusColor(String status) {
        if (status == null) return "#888888";
        return switch (status) {
            case "In Dropbox"        -> "#f59e0b";
            case "In Custody"        -> "#22c55e";
            case "In Storage"        -> "#14b8a6";
            case "Checked In"        -> "#22c55e";
            case "Checked Out"       -> "#3b82f6";
            case "Deposited"         -> "#6366f1";
            case "Destroyed"         -> "#ef4444";
            case "Disbursed"         -> "#6b7280";
            case "Returned to Owner" -> "#14b8a6";
            case "Missing"           -> "#dc2626";
            case "Pending"           -> "#94a3b8";
            default                  -> "#64748b";
        };
    }

    private void loadData() {
        try {
            caseNumberById.clear();
            for (Case c : caseRepo.findAll()) {
                caseNumberById.put(c.getId(), c.getCaseNumber());
            }
            allEvidence = evidenceRepo.findAll();
            applyFilter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<Evidence> filtered = allEvidence.stream()
                .filter(e -> matchesViewMode(e))
                .filter(e -> q.isEmpty() || matchesSearch(e, q))
                .collect(Collectors.toList());
        inventoryTable.setItems(FXCollections.observableArrayList(filtered));
        recordCountLabel.setText(filtered.size() + " records");
    }

    private boolean matchesViewMode(Evidence e) {
        String status = nullSafe(e.getStatus());
        return switch (viewMode) {
            case "current"    -> !HISTORICAL_STATUSES.contains(status);
            case "historical" -> HISTORICAL_STATUSES.contains(status);
            default           -> true;
        };
    }

    private boolean matchesSearch(Evidence e, String q) {
        return nullSafe(e.getScanCode()).toLowerCase().contains(q)
            || nullSafe(e.getBarcode()).toLowerCase().contains(q)
                || nullSafe(e.getDescription()).toLowerCase().contains(q)
                || nullSafe(e.getEvidenceType()).toLowerCase().contains(q)
                || nullSafe(e.getStorageLocation()).toLowerCase().contains(q)
                || nullSafe(e.getStatus()).toLowerCase().contains(q)
                || caseNumberById.getOrDefault(e.getCaseId(), "").toLowerCase().contains(q);
    }

    private void openDetailDialog(Evidence evidence) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EvidenceDetail.fxml"));
            Parent root = loader.load();
            EvidenceDetailController ctrl = loader.getController();
            ctrl.setEvidence(evidence, caseNumberById.getOrDefault(evidence.getCaseId(), ""));

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            Window owner = inventoryTable.getScene().getWindow();
            dialog.initOwner(owner);
            dialog.setTitle("Evidence Detail â€” " + evidence.getBarcode());
            Scene scene = new Scene(root, 860, 640);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            loadData(); // refresh after potential status changes
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML private void onViewCurrent() {
        viewMode = "current";
        btnViewCurrent.getStyleClass().setAll("btn-primary");
        btnViewAll.getStyleClass().setAll("btn-secondary");
        btnViewHistorical.getStyleClass().setAll("btn-secondary");
        applyFilter();
    }

    @FXML private void onViewAll() {
        viewMode = "all";
        btnViewCurrent.getStyleClass().setAll("btn-secondary");
        btnViewAll.getStyleClass().setAll("btn-primary");
        btnViewHistorical.getStyleClass().setAll("btn-secondary");
        applyFilter();
    }

    @FXML private void onViewHistorical() {
        viewMode = "historical";
        btnViewCurrent.getStyleClass().setAll("btn-secondary");
        btnViewAll.getStyleClass().setAll("btn-secondary");
        btnViewHistorical.getStyleClass().setAll("btn-primary");
        applyFilter();
    }

    private String nullSafe(String value) { return value == null ? "" : value; }

    @FXML private void onPrint() {
        String title = "Evidence Inventory (" + (viewMode == null ? "current" : viewMode) + ")";
        javafx.stage.Window w = inventoryTable.getScene() != null ? inventoryTable.getScene().getWindow() : null;
        com.evidenceharbor.util.PrintSheetUtil.printTable(w, title, inventoryTable);
    }

    @FXML private void onBatchTransfer() { Navigator.get().showBatchCocTransfer(); }

    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }    @FXML private void onBack()          { Navigator.get().showCaseList(); }
    @FXML private void onDashboard()     { Navigator.get().showCaseList(); }
}

