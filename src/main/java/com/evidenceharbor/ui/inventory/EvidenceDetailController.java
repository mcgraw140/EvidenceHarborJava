package com.evidenceharbor.ui.inventory;

import com.evidenceharbor.domain.ChainOfCustody;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.persistence.ChainOfCustodyRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EvidenceDetailController implements Initializable {

    @FXML private Label labelBarcode;
    @FXML private Label labelStatusBadge;
    @FXML private Label infoCase;
    @FXML private Label infoType;
    @FXML private Label infoDate;
    @FXML private Label infoLocation;
    @FXML private Label infoDescription;
    @FXML private Label cocCountLabel;
    @FXML private TableView<ChainOfCustody> cocTable;
    @FXML private TableColumn<ChainOfCustody, String> cocColTimestamp;
    @FXML private TableColumn<ChainOfCustody, String> cocColAction;
    @FXML private TableColumn<ChainOfCustody, String> cocColPerformedBy;
    @FXML private TableColumn<ChainOfCustody, String> cocColFrom;
    @FXML private TableColumn<ChainOfCustody, String> cocColTo;
    @FXML private TableColumn<ChainOfCustody, String> cocColToPerson;
    @FXML private TableColumn<ChainOfCustody, String> cocColNotes;

    private Evidence evidence;
    private String caseNumber;
    private final ChainOfCustodyRepository cocRepo = new ChainOfCustodyRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cocColTimestamp.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTimestamp()));
        cocColAction.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAction()));
        cocColPerformedBy.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getPerformedByName())));
        cocColFrom.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getFromLocation())));
        cocColTo.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getToLocation())));
        cocColToPerson.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getToPerson())));
        cocColNotes.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getNotes())));
    }

    public void setEvidence(Evidence e, String caseNumber) {
        this.evidence = e;
        this.caseNumber = caseNumber;
        populateFields();
        loadCoc();
    }

    private void populateFields() {
        labelBarcode.setText(ns(evidence.getBarcode()));
        String status = ns(evidence.getStatus());
        labelStatusBadge.setText(status);
        String color = statusColor(status);
        labelStatusBadge.setStyle("-fx-background-color: " + color + "33; -fx-text-fill: " + color +
                "; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 6;");
        infoCase.setText(caseNumber);
        infoType.setText(ns(evidence.getEvidenceType()));
        infoDate.setText(ns(evidence.getCollectionDate()));
        infoLocation.setText(ns(evidence.getStorageLocation()));
        infoDescription.setText(ns(evidence.getDescription()));
    }

    private void loadCoc() {
        try {
            List<ChainOfCustody> entries = cocRepo.findByEvidence(evidence.getId());
            cocTable.setItems(FXCollections.observableArrayList(entries));
            cocCountLabel.setText(entries.size() + " entries");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onAddTransfer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CocTransfer.fxml"));
            Parent root = loader.load();
            CocTransferController ctrl = loader.getController();
            ctrl.setEvidence(evidence);

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(labelBarcode.getScene().getWindow());
            dialog.setTitle("Add Chain of Custody Entry");
            Scene scene = new Scene(root, 560, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            // Reload evidence status and CoC after transfer
            loadCoc();
            populateFields();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onClose() {
        labelBarcode.getScene().getWindow().hide();
    }

    private String statusColor(String status) {
        if (status == null) return "#888888";
        return switch (status) {
            case "In Dropbox"        -> "#f59e0b";
            case "In Custody"        -> "#22c55e";
            case "In Storage"        -> "#14b8a6";
            case "Checked In"        -> "#22c55e";
            case "Checked Out"       -> "#3b82f6";
            case "Deposited"         -> "#6366f1";
            case "Destroyed"         -> "#ef4444";
            case "Disbursed"         -> "#6b7280";
            case "Returned to Owner" -> "#14b8a6";
            case "Missing"           -> "#dc2626";
            case "Pending"           -> "#94a3b8";
            default                  -> "#64748b";
        };
    }

    private String ns(String s) { return s == null ? "" : s; }
}
