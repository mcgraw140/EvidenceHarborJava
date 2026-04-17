package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.QmEquipment;
import com.evidenceharbor.domain.QmAssignment;
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
import java.util.List;
import java.util.ResourceBundle;

public class QmAssignEquipmentController implements Initializable {

    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbCategoryFilter;
    @FXML private ComboBox<String> cbStatusFilter;

    @FXML private TableView<QmEquipment> equipTable;
    @FXML private TableColumn<QmEquipment, String> colName;
    @FXML private TableColumn<QmEquipment, String> colCategory;
    @FXML private TableColumn<QmEquipment, String> colMake;
    @FXML private TableColumn<QmEquipment, String> colModel;
    @FXML private TableColumn<QmEquipment, String> colSerial;
    @FXML private TableColumn<QmEquipment, String> colStatus;
    @FXML private TableColumn<QmEquipment, String> colEquipAction;

    @FXML private TableView<QmAssignment> assignTable;
    @FXML private TableColumn<QmAssignment, String> colAOfficer;
    @FXML private TableColumn<QmAssignment, String> colAItem;
    @FXML private TableColumn<QmAssignment, String> colACategory;
    @FXML private TableColumn<QmAssignment, String> colASerial;
    @FXML private TableColumn<QmAssignment, String> colADate;
    @FXML private TableColumn<QmAssignment, String> colAAction;

    private final QmRepository repo = new QmRepository();
    private FilteredList<QmEquipment> filteredEquipment;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbCategoryFilter.setItems(FXCollections.observableArrayList(
                "All", "Weapon", "Uniform", "Equipment", "Vehicle", "Other"));
        cbCategoryFilter.setValue("All");
        cbStatusFilter.setItems(FXCollections.observableArrayList(
                "All", "Available", "Assigned", "Maintenance"));
        cbStatusFilter.setValue("All");

