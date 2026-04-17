package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import javafx.fxml.FXML;

public class QmDashboardController {

    @FXML private void onAssignEquipment()  { Navigator.get().showQmAssignEquipment(); }
    @FXML private void onAmmunition()       { Navigator.get().showQmAmmunition(); }
    @FXML private void onInventoryLevels()  { Navigator.get().showQmInventoryLevels(); }
    @FXML private void onOfficerLoadouts()  { Navigator.get().showQmOfficerLoadouts(); }
    @FXML private void onVehicleImpound()   { Navigator.get().showQmVehicleImpound(); }
    @FXML private void onInventoryAudit()   { Navigator.get().showQmInventoryAudit(); }

    @FXML private void onCases()          { Navigator.get().showCaseList(); }
    @FXML private void onInventory()      { Navigator.get().showInventory(); }
    @FXML private void onPeople()         { Navigator.get().showPeople(); }
    @FXML private void onDropbox()        { Navigator.get().showDropbox(); }
    @FXML private void onReports()        { Navigator.get().showReports(); }
    @FXML private void onSettings()       { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onQuartermaster()  { /* already here */ }
    @FXML private void onImpound()        { Navigator.get().showImpoundLot(); }

    @FXML private void onEquipment() {
        onComingSoon("Equipment");
    }

    @FXML private void onWeapons() {
        onComingSoon("Weapons");
    }

    @FXML private void onUniforms() {
        onComingSoon("Uniforms");
    }

    private void onComingSoon(String feature) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                feature + " workspace is not configured yet.");
        a.showAndWait();
    }
}
