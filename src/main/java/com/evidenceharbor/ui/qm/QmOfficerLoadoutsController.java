package com.evidenceharbor.ui.qm;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.QmAssignment;
import com.evidenceharbor.persistence.QmRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class QmOfficerLoadoutsController implements Initializable {

    @FXML private ListView<String> officerList;
    @FXML private Label lblOfficerName;
    @FXML private Label lblItemCount;
    @FXML private TableView<QmAssignment> loadoutTable;
    @FXML private TableColumn<QmAssignment, String> colItem;
    @FXML private TableColumn<QmAssignment, String> colCategory;
    @FXML private TableColumn<QmAssignment, String> colSerial;
    @FXML private TableColumn<QmAssignment, String> colAssignedDate;
    @FXML private TableColumn<QmAssignment, String> colLoadoutAction;

    private final QmRepository repo = new QmRepository();
    private List<QmAssignment> allActive;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colItem.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEquipmentName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEquipmentCategory()));
        colSerial.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEquipmentSerial())));
        colAssignedDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAssignedDate()));

        colLoadoutAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnReturn = new Button("Return");
            { btnReturn.setStyle("-fx-background-color:#e53e3e;-fx-text-fill:white;"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                QmAssignment a = getTableView().getItems().get(getIndex());
                btnReturn.setOnAction(e -> doReturn(a));
                setGraphic(btnReturn);
            }
        });

        officerList.getSelectionModel().selectedItemProperty().addListener(
                (obs, o, sel) -> showOfficer(sel));

        loadAll();
    }

    private void loadAll() {
        allActive = repo.findActiveAssignments();
        List<String> officers = allActive.stream()
                .map(QmAssignment::getOfficerName)
                .distinct().sorted().collect(Collectors.toList());
        officerList.setItems(FXCollections.observableArrayList(officers));
        if (!officers.isEmpty()) officerList.getSelectionModel().selectFirst();
        else clearDetail();
    }

    private void showOfficer(String name) {
        if (name == null) { clearDetail(); return; }
        List<QmAssignment> items = allActive.stream()
                .filter(a -> name.equals(a.getOfficerName())).collect(Collectors.toList());
        lblOfficerName.setText(name);
        lblItemCount.setText(items.size() + " item(s) assigned");
        loadoutTable.setItems(FXCollections.observableArrayList(items));
    }

    private void clearDetail() {
        lblOfficerName.setText("No officer selected");
        lblItemCount.setText("");
        loadoutTable.getItems().clear();
    }

    private void doReturn(QmAssignment a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Return \"" + a.getEquipmentName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                repo.returnEquipment(a.getId(), a.getEquipmentId());
                loadAll();
            }
        });
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

    @FXML private void onEquipment() { showInfo("Equipment workspace is not configured yet."); }
    @FXML private void onWeapons()   { showInfo("Weapons workspace is not configured yet."); }
    @FXML private void onUniforms()  { showInfo("Uniforms workspace is not configured yet."); }

    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private String nvl(String s) { return s == null ? "" : s; }
}
