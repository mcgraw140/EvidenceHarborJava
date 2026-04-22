package com.evidenceharbor.ui.cases;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.app.SessionManager;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EvidenceDashboardController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navInventoryBtn;
    @FXML private Button navDropboxBtn;
    @FXML private Button navReportsBtn;
    @FXML private VBox cardInventory;
    @FXML private VBox cardDropbox;
    @FXML private VBox cardReports;

    @FXML private Label lblOfficerName;
    @FXML private Label statActiveCases;
    @FXML private Label statItemsInCustody;
    @FXML private Label statDropboxItems;
    @FXML private Label statTotalEvidence;

    private final CaseRepository caseRepo = new CaseRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NavHelper.applyNavVisibility(navAdminTab, null, null, navInventoryBtn, navReportsBtn, navDropboxBtn);

        // Hide nav cards for modules the user cannot access
        if (cardInventory != null && navInventoryBtn != null) {
            cardInventory.visibleProperty().bind(navInventoryBtn.visibleProperty());
            cardInventory.managedProperty().bind(navInventoryBtn.managedProperty());
        }
        if (cardDropbox != null && navDropboxBtn != null) {
            cardDropbox.visibleProperty().bind(navDropboxBtn.visibleProperty());
            cardDropbox.managedProperty().bind(navDropboxBtn.managedProperty());
        }
        if (cardReports != null && navReportsBtn != null) {
            cardReports.visibleProperty().bind(navReportsBtn.visibleProperty());
            cardReports.managedProperty().bind(navReportsBtn.managedProperty());
        }

        Officer o = SessionManager.getCurrentOfficer();
        if (lblOfficerName != null && o != null) {
            lblOfficerName.setText("Signed in: " + o.getName());
        }

        refreshStats();
    }

    private void refreshStats() {
        try {
            int totalCases = caseRepo.findAll().size();
            statActiveCases.setText(String.valueOf(totalCases));
        } catch (Exception ex) {
            statActiveCases.setText("—");
        }
        try {
            List<Evidence> all = evidenceRepo.findAll();
            statTotalEvidence.setText(String.valueOf(all.size()));
            long inCustody = all.stream()
                    .filter(e -> {
                        String s = e.getStatus();
                        return s != null && !s.equalsIgnoreCase("Released")
                                && !s.equalsIgnoreCase("Destroyed")
                                && !s.equalsIgnoreCase("Returned");
                    })
                    .count();
            statItemsInCustody.setText(String.valueOf(inCustody));
            long dropbox = all.stream()
                    .filter(e -> "Dropbox".equalsIgnoreCase(e.getStatus())
                            || "In Dropbox".equalsIgnoreCase(e.getStatus()))
                    .count();
            statDropboxItems.setText(String.valueOf(dropbox));
        } catch (Exception ex) {
            statTotalEvidence.setText("—");
            statItemsInCustody.setText("—");
            statDropboxItems.setText("—");
        }
    }

    @FXML private void onEvidenceDashboard() { /* already here */ }
    @FXML private void onNewCase()           { Navigator.get().showCaseListAndCreate(); }
    @FXML private void onCases()             { Navigator.get().showCaseList(); }
    @FXML private void onInventory()         { Navigator.get().showInventory(); }
    @FXML private void onDropbox()           { Navigator.get().showDropbox(); }
    @FXML private void onReports()           { Navigator.get().showReports(); }
    @FXML private void onPeople()            { Navigator.get().showPeople(); }
    @FXML private void onImpound()           { Navigator.get().showImpoundLot(); }
    @FXML private void onAdmin()             { Navigator.get().showAdminDashboard(); }
}
