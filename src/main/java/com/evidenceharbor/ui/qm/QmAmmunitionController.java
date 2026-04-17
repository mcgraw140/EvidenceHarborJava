package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.QmAmmunition;
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

public class QmAmmunitionController implements Initializable {

    @FXML private TableView<QmAmmunition> ammoTable;
    @FXML private TableColumn<QmAmmunition, String> colCaliber;
    @FXML private TableColumn<QmAmmunition, String> colQuantity;
    @FXML private TableColumn<QmAmmunition, String> colLocation;
    @FXML private TableColumn<QmAmmunition, String> colUpdated;
    @FXML private TableColumn<QmAmmunition, String> colNotes;
    @FXML private TableColumn<QmAmmunition, String> colAction;

    private final QmRepository repo = new QmRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colCaliber.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCaliber()));
        colQuantity.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        colLocation.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getLocation())));
        colUpdated.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUpdatedAt()));
        colNotes.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNotes())));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDel  = new Button("Delete");
            { btnEdit.setStyle("-fx-background-color:#718096;-fx-text-fill:white;");
              btnDel.setStyle("-fx-background-color:#e53e3e;-fx-text-fill:white;"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                QmAmmunition a = getTableView().getItems().get(getIndex());
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, btnEdit, btnDel);
                btnEdit.setOnAction(e -> showDialog(a));
                btnDel.setOnAction(e -> doDelete(a));
                setGraphic(box);
            }
        });

        load();
    }

    private void load() {
        ammoTable.setItems(FXCollections.observableArrayList(repo.findAllAmmunition()));
    }

    @FXML
    private void onAdd() { showDialog(null); }

    private void showDialog(QmAmmunition existing) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Add Ammunition" : "Edit Ammunition");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfCaliber  = new TextField(existing != null ? existing.getCaliber() : "");
        TextField tfQuantity = new TextField(existing != null ? String.valueOf(existing.getQuantity()) : "0");
        TextField tfLocation = new TextField(existing != null ? nvl(existing.getLocation()) : "");
        TextArea  taNotes    = new TextArea(existing != null ? nvl(existing.getNotes()) : "");
        taNotes.setPrefRowCount(2);

        grid.add(new Label("Caliber:*"),  0, 0); grid.add(tfCaliber,  1, 0);
        grid.add(new Label("Quantity:*"), 0, 1); grid.add(tfQuantity, 1, 1);
        grid.add(new Label("Location:"),  0, 2); grid.add(tfLocation, 1, 2);
        grid.add(new Label("Notes:"),     0, 3); grid.add(taNotes,    1, 3);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String caliber = tfCaliber.getText().trim();
                if (caliber.isEmpty()) { showError("Caliber required."); return; }
                int qty;
                try { qty = Integer.parseInt(tfQuantity.getText().trim()); }
                catch (NumberFormatException e) { showError("Quantity must be a number."); return; }
                if (existing == null) {
                    repo.createAmmunition(caliber, qty, tfLocation.getText().trim(), taNotes.getText().trim());
                } else {
                    repo.updateAmmunition(existing.getId(), caliber, qty,
                            tfLocation.getText().trim(), taNotes.getText().trim());
                }
                load();
            }
        });
    }

    private void doDelete(QmAmmunition a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + a.getCaliber() + " entry?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) { repo.deleteAmmunition(a.getId()); load(); } });
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
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private String nvl(String s) { return s == null ? "" : s; }
}
