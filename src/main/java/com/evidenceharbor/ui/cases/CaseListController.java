package com.evidenceharbor.ui.cases;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Case;
import com.evidenceharbor.domain.CasePerson;
import com.evidenceharbor.persistence.CaseRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CaseListController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<Case> caseTable;
    @FXML private TableColumn<Case, String> colCaseNumber;
    @FXML private TableColumn<Case, String> colDate;
    @FXML private TableColumn<Case, String> colPeople;
    @FXML private TableColumn<Case, String> colOfficer;

    private final CaseRepository caseRepo = new CaseRepository();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colCaseNumber.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCaseNumber()));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getIncidentDate().format(FMT)));
        colPeople.setCellValueFactory(cd -> {
            List<CasePerson> people = cd.getValue().getPersons();
            if (people.isEmpty()) return new SimpleStringProperty("N/A");
            return new SimpleStringProperty(people.stream()
                    .map(p -> p.getPerson().getFullName() + " (" + p.getRole() + ")")
                    .collect(Collectors.joining(", ")));
        });
        colOfficer.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOfficer().getName()));

        // Double-click to open case detail
        caseTable.setRowFactory(tv -> {
            TableRow<Case> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    Navigator.get().showCaseDetail(row.getItem());
                }
            });
            return row;
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadCases(newVal));
        loadCases("");
    }

    private void loadCases(String query) {
        try {
            List<Case> cases = query == null || query.isBlank()
                    ? caseRepo.findAll()
                    : caseRepo.search(query);
            caseTable.setItems(FXCollections.observableArrayList(cases));
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void onNewCase() {
        NewCaseDialog.show(caseRepo, () -> loadCases(searchField.getText()));
    }

    @FXML
    private void onCases() {
        loadCases(searchField.getText());
    }

    @FXML
    private void onInventory() {
        Navigator.get().showInventory();
    }

    @FXML
    private void onPeople() {
        Navigator.get().showPeople();
    }

    @FXML
    private void onDropbox() {
        Navigator.get().showDropbox();
    }

    @FXML
    private void onReports() {
        Navigator.get().showReports();
    }

    private void showError(Exception e) {
        new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
    }
}
