package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.QmInventoryAudit;
import com.evidenceharbor.persistence.QmRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class QmInventoryAuditController implements Initializable {

    @FXML private TableView<QmInventoryAudit> auditTable;
    @FXML private TableColumn<QmInventoryAudit, String> colADate;
    @FXML private TableColumn<QmInventoryAudit, String> colABy;
    @FXML private TableColumn<QmInventoryAudit, String> colAStatus;
    @FXML private TableColumn<QmInventoryAudit, String> colACompleted;
    @FXML private TableColumn<QmInventoryAudit, String> colANotes;
    @FXML private TableColumn<QmInventoryAudit, String> colAAction;

    private final QmRepository repo = new QmRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colADate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt()));
        colABy.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getCreatedBy())));
        colAStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colACompleted.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getCompletedAt())));
        colANotes.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNotes())));

        colAAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnComplete = new Button("Complete");
            { btnComplete.setStyle("-fx-background-color:#38a169;-fx-text-fill:white;"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                QmInventoryAudit a = getTableView().getItems().get(getIndex());
                btnComplete.setDisable("Completed".equals(a.getStatus()));
                btnComplete.setOnAction(e -> doComplete(a));
                setGraphic(btnComplete);
            }
        });

        load();
    }

    private void load() {
        auditTable.setItems(FXCollections.observableArrayList(repo.findAllAudits()));
    }

    @FXML
    private void onNewAudit() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("New Inventory Audit");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfBy   = new TextField();
        TextArea  taNotes = new TextArea(); taNotes.setPrefRowCount(3);

        grid.add(new Label("Created By:"), 0, 0); grid.add(tfBy,    1, 0);
        grid.add(new Label("Notes:"),      0, 1); grid.add(taNotes, 1, 1);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                repo.createAudit(tfBy.getText().trim(), taNotes.getText().trim());
                load();
            }
        });
    }

    private void doComplete(QmInventoryAudit a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark this audit as Completed?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) { repo.completeAudit(a.getId()); load(); } });
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

    @FXML private void onEquipment() { Navigator.get().showQmEquipment(); }
    @FXML private void onWeapons()   { Navigator.get().showQmWeapons(); }
    @FXML private void onUniforms()  { Navigator.get().showQmUniforms(); }

    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private String nvl(String s) { return s == null ? "" : s; }
}