        // Equipment table
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colMake.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getMake())));
        colModel.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getModel())));
        colSerial.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getSerial())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        colEquipAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnAssign = new Button("Assign");
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDel    = new Button("Del");
            {
                btnAssign.setStyle("-fx-background-color:#3182ce;-fx-text-fill:white;");
                btnEdit.setStyle("-fx-background-color:#718096;-fx-text-fill:white;");
                btnDel.setStyle("-fx-background-color:#e53e3e;-fx-text-fill:white;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                QmEquipment eq = getTableView().getItems().get(getIndex());
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, btnAssign, btnEdit, btnDel);
                btnAssign.setDisable(!"Available".equals(eq.getStatus()));
                btnAssign.setOnAction(e -> doAssign(eq));
                btnEdit.setOnAction(e -> doEditEquipment(eq));
                btnDel.setOnAction(e -> doDeleteEquipment(eq));
                setGraphic(box);
            }
        });

        // Assignment table
        colAOfficer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOfficerName()));
        colAItem.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEquipmentName()));
        colACategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEquipmentCategory()));
        colASerial.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEquipmentSerial())));
        colADate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAssignedDate()));

        colAAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnReturn = new Button("Return");
            { btnReturn.setStyle("-fx-background-color:#38a169;-fx-text-fill:white;"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                QmAssignment a = getTableView().getItems().get(getIndex());
                btnReturn.setOnAction(e -> doReturn(a));
                setGraphic(btnReturn);
            }
        });

        tfSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        cbCategoryFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        cbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        loadAll();
    }

    private void loadAll() {
        List<QmEquipment> all = repo.findAllEquipment();
        filteredEquipment = new FilteredList<>(FXCollections.observableArrayList(all));
        equipTable.setItems(filteredEquipment);
        assignTable.setItems(FXCollections.observableArrayList(repo.findActiveAssignments()));
    }

    private void applyFilter() {
        String search = tfSearch.getText().toLowerCase();
        String cat    = cbCategoryFilter.getValue();
        String status = cbStatusFilter.getValue();
        filteredEquipment.setPredicate(eq -> {
            boolean matchSearch = search.isEmpty()
                    || eq.getName().toLowerCase().contains(search)
                    || nvl(eq.getSerial()).toLowerCase().contains(search);
            boolean matchCat    = "All".equals(cat)    || cat.equals(eq.getCategory());
            boolean matchStatus = "All".equals(status) || status.equals(eq.getStatus());
            return matchSearch && matchCat && matchStatus;
        });
    }

    @FXML
    private void onAddEquipment() {
        showEquipmentDialog(null);
    }

    private void doAssign(QmEquipment eq) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Assign: " + eq.getName());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        TextField tfOfficer = new TextField();
        TextArea taNotes    = new TextArea(); taNotes.setPrefRowCount(2);
        grid.add(new Label("Officer Name:*"), 0, 0); grid.add(tfOfficer, 1, 0);
        grid.add(new Label("Notes:"),         0, 1); grid.add(taNotes,   1, 1);
        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String officer = tfOfficer.getText().trim();
                if (officer.isEmpty()) { showError("Officer name required."); return; }
                repo.assignEquipment(eq.getId(), officer, taNotes.getText().trim());
                loadAll();
            }
        });
    }

    private void doReturn(QmAssignment a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Return \"" + a.getEquipmentName() + "\" from " + a.getOfficerName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { repo.returnEquipment(a.getId(), a.getEquipmentId()); loadAll(); }
        });
    }

    private void doEditEquipment(QmEquipment eq) {
        showEquipmentDialog(eq);
    }

    private void doDeleteEquipment(QmEquipment eq) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + eq.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { repo.deleteEquipment(eq.getId()); loadAll(); }
        });
    }

    private void showEquipmentDialog(QmEquipment existing) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Add Equipment" : "Edit Equipment");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfName     = new TextField(existing != null ? existing.getName() : "");
        ComboBox<String> cbCat = new ComboBox<>(FXCollections.observableArrayList(
                "Weapon", "Uniform", "Equipment", "Vehicle", "Other"));
        cbCat.setValue(existing != null ? existing.getCategory() : "Equipment");
        TextField tfMake   = new TextField(existing != null ? nvl(existing.getMake())   : "");
        TextField tfModel  = new TextField(existing != null ? nvl(existing.getModel())  : "");
        TextField tfSerial = new TextField(existing != null ? nvl(existing.getSerial()) : "");
        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList(
                "Available", "Assigned", "Maintenance"));
        cbStatus.setValue(existing != null ? existing.getStatus() : "Available");
        TextArea taNotes = new TextArea(existing != null ? nvl(existing.getNotes()) : "");
        taNotes.setPrefRowCount(2);

        grid.add(new Label("Name:*"),     0, 0); grid.add(tfName,   1, 0);
        grid.add(new Label("Category:*"), 0, 1); grid.add(cbCat,    1, 1);
        grid.add(new Label("Make:"),      0, 2); grid.add(tfMake,   1, 2);
        grid.add(new Label("Model:"),     0, 3); grid.add(tfModel,  1, 3);
        grid.add(new Label("Serial #:"),  0, 4); grid.add(tfSerial, 1, 4);
        grid.add(new Label("Status:"),    0, 5); grid.add(cbStatus, 1, 5);
        grid.add(new Label("Notes:"),     0, 6); grid.add(taNotes,  1, 6);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String name = tfName.getText().trim();
                if (name.isEmpty()) { showError("Name required."); return; }
                if (existing == null) {
                    repo.createEquipment(name, cbCat.getValue(), tfMake.getText().trim(),
                            tfModel.getText().trim(), tfSerial.getText().trim(), taNotes.getText().trim());
                } else {
                    repo.updateEquipment(existing.getId(), name, cbCat.getValue(),
                            tfMake.getText().trim(), tfModel.getText().trim(),
                            tfSerial.getText().trim(), cbStatus.getValue(), taNotes.getText().trim());
                }
                loadAll();
            }
        });
    }

    // ── Nav bar ───────────────────────────────────────────────────────────────
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
