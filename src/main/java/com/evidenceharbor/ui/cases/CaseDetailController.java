package com.evidenceharbor.ui.cases;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.*;
import com.evidenceharbor.persistence.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class CaseDetailController implements Initializable {

    @FXML private Label breadcrumbCase;
    @FXML private Label caseNumberLabel;
    @FXML private Label officerLabel;
    @FXML private Label assistingLabel;

    @FXML private VBox chargesList;
    @FXML private ComboBox<Charge> chargeCombo;

    @FXML private TableView<CasePerson> peopleTable;
    @FXML private TableColumn<CasePerson, String> colPersonName;
    @FXML private TableColumn<CasePerson, String> colPersonRole;
    @FXML private TableColumn<CasePerson, String> colPersonAction;

    @FXML private TableView<Evidence> evidenceTable;
    @FXML private TableColumn<Evidence, String> colBarcode;
    @FXML private TableColumn<Evidence, String> colEvidDesc;
    @FXML private TableColumn<Evidence, String> colEvidType;
    @FXML private TableColumn<Evidence, String> colEvidLoc;
    @FXML private TableColumn<Evidence, String> colEvidStatus;

    @FXML private TableView<QmVehicleImpound> vehicleTable;
    @FXML private TableColumn<QmVehicleImpound, String> colVehicleYear;
    @FXML private TableColumn<QmVehicleImpound, String> colVehicleMake;
    @FXML private TableColumn<QmVehicleImpound, String> colVehicleModel;
    @FXML private TableColumn<QmVehicleImpound, String> colVehiclePlate;
    @FXML private TableColumn<QmVehicleImpound, String> colVehicleVin;
    @FXML private TableColumn<QmVehicleImpound, String> colVehicleStatus;
    @FXML private TableColumn<QmVehicleImpound, String> colVehicleDate;

    private Case currentCase;
    private final CaseRepository caseRepo = new CaseRepository();
    private final ChargeRepository chargeRepo = new ChargeRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final PersonRepository personRepo = new PersonRepository();
    private final QmRepository qmRepo = new QmRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colPersonName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPerson().getFullName()));
        colPersonRole.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRole()));
        colPersonAction.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            { removeBtn.getStyleClass().add("btn-danger");
              removeBtn.setOnAction(e -> {
                  CasePerson cp = getTableRow().getItem();
                  if (cp != null) removePerson(cp);
              }); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        colBarcode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getBarcode()));
        colEvidDesc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDescription()));
        colEvidType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEvidenceType()));
        colEvidLoc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStorageLocation()));
        colEvidStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));

        colVehicleYear.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getYear())));
        colVehicleMake.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getMake())));
        colVehicleModel.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getModel())));
        colVehiclePlate.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getPlate())));
        colVehicleVin.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getVin())));
        colVehicleStatus.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getStatus())));
        colVehicleDate.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getImpoundDate())));
    }

    public void setCase(Case c) {
        this.currentCase = c;
        refresh();
    }

    private void refresh() {
        try {
            currentCase = caseRepo.findById(currentCase.getId());
        } catch (Exception e) { showError(e); return; }

        breadcrumbCase.setText("Case #" + currentCase.getCaseNumber());
        caseNumberLabel.setText("Case #" + currentCase.getCaseNumber());
        officerLabel.setText(currentCase.getOfficer().getName());

        // Charges list
        chargesList.getChildren().clear();
        for (Charge ch : currentCase.getCharges()) {
            HBox row = new HBox();
            row.getStyleClass().add("charge-row");
            row.setSpacing(10);
            Label lbl = new Label(ch.toString());
            lbl.getStyleClass().add("charge-label");
            HBox.setHgrow(lbl, Priority.ALWAYS);
            Button del = new Button("🗑");
            del.getStyleClass().add("btn-icon-danger");
            del.setOnAction(e -> removeCharge(ch));
            row.getChildren().addAll(lbl, del);
            chargesList.getChildren().add(row);
        }

        // Charge combo (available to add)
        try {
            List<Charge> all = chargeRepo.findAll();
            all.removeIf(ch -> currentCase.getCharges().stream().anyMatch(c -> c.getId() == ch.getId()));
            chargeCombo.setItems(FXCollections.observableArrayList(all));
        } catch (Exception e) { showError(e); }

        // People table
        peopleTable.setItems(FXCollections.observableArrayList(currentCase.getPersons()));

        // Evidence table
        try {
            evidenceTable.setItems(FXCollections.observableArrayList(evidenceRepo.findByCase(currentCase.getId())));
        } catch (Exception e) { showError(e); }

        // Vehicles table (impound records linked to this case)
        vehicleTable.setItems(FXCollections.observableArrayList(qmRepo.findVehiclesByCase(currentCase.getId())));
    }

    @FXML private void onBack() { Navigator.get().showCaseList(); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }
    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onQuartermaster() { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }    @FXML private void onAddEvidence() { Navigator.get().showAddEvidence(currentCase); }
    @FXML
    private void onImpoundVehicle() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Impound Vehicle");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField tfYear = new TextField();
        TextField tfMake = new TextField();
        TextField tfModel = new TextField();
        TextField tfPlate = new TextField();
        TextField tfVin = new TextField();
        TextField tfColor = new TextField();
        TextField tfReason = new TextField();
        DatePicker dpDate = new DatePicker(LocalDate.now());
        TextArea taNotes = new TextArea();
        taNotes.setPrefRowCount(2);

        grid.add(new Label("Year:"), 0, 0); grid.add(tfYear, 1, 0);
        grid.add(new Label("Make:"), 0, 1); grid.add(tfMake, 1, 1);
        grid.add(new Label("Model:"), 0, 2); grid.add(tfModel, 1, 2);
        grid.add(new Label("Plate:"), 0, 3); grid.add(tfPlate, 1, 3);
        grid.add(new Label("VIN:"), 0, 4); grid.add(tfVin, 1, 4);
        grid.add(new Label("Color:"), 0, 5); grid.add(tfColor, 1, 5);
        grid.add(new Label("Impound Date:"), 0, 6); grid.add(dpDate, 1, 6);
        grid.add(new Label("Reason:"), 0, 7); grid.add(tfReason, 1, 7);
        grid.add(new Label("Notes:"), 0, 8); grid.add(taNotes, 1, 8);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e -> {
            if (tfMake.getText().trim().isEmpty() || tfModel.getText().trim().isEmpty() || tfVin.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Make, Model, and VIN are required.").showAndWait();
                e.consume();
                return;
            }

            String date = dpDate.getValue() == null ? LocalDate.now().toString() : dpDate.getValue().toString();
            qmRepo.createVehicleForCase(
                    currentCase.getId(),
                    tfMake.getText().trim(),
                    tfModel.getText().trim(),
                    tfYear.getText().trim(),
                    tfVin.getText().trim(),
                    tfPlate.getText().trim(),
                    tfColor.getText().trim(),
                    date,
                    tfReason.getText().trim(),
                    taNotes.getText().trim());
            refresh();
        });

        dlg.showAndWait();
    }
    @FXML private void onPrint() {
        new Alert(Alert.AlertType.INFORMATION, "Print report coming soon.").showAndWait();
    }

    @FXML
    private void onAddCharge() {
        Charge selected = chargeCombo.getValue();
        if (selected == null) return;
        try {
            chargeRepo.addToCase(currentCase.getId(), selected.getId());
            refresh();
        } catch (Exception e) { showError(e); }
    }

    private void removeCharge(Charge ch) {
        try {
            chargeRepo.removeFromCase(currentCase.getId(), ch.getId());
            refresh();
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void onAddNewCharge() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add New Charge");
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        TextField codeField = new TextField(); codeField.setPromptText("Charge code");
        TextField descField = new TextField(); descField.setPromptText("Description");
        grid.add(new Label("Code:"), 0, 0); grid.add(codeField, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e -> {
            try {
                Charge ch = new Charge();
                ch.setCode(codeField.getText().trim());
                ch.setDescription(descField.getText().trim());
                chargeRepo.save(ch);
                chargeRepo.addToCase(currentCase.getId(), ch.getId());
                refresh();
            } catch (Exception ex) { showError(ex); e.consume(); }
        });
        dialog.showAndWait();
    }

    @FXML
    private void onAssociatePerson() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Associate Person");
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ComboBox<Person> personBox = new ComboBox<>();
        ComboBox<String> roleBox = new ComboBox<>();
        TextField newPersonField = new TextField(); newPersonField.setPromptText("Or type new person name");

        List<Person> people = new ArrayList<>();
        try { people = personRepo.findAll(); } catch (Exception e) { showError(e); }
        makeSearchableComboBox(personBox, people, Person::getFullName);

        List<String> roles = Arrays.asList(
            "Victim", "Suspect", "Witness", "Owner", "Reporting Party",
            "Complainant", "Driver", "Passenger", "Other");
        makeSearchableComboBox(roleBox, roles, Function.identity());
        roleBox.setPromptText("Select role...");

        grid.add(new Label("Existing Person:"), 0, 0); grid.add(personBox, 1, 0);
        grid.add(new Label("New Person Name:"), 0, 1); grid.add(newPersonField, 1, 1);
        grid.add(new Label("Role:"), 0, 2); grid.add(roleBox, 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e -> {
            try {
            String role = roleBox.getEditor().getText().trim();
                if (role.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Role is required.").showAndWait(); e.consume(); return; }
                Person p = personBox.getValue();
                if (p == null) {
                    String name = newPersonField.getText().trim();
                    if (name.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Select or enter a person.").showAndWait(); e.consume(); return; }
                    p = new Person(); p.setFullName(name);
                    personRepo.save(p);
                }
                caseRepo.associatePerson(currentCase.getId(), p.getId(), role);
                refresh();
            } catch (Exception ex) { showError(ex); e.consume(); }
        });
        dialog.showAndWait();
    }

    private <T> void makeSearchableComboBox(ComboBox<T> comboBox, List<T> sourceItems, Function<T, String> labelMapper) {
        ObservableList<T> originalItems = FXCollections.observableArrayList(sourceItems);
        FilteredList<T> filteredItems = new FilteredList<>(originalItems, item -> true);

        comboBox.setEditable(true);
        comboBox.setItems(filteredItems);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : labelMapper.apply(object);
            }

            @Override
            public T fromString(String string) {
                if (string == null || string.isBlank()) return null;
                for (T item : originalItems) {
                    if (labelMapper.apply(item).equalsIgnoreCase(string.trim())) {
                        return item;
                    }
                }
                return comboBox.getValue();
            }
        });

        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String query = newText == null ? "" : newText.trim().toLowerCase();
            filteredItems.setPredicate(item -> labelMapper.apply(item).toLowerCase().contains(query));
            if (!comboBox.isShowing()) {
                comboBox.show();
            }
        });

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                comboBox.getEditor().setText(labelMapper.apply(newVal));
            }
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String selectedText = comboBox.getValue() == null ? "" : labelMapper.apply(comboBox.getValue());
                comboBox.getEditor().setText(selectedText);
                filteredItems.setPredicate(item -> true);
            }
        });
    }

    private void removePerson(CasePerson cp) {
        try { caseRepo.removePerson(cp.getId()); refresh(); }
        catch (Exception e) { showError(e); }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
