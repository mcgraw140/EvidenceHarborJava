package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.QmVehicleImpound;
import com.evidenceharbor.persistence.QmRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class QmVehicleImpoundController implements Initializable {

    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatusFilter;

    @FXML private TableView<QmVehicleImpound> vehicleTable;
    @FXML private TableColumn<QmVehicleImpound, String> colMake;
    @FXML private TableColumn<QmVehicleImpound, String> colModel;
    @FXML private TableColumn<QmVehicleImpound, String> colYear;
    @FXML private TableColumn<QmVehicleImpound, String> colPlate;
    @FXML private TableColumn<QmVehicleImpound, String> colVin;
    @FXML private TableColumn<QmVehicleImpound, String> colColor;
    @FXML private TableColumn<QmVehicleImpound, String> colImpoundDate;
    @FXML private TableColumn<QmVehicleImpound, String> colVStatus;
    @FXML private TableColumn<QmVehicleImpound, String> colReason;
    @FXML private TableColumn<QmVehicleImpound, String> colVAction;

    private final QmRepository repo = new QmRepository();
    private FilteredList<QmVehicleImpound> filtered;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbStatusFilter.setItems(FXCollections.observableArrayList("All", "Impounded", "Released"));
        cbStatusFilter.setValue("All");

        colMake.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getMake())));
        colModel.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getModel())));
        colYear.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getYear())));
        colPlate.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getPlate())));
        colVin.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getVin())));
        colColor.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getColor())));
        colImpoundDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImpoundDate()));
        colVStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colReason.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getReason())));

        colVAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnRelease = new Button("Release");
            private final Button btnDel     = new Button("Delete");
            { btnRelease.setStyle("-fx-background-color:#38a169;-fx-text-fill:white;");
              btnDel.setStyle("-fx-background-color:#e53e3e;-fx-text-fill:white;"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                QmVehicleImpound v = getTableView().getItems().get(getIndex());
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, btnRelease, btnDel);
                btnRelease.setDisable("Released".equals(v.getStatus()));
                btnRelease.setOnAction(e -> doRelease(v));
                btnDel.setOnAction(e -> doDelete(v));
                setGraphic(box);
            }
        });

        tfSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        cbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        load();
    }

    private void load() {
        filtered = new FilteredList<>(FXCollections.observableArrayList(repo.findAllVehicles()));
        vehicleTable.setItems(filtered);
    }

    private void applyFilter() {
        String s = tfSearch.getText().toLowerCase();
        String status = cbStatusFilter.getValue();
        filtered.setPredicate(v -> {
            boolean matchSearch = s.isEmpty()
                    || nvl(v.getPlate()).toLowerCase().contains(s)
                    || nvl(v.getMake()).toLowerCase().contains(s)
                    || nvl(v.getModel()).toLowerCase().contains(s)
                    || nvl(v.getVin()).toLowerCase().contains(s);
            boolean matchStatus = "All".equals(status) || status.equals(v.getStatus());
            return matchSearch && matchStatus;
        });
    }

    @FXML
    private void onAddVehicle() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Impound Vehicle");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfMake   = new TextField();
        TextField tfModel  = new TextField();
        TextField tfYear   = new TextField();
        TextField tfVin    = new TextField();
        TextField tfPlate  = new TextField();
        TextField tfColor  = new TextField();
        TextField tfReason = new TextField();
        TextArea  taNotes  = new TextArea(); taNotes.setPrefRowCount(2);
        DatePicker dpDate  = new DatePicker(LocalDate.now());

        grid.add(new Label("Make:"),         0, 0); grid.add(tfMake,   1, 0);
        grid.add(new Label("Model:"),        0, 1); grid.add(tfModel,  1, 1);
        grid.add(new Label("Year:"),         0, 2); grid.add(tfYear,   1, 2);
        grid.add(new Label("Plate:"),        0, 3); grid.add(tfPlate,  1, 3);
        grid.add(new Label("VIN:"),          0, 4); grid.add(tfVin,    1, 4);
        grid.add(new Label("Color:"),        0, 5); grid.add(tfColor,  1, 5);
        grid.add(new Label("Impound Date:"), 0, 6); grid.add(dpDate,   1, 6);
        grid.add(new Label("Reason:"),       0, 7); grid.add(tfReason, 1, 7);
        grid.add(new Label("Notes:"),        0, 8); grid.add(taNotes,  1, 8);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String dateStr = dpDate.getValue() != null ? dpDate.getValue().toString() : LocalDate.now().toString();
                repo.createVehicle(tfMake.getText().trim(), tfModel.getText().trim(),
                        tfYear.getText().trim(), tfVin.getText().trim(),
                        tfPlate.getText().trim(), tfColor.getText().trim(),
                        dateStr, tfReason.getText().trim(), taNotes.getText().trim());
                load();
            }
        });
    }

    private void doRelease(QmVehicleImpound v) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Release vehicle " + nvl(v.getMake()) + " " + nvl(v.getModel()) + " (" + nvl(v.getPlate()) + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) { repo.releaseVehicle(v.getId()); load(); } });
    }

    private void doDelete(QmVehicleImpound v) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete vehicle record?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) { repo.deleteVehicle(v.getId()); load(); } });
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
