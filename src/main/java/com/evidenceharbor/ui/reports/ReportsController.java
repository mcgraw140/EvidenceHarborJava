package com.evidenceharbor.ui.reports;

import com.evidenceharbor.app.Navigator;
import javafx.fxml.FXML;

public class ReportsController {

    @FXML private void onCases() { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople() { Navigator.get().showPeople(); }
    @FXML private void onDropbox() { Navigator.get().showDropbox(); }
    @FXML private void onReports() { }
    @FXML private void onBack() { Navigator.get().showCaseList(); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }
}
