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
        Navigator.get().showQmEquipment();
    }

    @FXML private void onWeapons() {
        Navigator.get().showQmWeapons();
    }

    @FXML private void onUniforms() {
        Navigator.get().showQmUniforms();
    }
}
