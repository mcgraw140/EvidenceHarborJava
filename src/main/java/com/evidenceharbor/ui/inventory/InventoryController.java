package com.evidenceharbor.ui.inventory;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Case;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class InventoryController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<Evidence> inventoryTable;
    @FXML private TableColumn<Evidence, String> colBarcode;
    @FXML private TableColumn<Evidence, String> colCase;
    @FXML private TableColumn<Evidence, String> colDescription;
    @FXML private TableColumn<Evidence, String> colType;
    @FXML private TableColumn<Evidence, String> colLocation;

    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final CaseRepository caseRepo = new CaseRepository();
    private final Map<Integer, String> caseNumberById = new HashMap<>();
    private List<Evidence> allEvidence = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colBarcode.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getBarcode())));
        colCase.setCellValueFactory(cd -> new SimpleStringProperty(caseNumberById.getOrDefault(cd.getValue().getCaseId(), String.valueOf(cd.getValue().getCaseId()))));
        colDescription.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getDescription())));
        colType.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getEvidenceType())));
        colLocation.setCellValueFactory(cd -> new SimpleStringProperty(nullSafe(cd.getValue().getStorageLocation())));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearch(newVal));
        loadData();
    }

    private void loadData() {
        try {
            for (Case c : caseRepo.findAll()) {
                caseNumberById.put(c.getId(), c.getCaseNumber());
            }
            allEvidence = evidenceRepo.findAll();
            inventoryTable.setItems(FXCollections.observableArrayList(allEvidence));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applySearch(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            inventoryTable.setItems(FXCollections.observableArrayList(allEvidence));
            return;
        }
        List<Evidence> filtered = allEvidence.stream()
                .filter(e -> nullSafe(e.getBarcode()).toLowerCase().contains(q)
                        || nullSafe(e.getDescription()).toLowerCase().contains(q)
                        || nullSafe(e.getEvidenceType()).toLowerCase().contains(q)
                        || nullSafe(e.getStorageLocation()).toLowerCase().contains(q)
                        || caseNumberById.getOrDefault(e.getCaseId(), "").toLowerCase().contains(q))
                .collect(Collectors.toList());
        inventoryTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private String nullSafe(String value) { return value == null ? "" : value; }

    @FXML private void onCases() { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { }
    @FXML private void onPeople() { Navigator.get().showPeople(); }
    @FXML private void onDropbox() { Navigator.get().showDropbox(); }
    @FXML private void onReports() { Navigator.get().showReports(); }
    @FXML private void onBack() { Navigator.get().showCaseList(); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }
}
