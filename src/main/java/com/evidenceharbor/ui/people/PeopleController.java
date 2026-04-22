package com.evidenceharbor.ui.people;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Person;
import com.evidenceharbor.persistence.PersonRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PeopleController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navDropboxBtn;

    @FXML private TextField searchField;
    @FXML private TableView<Person> peopleTable;
    @FXML private TableColumn<Person, String> colName;
    @FXML private TableColumn<Person, String> colType;
    @FXML private TableColumn<Person, String> colDob;
    @FXML private TableColumn<Person, String> colSsn;
    @FXML private TableColumn<Person, String> colAddress;
    @FXML private TableColumn<Person, String> colContact;

    private final PersonRepository personRepo = new PersonRepository();
    private List<Person> allPeople = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFullName()));
        colType.setCellValueFactory(cd -> new SimpleStringProperty("Civilian"));
        colDob.setCellValueFactory(cd -> new SimpleStringProperty("12/31/1900"));
        colSsn.setCellValueFactory(cd -> new SimpleStringProperty("***-**-0000"));
        colAddress.setCellValueFactory(cd -> new SimpleStringProperty("Address not set"));
        colContact.setCellValueFactory(cd -> new SimpleStringProperty(""));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearch(newVal));
        loadPeople();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);
    }

    private void loadPeople() {
        try {
            allPeople = personRepo.findAll();
            peopleTable.setItems(FXCollections.observableArrayList(allPeople));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applySearch(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            peopleTable.setItems(FXCollections.observableArrayList(allPeople));
            return;
        }
        peopleTable.setItems(FXCollections.observableArrayList(
                allPeople.stream()
                        .filter(p -> p.getFullName() != null && p.getFullName().toLowerCase().contains(q))
                        .collect(Collectors.toList())
        ));
    }

    @FXML private void onCases() { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople() { }
    @FXML private void onDropbox() { Navigator.get().showDropbox(); }
    @FXML private void onReports() { Navigator.get().showReports(); }
    @FXML private void onSettings() { Navigator.get().showSettings(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }    @FXML private void onBack() { Navigator.get().showCaseList(); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }
}
