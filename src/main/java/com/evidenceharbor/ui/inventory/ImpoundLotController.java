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
        List<Evidence> filtered = q.isEmpty() ? new ArrayList<>(allVehicles)
                : allVehicles.stream()
                    .filter(e -> ns(e.getBarcode()).toLowerCase().contains(q)
                              || ns(e.getVehicleMake()).toLowerCase().contains(q)
                              || ns(e.getVehicleModel()).toLowerCase().contains(q)
                              || ns(e.getVehicleVin()).toLowerCase().contains(q)
                              || ns(e.getVehicleLicensePlate()).toLowerCase().contains(q)
                              || caseNumberById.getOrDefault(e.getCaseId(), "").toLowerCase().contains(q))
                    .collect(Collectors.toList());
        vehicleTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private String ns(String s) { return s == null ? "" : s; }

    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onQuartermaster() { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }    @FXML private void onBack()          { Navigator.get().showQmDashboard(); }
    @FXML private void onDashboard()     { Navigator.get().showCaseList(); }
}
