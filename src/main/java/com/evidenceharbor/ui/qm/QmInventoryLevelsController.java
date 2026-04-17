package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.persistence.QmRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class QmInventoryLevelsController implements Initializable {

    @FXML private TableView<String[]> summaryTable;
    @FXML private TableColumn<String[], String> colSummaryCategory;
    @FXML private TableColumn<String[], String> colSummaryStatus;
    @FXML private TableColumn<String[], String> colSummaryCount;

    private final QmRepository repo = new QmRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colSummaryCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colSummaryStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colSummaryCount.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        load();
    }

    @FXML
    private void onRefresh() { load(); }

    private void load() {
        List<String[]> summary = repo.getInventorySummary();
        ObservableList<String[]> data = FXCollections.observableArrayList(summary);
        summaryTable.setItems(data);
    }

    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onQuartermaster() { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }

    @FXML private void onAssignEquipment() { Navigator.get().showQmAssignEquipment(); }
    @FXML private void onAmmunition()      { Navigator.get().showQmAmmunition(); }
    @FXML private void onInventoryLevels() { Navigator.get().showQmInventoryLevels(); }
    @FXML private void onInventoryAudit()  { Navigator.get().showQmInventoryAudit(); }
    @FXML private void onOfficerLoadouts() { Navigator.get().showQmOfficerLoadouts(); }

    @FXML private void onEquipment() { showInfo("Equipment workspace is not configured yet."); }
    @FXML private void onWeapons()   { showInfo("Weapons workspace is not configured yet."); }
    @FXML private void onUniforms()  { showInfo("Uniforms workspace is not configured yet."); }

    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
