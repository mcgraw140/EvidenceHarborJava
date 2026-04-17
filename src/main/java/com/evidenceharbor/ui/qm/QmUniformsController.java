package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class QmUniformsController implements Initializable {

    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbTypeFilter;
    @FXML private ComboBox<String> cbSizeFilter;
    @FXML private TableView<?> uniformsTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup will be added as needed
    }

    @FXML private void onAddUniform() {
        System.out.println("Add Uniform clicked");
    }

    // ──────────────────────── QM Navigation ────────────────────────
    @FXML private void onQuartermaster() { Navigator.get().showQmDashboard(); }
    @FXML private void onAssignEquipment() { Navigator.get().showQmAssignEquipment(); }
    @FXML private void onEquipment() { Navigator.get().showQmEquipment(); }
    @FXML private void onWeapons() { Navigator.get().showQmWeapons(); }
    @FXML private void onUniforms() { }
    @FXML private void onAmmunition() { Navigator.get().showQmAmmunition(); }
    @FXML private void onInventoryLevels() { Navigator.get().showQmInventoryLevels(); }
    @FXML private void onInventoryAudit() { Navigator.get().showQmInventoryAudit(); }
    @FXML private void onOfficerLoadouts() { Navigator.get().showQmOfficerLoadouts(); }

    // ──────────────────────── Main Module Navigation ────────────────────────
    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onImpound()   { Navigator.get().showImpoundLot(); }
    @FXML private void onAdmin()     { Navigator.get().showAdminDashboard(); }
}
