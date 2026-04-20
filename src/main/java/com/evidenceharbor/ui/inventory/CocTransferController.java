package com.evidenceharbor.ui.inventory;

import com.evidenceharbor.app.CurrentOfficerResolver;
import com.evidenceharbor.domain.ChainOfCustody;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.ChainOfCustodyRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import com.evidenceharbor.persistence.LookupRepository;
import com.evidenceharbor.persistence.OfficerRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class CocTransferController implements Initializable {

    // Action → new status mapping
    private static final Map<String, String> ACTION_STATUS = Map.ofEntries(
            Map.entry("Check In",               "In Custody"),
            Map.entry("Check Out to Person",     "Checked Out"),
            Map.entry("Submit for Analysis",     "Checked Out"),
            Map.entry("Agency Transfer",         "Checked Out"),
            Map.entry("Bank Deposit",            "Deposited"),
            Map.entry("Bank Removal",            "In Custody"),
            Map.entry("Destroy",                 "Destroyed"),
            Map.entry("Disburse",                "Disbursed"),
                Map.entry("Move to New Location",    "In Storage"),
            Map.entry("Return to Owner",         "Returned to Owner")
    );

    @FXML private ComboBox<String> actionCombo;
    @FXML private ComboBox<Officer> performedByCombo;
    @FXML private TextField fromLocationField;
    @FXML private Label toLocationLabel;
    @FXML private ComboBox<String> toLocationCombo;
    @FXML private Label toPersonLabel;
    @FXML private TextField toPersonField;
    @FXML private TextField reasonField;
    @FXML private TextArea notesArea;

    private Evidence evidence;
    private final ChainOfCustodyRepository cocRepo = new ChainOfCustodyRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final LookupRepository lookupRepo = new LookupRepository();
    private final OfficerRepository officerRepo = new OfficerRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            List<String> actions = lookupRepo.getTransferActions();
            actionCombo.setItems(FXCollections.observableArrayList(actions));

            List<String> storageLocations = lookupRepo.getStorageLocations();
            toLocationCombo.setItems(FXCollections.observableArrayList(storageLocations));

            List<Officer> officers = officerRepo.findAll();
            performedByCombo.setItems(FXCollections.observableArrayList(officers));
            Officer defaultOfficer = CurrentOfficerResolver.resolveDefaultOfficer(officerRepo);
            if (defaultOfficer != null) {
                performedByCombo.getItems().stream()
                        .filter(o -> o.getId() == defaultOfficer.getId())
                        .findFirst()
                        .ifPresent(o -> performedByCombo.getSelectionModel().select(o));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Show/hide conditional fields based on selected action
        actionCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateConditionalFields(newVal));
        updateConditionalFields(null);
    }

    private void updateConditionalFields(String action) {
        boolean showLocation = action == null || action.isEmpty()
                || action.equals("Check In")
                || action.equals("Move to New Location")
                || action.equals("Submit for Analysis")
                || action.equals("Agency Transfer")
                || action.equals("Bank Deposit")
                || action.equals("Bank Removal");

        boolean showPerson = action != null && (
                action.equals("Check Out to Person")
                || action.equals("Return to Owner")
                || action.equals("Agency Transfer"));

        toLocationLabel.setVisible(showLocation);
        toLocationCombo.setVisible(showLocation);
        toLocationLabel.setManaged(showLocation);
        toLocationCombo.setManaged(showLocation);

        toPersonLabel.setVisible(showPerson);
        toPersonField.setVisible(showPerson);
        toPersonLabel.setManaged(showPerson);
        toPersonField.setManaged(showPerson);
    }

    public void setEvidence(Evidence e) {
        this.evidence = e;
        fromLocationField.setText(e.getStorageLocation() != null ? e.getStorageLocation() : "");
    }

    @FXML
    private void onSave() {
        String action = actionCombo.getValue();
        if (action == null || action.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Please select an action.").showAndWait();
            return;
        }

        String toLocation = toLocationCombo.isVisible() ? toLocationCombo.getValue() : null;
        String toPerson = toPersonField.isVisible() ? toPersonField.getText().trim() : null;
        Officer selectedOfficer = performedByCombo.getValue();
        String performedBy = selectedOfficer == null ? "" : selectedOfficer.getName();

        ChainOfCustody entry = new ChainOfCustody();
        entry.setEvidenceId(evidence.getId());
        entry.setAction(action);
        entry.setPerformedBy(performedBy);
        entry.setPerformedByName(performedBy);
        entry.setFromLocation(fromLocationField.getText().trim());
        entry.setToLocation(toLocation);
        entry.setToPerson(toPerson != null && toPerson.isEmpty() ? null : toPerson);
        entry.setReason(reasonField.getText().trim());
        entry.setNotes(notesArea.getText().trim());

        try {
            cocRepo.addEntry(entry);

            // Update evidence status and storage location
            String newStatus = ACTION_STATUS.getOrDefault(action, evidence.getStatus());
            String newLocation = (toLocation != null && !toLocation.isBlank())
                    ? toLocation
                    : evidence.getStorageLocation();
            evidenceRepo.updateStatus(evidence.getId(), newStatus, newLocation);
            evidence.setStatus(newStatus);
            evidence.setStorageLocation(newLocation);

            actionCombo.getScene().getWindow().hide();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error saving: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onCancel() {
        actionCombo.getScene().getWindow().hide();
    }
}
