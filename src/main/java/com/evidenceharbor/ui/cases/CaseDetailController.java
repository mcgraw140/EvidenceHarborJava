package com.evidenceharbor.ui.cases;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.*;
import com.evidenceharbor.persistence.*;
import com.evidenceharbor.util.TableExportUtil;
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
    @FXML private VBox caseContent;

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

    @FXML private TableView<Evidence> vehicleTable;
    @FXML private TableColumn<Evidence, String> colVehicleYear;
    @FXML private TableColumn<Evidence, String> colVehicleMake;
    @FXML private TableColumn<Evidence, String> colVehicleModel;
    @FXML private TableColumn<Evidence, String> colVehiclePlate;
    @FXML private TableColumn<Evidence, String> colVehicleVin;
    @FXML private TableColumn<Evidence, String> colVehicleStatus;

    private Case currentCase;
    private final CaseRepository caseRepo = new CaseRepository();
    private final ChargeRepository chargeRepo = new ChargeRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final PersonRepository personRepo = new PersonRepository();
    private final LookupRepository lookupRepo = new LookupRepository();

    @Override
    @SuppressWarnings("unchecked")
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

        colVehicleYear.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getVehicleYear())));
        colVehicleMake.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getVehicleMake())));
        colVehicleModel.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getVehicleModel())));
        colVehiclePlate.setCellValueFactory(cd -> {
            Evidence ev = cd.getValue();
            String s = nvl(ev.getVehicleLicensePlate());
            String st = nvl(ev.getVehicleLicenseState());
            return new SimpleStringProperty(st.isEmpty() ? s : (s + " " + st).trim());
        });
        colVehicleVin.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getVehicleVin())));
        colVehicleStatus.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getStatus())));

        vehicleTable.setRowFactory(tv -> {
            TableRow<Evidence> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && e.getClickCount() == 2 && !row.isEmpty()) {
                    if (com.evidenceharbor.ui.inventory.VehicleDetailsDialog.show(row.getItem())) refresh();
                }
            });
            return row;
        });

        // Double-click any evidence row to open the full detail sheet (barcode + CoC).
        evidenceTable.setRowFactory(tv -> {
            TableRow<Evidence> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && e.getClickCount() == 2 && !row.isEmpty()) {
                    openEvidenceDetail(row.getItem());
                }
            });
            return row;
        });

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
            List<Evidence> caseItems = evidenceRepo.findByCase(currentCase.getId());
            List<Evidence> nonVehicles = new ArrayList<>();
            List<Evidence> vehicles = new ArrayList<>();
            for (Evidence ev : caseItems) {
                if ("Vehicle".equalsIgnoreCase(ev.getEvidenceType())) vehicles.add(ev);
                else nonVehicles.add(ev);
            }
            evidenceTable.setItems(FXCollections.observableArrayList(nonVehicles));
            vehicleTable.setItems(FXCollections.observableArrayList(vehicles));
        } catch (Exception e) { showError(e); }

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
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onAddEvidence()   { Navigator.get().showAddEvidence(currentCase); }

    @FXML
    private void onImpoundVehicle() {
        Dialog<Evidence> dialog = new Dialog<>();
        dialog.setTitle("Impound Vehicle - Case #" + currentCase.getCaseNumber());
        dialog.setHeaderText("Enter vehicle information");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ComboBox<String> bodyTypeBox = new ComboBox<>();
        bodyTypeBox.setEditable(true);
        bodyTypeBox.setPromptText("Select vehicle type...");
        try { bodyTypeBox.getItems().setAll(lookupRepo.getVehicleTypes()); } catch (Exception ignored) {}
        bodyTypeBox.setMaxWidth(Double.MAX_VALUE);

        TextField yearField   = new TextField(); yearField.setPromptText("YYYY");
        TextField makeField   = new TextField(); makeField.setPromptText("e.g. Ford");
        TextField modelField  = new TextField(); modelField.setPromptText("e.g. F-150");
        TextField colorField  = new TextField();
        TextField vinField    = new TextField();
        TextField plateField  = new TextField();
        TextField stateField  = new TextField(); stateField.setPromptText("e.g. TX");
        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.setEditable(true);
        locationBox.setPromptText("Select impound location...");
        try { locationBox.getItems().setAll(lookupRepo.getImpoundLocations()); } catch (Exception ignored) {}
        locationBox.setMaxWidth(Double.MAX_VALUE);
        CheckBox stolenBox    = new CheckBox("Reported Stolen");
        TextArea descField    = new TextArea(); descField.setPromptText("Notes / description");
        descField.setPrefRowCount(3);

        int r = 0;
        grid.add(new Label("Type *"), 0, r);  grid.add(bodyTypeBox, 1, r++);
        grid.add(new Label("Year"), 0, r);    grid.add(yearField, 1, r++);
        grid.add(new Label("Make *"), 0, r);  grid.add(makeField, 1, r++);
        grid.add(new Label("Model"), 0, r);   grid.add(modelField, 1, r++);
        grid.add(new Label("Color"), 0, r);   grid.add(colorField, 1, r++);
        grid.add(new Label("VIN"), 0, r);     grid.add(vinField, 1, r++);
        grid.add(new Label("Plate"), 0, r);   grid.add(plateField, 1, r++);
        grid.add(new Label("State"), 0, r);   grid.add(stateField, 1, r++);
        grid.add(new Label("Location *"), 0, r);grid.add(locationBox, 1, r++);
        grid.add(stolenBox, 1, r++);
        grid.add(new Label("Notes"), 0, r);   grid.add(descField, 1, r++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e -> {
            if (makeField.getText() == null || makeField.getText().trim().isEmpty()) {
                warn("Make is required.", "Enter the vehicle make.");
                e.consume();
                return;
            }
            String location = locationBox.getEditor().getText();
            if (location == null) location = locationBox.getValue();
            if (location == null || location.trim().isEmpty()) {
                warn("Location is required.", "Select or enter a storage location.");
                e.consume();
                return;
            }
            String loc = location.trim();
            try {
                Evidence ev = new Evidence();
                ev.setCaseId(currentCase.getId());
                ev.setEvidenceType("Vehicle");
                ev.setStatus("In Custody");
                ev.setStorageLocation(loc);
                ev.setCollectionDate(LocalDate.now().toString());
                ev.setDescription(descField.getText().trim());
                ev.setVehicleBodyType(bodyTypeBox.getValue());
                ev.setVehicleYear(yearField.getText().trim());
                ev.setVehicleMake(makeField.getText().trim());
                ev.setVehicleModel(modelField.getText().trim());
                ev.setVehicleColor(colorField.getText().trim());
                ev.setVehicleVin(vinField.getText().trim());
                ev.setVehicleLicensePlate(plateField.getText().trim());
                ev.setVehicleLicenseState(stateField.getText().trim());
                ev.setVehicleReportedStolen(stolenBox.isSelected());
                ev.setVehicleImpounded(true);
                evidenceRepo.save(ev);

                // Chain of custody: Impound
                com.evidenceharbor.domain.ChainOfCustody coc = new com.evidenceharbor.domain.ChainOfCustody();
                coc.setEvidenceId(ev.getId());
                coc.setAction("Impound");
                com.evidenceharbor.domain.Officer o = com.evidenceharbor.app.SessionManager.getCurrentOfficer();
                String actor = o == null ? "" : o.getName();
                coc.setPerformedBy(actor);
                coc.setPerformedByName(actor);
                coc.setFromLocation("");
                coc.setToLocation(loc);
                coc.setNotes("Vehicle impounded");
                new com.evidenceharbor.persistence.ChainOfCustodyRepository().addEntry(coc);

                refresh();
            } catch (Exception ex) { showError(ex); e.consume(); }
        });
        dialog.showAndWait();
    }

    @FXML private void onPrint() {
        javafx.stage.Window w = caseContent != null && caseContent.getScene() != null
            ? caseContent.getScene().getWindow() : null;
        TableExportUtil.printNode(w, caseContent);
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
        // Load people up-front so we can fail fast if the DB call blows up.
        List<Person> people;
        try {
            people = personRepo.findAllRecent();
        } catch (Exception e) {
            showError(e);
            return;
        }
        final ObservableList<Person> allPeople = FXCollections.observableArrayList(people);
        final List<String> roles = getPersonRolesForDropdown();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Associate Person with Case #" + currentCase.getCaseNumber());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Left column: search + list of matching people
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name, DOB, SSN, or address\u2026");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button refreshBtn = new Button("\u21bb Refresh");
        refreshBtn.setTooltip(new Tooltip("Reload list (sorted by most recently created)"));

        ListView<Person> personList = new ListView<>();
        personList.setPrefHeight(260);
        personList.setPrefWidth(360);
        personList.setPlaceholder(new Label("No people match your search."));
        personList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Person p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                StringBuilder sb = new StringBuilder(p.getFullName() == null ? "(no name)" : p.getFullName());
                String dob = p.getDob();
                if (dob != null && !dob.isBlank()) sb.append("   \u2022   DOB ").append(dob);
                String city = p.getCity();
                String state = p.getState();
                if ((city != null && !city.isBlank()) || (state != null && !state.isBlank())) {
                    sb.append("   \u2022   ");
                    if (city != null && !city.isBlank()) sb.append(city);
                    if (state != null && !state.isBlank()) {
                        if (city != null && !city.isBlank()) sb.append(", ");
                        sb.append(state);
                    }
                }
                setText(sb.toString());
            }
        });

        FilteredList<Person> filtered = new FilteredList<>(allPeople, p -> true);
        personList.setItems(filtered);

        Label matchCount = new Label();
        Runnable updateCount = () ->
                matchCount.setText(filtered.size() + " of " + allPeople.size() + " people");
        updateCount.run();
        filtered.addListener((javafx.collections.ListChangeListener<Person>) c -> updateCount.run());

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            final String q = newV == null ? "" : newV.trim().toLowerCase();
            if (q.isEmpty()) {
                filtered.setPredicate(p -> true);
                return;
            }
            filtered.setPredicate(p -> {
                if (p == null) return false;
                if (p.getFullName() != null && p.getFullName().toLowerCase().contains(q)) return true;
                if (p.getDob()      != null && p.getDob().toLowerCase().contains(q))      return true;
                if (p.getSsn()      != null && p.getSsn().toLowerCase().contains(q))      return true;
                if (p.getStreet()   != null && p.getStreet().toLowerCase().contains(q))   return true;
                if (p.getCity()     != null && p.getCity().toLowerCase().contains(q))     return true;
                if (p.getState()    != null && p.getState().toLowerCase().contains(q))    return true;
                if (p.getZip()      != null && p.getZip().toLowerCase().contains(q))      return true;
                if (p.getContact()  != null && p.getContact().toLowerCase().contains(q))  return true;
                return false;
            });
        });

        // Auto-select the first row when the filter narrows to something.
        filtered.addListener((javafx.collections.ListChangeListener<Person>) c -> {
            if (personList.getSelectionModel().getSelectedItem() == null && !filtered.isEmpty()) {
                personList.getSelectionModel().selectFirst();
            }
        });
        if (!filtered.isEmpty()) personList.getSelectionModel().selectFirst();

        // Right column: role + action buttons
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList(roles));
        roleBox.setPromptText("Select role\u2026");
        roleBox.setEditable(false);
        roleBox.setMaxWidth(Double.MAX_VALUE);

        Button createPersonBtn = new Button("+ Create New Person");
        createPersonBtn.setMaxWidth(Double.MAX_VALUE);
        createPersonBtn.setOnAction(evt -> {
            Person created = showCreatePersonDialog();
            if (created == null) return;
            // Reload the full list from the DB so search hits the fresh record even
            // if the name changed during save (e.g. trimming).
            try {
                List<Person> fresh = personRepo.findAllRecent();
                allPeople.setAll(fresh);
            } catch (Exception ex) {
                showError(ex);
                return;
            }
            // Clear the search box AND force the predicate to "accept all" so the
            // new row is visible regardless of what was previously typed.
            searchField.setText("");
            filtered.setPredicate(p -> true);
            // Re-select the newly created person by id.
            Person match = null;
            for (Person p : allPeople) {
                if (p.getId() == created.getId()) { match = p; break; }
            }
            if (match != null) {
                personList.getSelectionModel().select(match);
                personList.scrollTo(match);
            }
        });

        // Manual refresh — reloads the list (newest first) without needing to create a new person.
        refreshBtn.setOnAction(evt -> {
            Person keep = personList.getSelectionModel().getSelectedItem();
            try {
                allPeople.setAll(personRepo.findAllRecent());
            } catch (Exception ex) {
                showError(ex);
                return;
            }
            searchField.setText("");
            filtered.setPredicate(p -> true);
            if (keep != null) {
                for (Person p : allPeople) {
                    if (p.getId() == keep.getId()) {
                        personList.getSelectionModel().select(p);
                        personList.scrollTo(p);
                        break;
                    }
                }
            } else if (!filtered.isEmpty()) {
                personList.getSelectionModel().selectFirst();
            }
        });

        Label selectedLabel = new Label("No person selected");
        selectedLabel.setWrapText(true);
        selectedLabel.setMaxWidth(260);
        personList.getSelectionModel().selectedItemProperty().addListener((obs, oldP, newP) -> {
            if (newP == null) {
                selectedLabel.setText("No person selected");
            } else {
                selectedLabel.setText("Selected: " + (newP.getFullName() == null ? "(no name)" : newP.getFullName()));
            }
        });

        VBox leftCol = new VBox(6,
                new Label("Search people:"),
                new HBox(6, searchField, refreshBtn),
                personList, matchCount);
        VBox rightCol = new VBox(10,
                new Label("Selected person:"), selectedLabel,
                new Separator(),
                new Label("Role:"), roleBox,
                createPersonBtn);
        VBox.setVgrow(personList, Priority.ALWAYS);
        rightCol.setPrefWidth(280);

        HBox root = new HBox(16, leftCol, rightCol);
        root.setPadding(new Insets(16));
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(720);

        // OK handler: validate then save
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        // Require both a selected person and a chosen role before OK is enabled.
        ok.disableProperty().bind(
                personList.getSelectionModel().selectedItemProperty().isNull()
                        .or(roleBox.valueProperty().isNull())
                        .or(roleBox.valueProperty().isEqualTo("")));
        ok.setOnAction(e -> {
            Person p = personList.getSelectionModel().getSelectedItem();
            if (p == null) {
                warn("Select a person", "Choose a person from the list or click \"+ Create New Person\".");
                e.consume();
                return;
            }
            String role = roleBox.getValue();
            if (role == null || role.isBlank()) {
                warn("Role is required", "Choose a role for this person.");
                e.consume();
                return;
            }
            try {
                caseRepo.associatePerson(currentCase.getId(), p.getId(), role.trim());
                refresh();
            } catch (Exception ex) {
                showError(ex);
                e.consume();
            }
        });

        javafx.application.Platform.runLater(searchField::requestFocus);
        dialog.showAndWait();
    }

    private Person showCreatePersonDialog() {
        Dialog<Person> dialog = new Dialog<>();
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

        grid.add(new Label("Full Name"), 0, 0); grid.add(fullNameField, 1, 0);
        grid.add(new Label("Date of Birth"), 0, 1); grid.add(dobPicker, 1, 1);
        grid.add(new Label("SSN"), 0, 2); grid.add(ssnField, 1, 2);
        grid.add(new Label("Street"), 0, 3); grid.add(streetField, 1, 3);
        grid.add(new Label("City"), 0, 4); grid.add(cityField, 1, 4);
        grid.add(new Label("Zip Code"), 0, 5); grid.add(zipField, 1, 5);
        grid.add(new Label("State"), 0, 6); grid.add(stateBox, 1, 6);
        grid.add(new Label("Contact"), 0, 7); grid.add(contactField, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());

        // Mutable holder so we can save inside onAction (keeps dialog open on error)
        Person[] savedHolder = new Person[1];
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e -> {
            String fullName = fullNameField.getText().trim();
            if (fullName.isEmpty()) {
                warn("Full name is required.", "Enter the person's full name.");
                e.consume();
                return;
            }
            // Pre-check SSN uniqueness so the user gets a friendly warning (dialog
            // stays open, all typed fields preserved) instead of a DB error.
            String ssnTyped = ssnField.getText() == null ? "" : ssnField.getText().trim();
            if (!ssnTyped.isEmpty()) {
                try {
                    Person dup = personRepo.findBySsn(ssnTyped);
                    if (dup != null) {
                        warn("Duplicate SSN",
                                "SSN " + ssnTyped + " is already on file for \""
                                        + (dup.getFullName() == null ? "(no name)" : dup.getFullName())
                                        + "\". Update the SSN to save this new person, or cancel and associate the existing record.");
                        ssnField.requestFocus();
                        ssnField.selectAll();
                        e.consume();
                        return;
                    }
                } catch (Exception ex) {
                    showError(ex);
                    e.consume();
                    return;
                }
            }
            try {
                Person p = new Person();
                p.setFullName(fullName);
                if (dobPicker.getValue() != null) {
                    p.setDob(dobPicker.getValue().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                }
                p.setSsn(ssnField.getText());
                p.setStreet(streetField.getText());
                p.setCity(cityField.getText());
                p.setState(stateBox.getValue());
                p.setZip(zipField.getText());
                p.setContact(contactField.getText());
                personRepo.save(p);
                savedHolder[0] = p;
            } catch (Exception ex) {
                showError(ex);
                e.consume(); // keep dialog open on DB error (e.g. duplicate SSN)
            }
        });

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? savedHolder[0] : null);
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

    private <T> void makeSearchableComboBox(ComboBox<T> comboBox, List<T> sourceItems, Function<T, String> labelMapper) {
        @SuppressWarnings("unchecked")
        ObservableList<T> originalItems = (sourceItems instanceof ObservableList)
                ? (ObservableList<T>) sourceItems
                : FXCollections.observableArrayList(sourceItems);
        FilteredList<T> filteredItems = new FilteredList<>(originalItems, item -> true);

        // Guard flag so we can tell user-typing apart from programmatic text updates
        // (selection, focus-loss re-sync, etc.) and skip re-showing the popup.
        final boolean[] programmaticTextUpdate = { false };

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
            if (programmaticTextUpdate[0]) return; // don't filter / re-show on programmatic updates
            String query = newText == null ? "" : newText.trim().toLowerCase();
            filteredItems.setPredicate(item -> labelMapper.apply(item).toLowerCase().contains(query));
            if (!comboBox.isShowing() && comboBox.isFocused()) {
                comboBox.show();
            }
        });

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            // When the user picks an item from the popup we want the editor text to reflect it.
            // Don't blank the text on null — that would erase a selection after a filter-reset.
            if (newVal == null) return;
            programmaticTextUpdate[0] = true;
            try {
                String label = labelMapper.apply(newVal);
                comboBox.getEditor().setText(label);
                comboBox.getEditor().positionCaret(label.length());
            } finally {
                programmaticTextUpdate[0] = false;
            }
        });

        // When the popup hides (user picked or dismissed), clear the filter so
        // the full list is available next time the user focuses the combo.
        comboBox.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) {
                programmaticTextUpdate[0] = true;
                try {
                    filteredItems.setPredicate(item -> true);
                } finally {
                    programmaticTextUpdate[0] = false;
                }
            }
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) return;
            // On focus loss, try to resolve the typed text to an existing item.
            // If resolution fails, preserve the typed text instead of blanking it out.
            T current = comboBox.getValue();
            String typed = comboBox.getEditor().getText();
            programmaticTextUpdate[0] = true;
            try {
                if (current == null && typed != null && !typed.isBlank()) {
                    String t = typed.trim();
                    for (T item : originalItems) {
                        if (labelMapper.apply(item).equalsIgnoreCase(t)) {
                            comboBox.setValue(item);
                            current = item;
                            break;
                        }
                    }
                }
                if (current != null) {
                    comboBox.getEditor().setText(labelMapper.apply(current));
                }
                filteredItems.setPredicate(item -> true);
            } finally {
                programmaticTextUpdate[0] = false;
            }
        });
    }

    private void removePerson(CasePerson cp) {
        try { caseRepo.removePerson(cp.getId()); refresh(); }
        catch (Exception e) { showError(e); }
    }

    private void openEvidenceDetail(Evidence ev) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/EvidenceDetail.fxml"));
            javafx.scene.Parent root = loader.load();
            com.evidenceharbor.ui.inventory.EvidenceDetailController ctrl = loader.getController();
            ctrl.setEvidence(ev, currentCase == null ? "" : currentCase.getCaseNumber());

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.initOwner(evidenceTable.getScene().getWindow());
            dialog.setTitle("Evidence Detail \u2014 " + nvl(ev.getBarcode()));
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 860, 640);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            refresh(); // pick up any CoC / status updates
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        String detail = e.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = e.getClass().getSimpleName();
        }
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Something went wrong");
        a.setContentText(detail);
        applyDialogTheme(a);
        a.showAndWait();
    }

    private void warn(String header, String detail) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Missing Information");
        a.setHeaderText(header == null || header.isBlank() ? "Missing information" : header);
        a.setContentText(detail == null || detail.isBlank()
                ? (header == null || header.isBlank() ? "Please fill in the required fields." : header)
                : detail);
        applyDialogTheme(a);
        a.showAndWait();
    }

    private void applyDialogTheme(Alert a) {
        try {
            a.getDialogPane().getStylesheets().add(
                    getClass().getResource("/styles/theme.css").toExternalForm());
        } catch (Exception ignore) {}
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
