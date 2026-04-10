package com.evidenceharbor.ui.dropbox;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.persistence.EvidenceRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DropboxController implements Initializable {

    @FXML private Label dropboxCountLabel;

    private final EvidenceRepository evidenceRepo = new EvidenceRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshCount();
    }

    private void refreshCount() {
        try {
            List<Evidence> all = evidenceRepo.findAll();
            long count = all.stream()
                    .filter(e -> e.getStorageLocation() != null)
                    .filter(e -> e.getStorageLocation().equalsIgnoreCase("Dropbox"))
                    .count();
            dropboxCountLabel.setText(String.valueOf(count));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML private void onCases() { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople() { Navigator.get().showPeople(); }
    @FXML private void onDropbox() { }
    @FXML private void onReports() { Navigator.get().showReports(); }
    @FXML private void onBack() { Navigator.get().showCaseList(); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }
}
