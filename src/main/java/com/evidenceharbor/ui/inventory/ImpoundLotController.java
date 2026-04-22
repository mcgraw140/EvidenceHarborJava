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
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ImpoundLotController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;

    @FXML private TextField searchField;
    @FXML private Label statTotal;
    @FXML private Label statImpounded;
    @FXML private Label statStolen;
    @FXML private Label recordCountLabel;
    @FXML private Button btnViewAll;
    @FXML private Button btnViewInCustody;
    @FXML private Button btnViewHistorical;

    @FXML private TableView<Evidence> vehicleTable;
    @FXML private TableColumn<Evidence, String> colBarcode;
    @FXML private TableColumn<Evidence, String> colCase;
    @FXML private TableColumn<Evidence, String> colMakeModel;
    @FXML private TableColumn<Evidence, String> colYear;
    @FXML private TableColumn<Evidence, String> colColor;
    @FXML private TableColumn<Evidence, String> colVin;
    @FXML private TableColumn<Evidence, String> colPlate;
    @FXML private TableColumn<Evidence, String> colStatus;
    @FXML private TableColumn<Evidence, String> colImpounded;

    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final CaseRepository caseRepo = new CaseRepository();
    private final Map<Integer, String> caseNumberById = new HashMap<>();
    private List<Evidence> allVehicles = List.of();

    private static final Set<String> HISTORICAL_STATUSES = Set.of("Destroyed", "Disbursed", "Returned to Owner");
    private String viewMode = "all"; // all, inCustody, historical

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colBarcode.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getBarcode())));
        colCase.setCellValueFactory(cd -> new SimpleStringProperty(
                caseNumberById.getOrDefault(cd.getValue().getCaseId(), "")));
        colMakeModel.setCellValueFactory(cd -> new SimpleStringProperty(
                ns(cd.getValue().getVehicleMake()) + " " + ns(cd.getValue().getVehicleModel())));
        colYear.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getVehicleYear())));
        colColor.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getVehicleColor())));
        colVin.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getVehicleVin())));
        colPlate.setCellValueFactory(cd -> new SimpleStringProperty(
                ns(cd.getValue().getVehicleLicensePlate()) + " " + ns(cd.getValue().getVehicleLicenseState())));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getStatus())));
        colImpounded.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().isVehicleImpounded() ? "Yes" : "No"));

        searchField.textProperty().addListener((obs, o, n) -> applySearch(n));
        vehicleTable.setRowFactory(tv -> {
            TableRow<Evidence> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && e.getClickCount() == 2 && !row.isEmpty()) {
                    if (VehicleDetailsDialog.show(row.getItem())) loadData();
                }
            });
            return row;
        });
        loadData();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, null);
    }

    private void loadData() {
        try {
            caseNumberById.clear();
            for (Case c : caseRepo.findAll()) caseNumberById.put(c.getId(), c.getCaseNumber());
            allVehicles = evidenceRepo.findAll().stream()
                    .filter(e -> "Vehicle".equalsIgnoreCase(e.getEvidenceType()))
                    .collect(Collectors.toList());
            updateStats();
            applySearch(searchField.getText());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allVehicles.size()));
        statImpounded.setText(String.valueOf(allVehicles.stream().filter(Evidence::isVehicleImpounded).count()));
        statStolen.setText(String.valueOf(allVehicles.stream().filter(Evidence::isVehicleReportedStolen).count()));
    }

    private void applySearch(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Evidence> filtered = allVehicles.stream()
                .filter(this::matchesViewMode)
                .filter(e -> q.isEmpty()
                        || ns(e.getBarcode()).toLowerCase().contains(q)
                        || ns(e.getVehicleMake()).toLowerCase().contains(q)
                        || ns(e.getVehicleModel()).toLowerCase().contains(q)
                        || ns(e.getVehicleVin()).toLowerCase().contains(q)
                        || ns(e.getVehicleLicensePlate()).toLowerCase().contains(q)
                        || caseNumberById.getOrDefault(e.getCaseId(), "").toLowerCase().contains(q))
                .collect(Collectors.toList());
        vehicleTable.setItems(FXCollections.observableArrayList(filtered));
        if (recordCountLabel != null) {
            recordCountLabel.setText(filtered.size() + " vehicle" + (filtered.size() == 1 ? "" : "s"));
        }
    }

    private boolean matchesViewMode(Evidence e) {
        String status = ns(e.getStatus());
        return switch (viewMode) {
            case "inCustody"  -> e.isVehicleImpounded() && !HISTORICAL_STATUSES.contains(status);
            case "historical" -> HISTORICAL_STATUSES.contains(status);
            default -> true; // "all"
        };
    }

    private String ns(String s) { return s == null ? "" : s; }

    @FXML private void onPrint() {
        javafx.stage.Window w = vehicleTable.getScene() != null ? vehicleTable.getScene().getWindow() : null;
        String label = switch (viewMode) {
            case "inCustody"  -> "In Custody";
            case "historical" -> "Historical";
            default -> "All";
        };
        com.evidenceharbor.util.PrintSheetUtil.printTable(w, "Vehicle Impound Lot (" + label + ")", vehicleTable);
    }

    @FXML private void onViewAll() {
        viewMode = "all";
        btnViewAll.getStyleClass().setAll("btn-primary");
        btnViewInCustody.getStyleClass().setAll("btn-secondary");
        btnViewHistorical.getStyleClass().setAll("btn-secondary");
        applySearch(searchField.getText());
    }

    @FXML private void onViewInCustody() {
        viewMode = "inCustody";
        btnViewAll.getStyleClass().setAll("btn-secondary");
        btnViewInCustody.getStyleClass().setAll("btn-primary");
        btnViewHistorical.getStyleClass().setAll("btn-secondary");
        applySearch(searchField.getText());
    }

    @FXML private void onViewHistorical() {
        viewMode = "historical";
        btnViewAll.getStyleClass().setAll("btn-secondary");
        btnViewInCustody.getStyleClass().setAll("btn-secondary");
        btnViewHistorical.getStyleClass().setAll("btn-primary");
        applySearch(searchField.getText());
    }

    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    @FXML private void onBack()          { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onDashboard()     { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
}
