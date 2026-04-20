package com.evidenceharbor.ui.cases;

import com.evidenceharbor.app.NavHelper;
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

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navDropboxBtn;

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
    private final LookupRepository lookupRepo = new LookupRepository();

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
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);

        // Make chargeCombo type-searchable
        chargeCombo.setEditable(true);
        chargeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Charge c) { return c == null ? "" : c.toString(); }
            @Override public Charge fromString(String s) {
                return chargeCombo.getItems().stream()
                        .filter(c -> c.toString().equalsIgnoreCase(s))
                        .findFirst().orElse(null);
            }
        });
        chargeCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Charge current = chargeCombo.getValue();
            // Don't filter when a real selection was just made
            if (current != null && current.toString().equals(newVal)) return;
            String lower = newVal == null ? "" : newVal.toLowerCase();
            ObservableList<Charge> base = (ObservableList<Charge>) chargeCombo.getUserData();
            if (base != null) {
                chargeCombo.hide();
                chargeCombo.getItems().setAll(
                    base.filtered(c -> c.toString().toLowerCase().contains(lower))
                );
                if (!newVal.isBlank()) chargeCombo.show();
            }
        });
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
            ObservableList<Charge> chargeItems = FXCollections.observableArrayList(all);
            chargeCombo.setUserData(chargeItems);
            chargeCombo.setItems(chargeItems);
            chargeCombo.setValue(null);
            chargeCombo.getEditor().clear();
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
        dialog.setTitle("Associate Person with Case #" + currentCase.getCaseNumber());
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ComboBox<Person> personBox = new ComboBox<>();
        ComboBox<String> roleBox = new ComboBox<>();
        Button createPersonBtn = new Button("+ Create New Person");

        List<Person> people = new ArrayList<>();
        try { people = personRepo.findAll(); } catch (Exception e) { showError(e); }
        makeSearchableComboBox(personBox, people, Person::getFullName);

        List<String> roles = getPersonRolesForDropdown();
        makeSearchableComboBox(roleBox, roles, Function.identity());
        roleBox.setPromptText("Select role...");
        createPersonBtn.setOnAction(evt -> {
            NewPersonSelection created = showCreatePersonDialog(roles);
            if (created == null || created.person() == null) {
                return;
            }
            personBox.getItems().add(created.person());
            personBox.getSelectionModel().select(created.person());
            if (created.role() != null && !created.role().isBlank()) {
                roleBox.getSelectionModel().select(created.role());
                roleBox.getEditor().setText(created.role());
            }
        });

        grid.add(new Label("Person:"), 0, 0); grid.add(personBox, 1, 0);
        grid.add(new Label(""), 0, 1); grid.add(createPersonBtn, 1, 1);
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
                if (p == null) { new Alert(Alert.AlertType.WARNING, "Select a person.").showAndWait(); e.consume(); return; }
                caseRepo.associatePerson(currentCase.getId(), p.getId(), role);
                refresh();
            } catch (Exception ex) { showError(ex); e.consume(); }
        });
        dialog.showAndWait();
    }

    private NewPersonSelection showCreatePersonDialog(List<String> roles) {
        Dialog<NewPersonSelection> dialog = new Dialog<>();
        dialog.setTitle("Add New Person");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");

        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("mm/dd/yyyy");

        TextField ssnField = new TextField();
        ssnField.setPromptText("SSN (XXX-XX-XXXX) - Optional");

        TextField streetField = new TextField();
        streetField.setPromptText("Street");
        TextField cityField = new TextField();
        cityField.setPromptText("City");
        TextField zipField = new TextField();
        zipField.setPromptText("Zip Code");

        ComboBox<String> stateBox = new ComboBox<>();
        makeSearchableComboBox(stateBox, Arrays.asList(
                "AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","IA","ID","IL","IN","KS","KY","LA",
                "MA","MD","ME","MI","MN","MO","MS","MT","NC","ND","NE","NH","NJ","NM","NV","NY","OH","OK",
                "OR","PA","RI","SC","SD","TN","TX","UT","VA","VT","WA","WI","WV","WY"
        ), Function.identity());
        stateBox.setPromptText("Select...");

        TextField contactField = new TextField();
        contactField.setPromptText("Contact Info (Phone/Email) - Optional");

        ComboBox<String> roleBox = new ComboBox<>();
        makeSearchableComboBox(roleBox, roles, Function.identity());
        roleBox.setPromptText("Select...");

        grid.add(new Label("Full Name"), 0, 0); grid.add(fullNameField, 1, 0);
        grid.add(new Label("Date of Birth"), 0, 1); grid.add(dobPicker, 1, 1);
        grid.add(new Label("SSN"), 0, 2); grid.add(ssnField, 1, 2);
        grid.add(new Label("Street"), 0, 3); grid.add(streetField, 1, 3);
        grid.add(new Label("City"), 0, 4); grid.add(cityField, 1, 4);
        grid.add(new Label("Zip Code"), 0, 5); grid.add(zipField, 1, 5);
        grid.add(new Label("State"), 0, 6); grid.add(stateBox, 1, 6);
        grid.add(new Label("Contact"), 0, 7); grid.add(contactField, 1, 7);
        grid.add(new Label("Role *"), 0, 8); grid.add(roleBox, 1, 8);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e -> {
            String fullName = fullNameField.getText().trim();
            String role = roleBox.getEditor().getText().trim();
            if (fullName.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Full name is required.").showAndWait();
                e.consume();
                return;
            }
            if (role.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Role is required.").showAndWait();
                e.consume();
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) {
                return null;
            }
            try {
                Person p = new Person();
                p.setFullName(fullNameField.getText().trim());
                personRepo.save(p);
                return new NewPersonSelection(p, roleBox.getEditor().getText().trim());
            } catch (Exception ex) {
                showError(ex);
                return null;
            }
        });

        return dialog.showAndWait().orElse(null);
    }

    private List<String> getPersonRolesForDropdown() {
        try {
            List<String> roles = lookupRepo.getPersonRoles();
            if (roles != null && !roles.isEmpty()) {
                return roles;
            }
        } catch (Exception ignored) {
            // Fallback keeps dialog usable if lookup load fails.
        }
        return Arrays.asList(
                "Victim", "Suspect", "Witness", "Owner", "Reporting Party",
                "Complainant", "Driver", "Passenger", "Other");
    }

    private record NewPersonSelection(Person person, String role) {}

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
