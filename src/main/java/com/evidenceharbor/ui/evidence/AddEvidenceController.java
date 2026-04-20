package com.evidenceharbor.ui.evidence;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.app.CurrentOfficerResolver;
import com.evidenceharbor.domain.*;
import com.evidenceharbor.persistence.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AddEvidenceController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navDropboxBtn;

    @FXML private Label breadcrumbCase;
    @FXML private Label caseNumberLabel;
    @FXML private DatePicker collectionDate;
    @FXML private ComboBox<Officer> collectedByCombo;

    @FXML private CheckBox collectedFromPersonCheck;
    @FXML private VBox collectedFromPersonBox;
    @FXML private ComboBox<Person> collectedFromPersonCombo;

    @FXML private TextField specificLocationField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField zipField;

    @FXML private ComboBox<String> evidenceTypeCombo;
    @FXML private TextField descriptionField;
    @FXML private VBox dynamicPanel;

    @FXML private TextField storageLocationField;
    @FXML private ComboBox<String> statusCombo;

    private Case currentCase;
    private final OfficerRepository officerRepo = new OfficerRepository();
    private final PersonRepository personRepo = new PersonRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final LookupRepository lookupRepo = new LookupRepository();
    private List<String> storageLocations = List.of();

    // Dynamic field references
    // Ammunition
    private ComboBox<String> caliberCombo;
    private TextField quantityField;

    // Biological/DNA
    private ComboBox<String> bioSourceCombo;

    // Currency
    private TextField currencyAmountField;

    // Electronics
    private ComboBox<String> electronicTypeCombo;
    private TextField electronicMakeField;
    private TextField electronicModelField;
    private TextField electronicSerialField;
    private TextField electronicUsernameField;
    private TextField electronicPasswordField;

    // Firearms
    private TextField firearmMakeField;
    private TextField firearmModelField;
    private TextField firearmSerialField;
    private ComboBox<String> firearmCaliberCombo;

    // Jewelry
    private TextField jewelryMakeField;
    private TextField jewelryModelField;
    private TextField jewelrySerialField;

    // Narcotic Equipment
    private TextField narcEquipMakeField;
    private TextField narcEquipModelField;
    private TextField narcEquipSerialField;

    // Narcotics
    private ComboBox<String> narcoticDrugTypeCombo;
    private ComboBox<String> narcoticUnitTypeCombo;
    private TextField narcoticUnitNumberField;

    // Vehicles
    private TextField vehicleMakeField;
    private TextField vehicleModelField;
    private TextField vehicleYearField;
    private TextField vehicleLicensePlateField;
    private TextField vehicleVinField;

    // Weapons
    private ComboBox<String> weaponTypeCombo;
    private TextField weaponMakeField;
    private TextField weaponModelField;
    private TextField weaponSerialField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            evidenceTypeCombo.setItems(FXCollections.observableArrayList(lookupRepo.getEvidenceTypes()));
            statusCombo.setItems(FXCollections.observableArrayList(lookupRepo.getEvidenceStatuses()));
            if (statusCombo.getItems().contains("In Dropbox")) {
                statusCombo.setValue("In Dropbox");
            } else if (!statusCombo.getItems().isEmpty()) {
                statusCombo.setValue(statusCombo.getItems().get(0));
            }
            statusCombo.setDisable(true);

            storageLocations = lookupRepo.getStorageLocations();
        } catch (Exception e) {
            showError(e);
            storageLocations = List.of();
        }

        collectionDate.setValue(LocalDate.now());
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);
    }

    public void initForCase(Case c) {
        this.currentCase = c;
        breadcrumbCase.setText("Case #" + c.getCaseNumber());
        caseNumberLabel.setText("Case #" + c.getCaseNumber());
        try {
            collectedByCombo.setItems(FXCollections.observableArrayList(officerRepo.findAll()));
            collectedFromPersonCombo.setItems(FXCollections.observableArrayList(personRepo.findAll()));

            Officer defaultOfficer = CurrentOfficerResolver.resolveDefaultOfficer(officerRepo);
            if (defaultOfficer != null) {
                collectedByCombo.getItems().stream()
                        .filter(o -> o.getId() == defaultOfficer.getId())
                        .findFirst()
                        .ifPresent(o -> collectedByCombo.getSelectionModel().select(o));
            }
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void onCollectedFromPersonToggle() {
        boolean show = collectedFromPersonCheck.isSelected();
        collectedFromPersonBox.setVisible(show);
        collectedFromPersonBox.setManaged(show);
    }

    @FXML
    private void onEvidenceTypeChanged() {
        dynamicPanel.getChildren().clear();
        String type = evidenceTypeCombo.getValue();
        if (type == null) return;

        statusCombo.setValue("In Dropbox");
        storageLocationField.setPromptText("Select an approved dropbox location");

        switch (type) {
            case "Ammunition"         -> buildAmmunitionPanel();
            case "Biological / DNA"   -> buildBiologicalPanel();
            case "Currency"           -> buildCurrencyPanel();
            case "Electronics"        -> buildElectronicsPanel();
            case "Firearm"            -> buildFirearmPanel();
            case "Jewelry"            -> buildJewelryPanel();
            case "Narcotic Equipment" -> buildNarcEquipPanel();
            case "Narcotics"          -> buildNarcoticsPanel();
            case "Weapon"             -> buildWeaponPanel();
        }
    }

    // ──────────────────────────────── PANEL BUILDERS ────────────────────────────────

    private void buildAmmunitionPanel() {
        GridPane g = grid();
        caliberCombo = comboField();
        quantityField = textField("e.g. 50");
        try { caliberCombo.getItems().addAll(lookupRepo.getCalibers()); } catch (Exception e) { showError(e); }
        addRow(g, 0, "Caliber", caliberCombo, "Number of Rounds", quantityField);
        dynamicPanel.getChildren().add(g);
    }

    private void buildBiologicalPanel() {
        GridPane g = grid();
        bioSourceCombo = comboField();
        try { bioSourceCombo.getItems().addAll(lookupRepo.getBiologicalSources()); } catch (Exception e) { showError(e); }
        addRow(g, 0, "Source", bioSourceCombo, null, null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildCurrencyPanel() {
        GridPane g = grid();
        currencyAmountField = textField("0.00");
        addRow(g, 0, "Amount ($)", currencyAmountField, null, null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildElectronicsPanel() {
        GridPane g = grid();
        electronicTypeCombo = comboField();
        electronicMakeField = textField("e.g. Apple, Samsung");
        electronicModelField = textField();
        electronicSerialField = textField();
        electronicUsernameField = textField();
        electronicPasswordField = textField();
        try { electronicTypeCombo.getItems().addAll(lookupRepo.getElectronicTypes()); } catch (Exception e) { showError(e); }

        addRow(g, 0, "Device Type",     electronicTypeCombo,    "Make",            electronicMakeField);
        addRow(g, 1, "Model",           electronicModelField,   "Serial #",        electronicSerialField);
        addRow(g, 2, "Device Username", electronicUsernameField, "Device Password", electronicPasswordField);
        dynamicPanel.getChildren().add(g);
    }

    private void buildFirearmPanel() {
        GridPane g = grid();
        firearmMakeField   = textField("e.g. Glock, Smith & Wesson");
        firearmModelField  = textField();
        firearmSerialField = textField();
        firearmCaliberCombo = comboField();
        try { firearmCaliberCombo.getItems().addAll(lookupRepo.getCalibers()); } catch (Exception e) { showError(e); }

        addRow(g, 0, "Make",     firearmMakeField,    "Model",    firearmModelField);
        addRow(g, 1, "Serial #", firearmSerialField,  "Caliber",  firearmCaliberCombo);
        dynamicPanel.getChildren().add(g);
    }

    private void buildJewelryPanel() {
        GridPane g = grid();
        jewelryMakeField  = textField();
        jewelryModelField = textField();
        jewelrySerialField = textField();

        addRow(g, 0, "Make",     jewelryMakeField,  "Model",    jewelryModelField);
        addRow(g, 1, "Serial #", jewelrySerialField, null,       null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildNarcEquipPanel() {
        GridPane g = grid();
        narcEquipMakeField  = textField();
        narcEquipModelField = textField();
        narcEquipSerialField = textField();

        addRow(g, 0, "Make",     narcEquipMakeField,  "Model",    narcEquipModelField);
        addRow(g, 1, "Serial #", narcEquipSerialField, null,       null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildNarcoticsPanel() {
        GridPane g = grid();
        narcoticDrugTypeCombo = comboField();
        narcoticUnitTypeCombo = comboField();
        narcoticUnitNumberField = textField();
        try { narcoticDrugTypeCombo.getItems().addAll(lookupRepo.getNarcoticsTypes()); } catch (Exception e) { showError(e); }
        try { narcoticUnitTypeCombo.getItems().addAll(lookupRepo.getNarcoticsUnitTypes()); } catch (Exception e) { showError(e); }

        addRow(g, 0, "Narcotics Type", narcoticDrugTypeCombo, "Unit Type",   narcoticUnitTypeCombo);
        addRow(g, 1, "Unit Number",    narcoticUnitNumberField, null,          null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildVehiclePanel() {
        GridPane g = grid();
        vehicleMakeField         = textField("e.g. Ford, Honda");
        vehicleModelField        = textField();
        vehicleYearField         = textField("YYYY");
        vehicleLicensePlateField = textField();
        vehicleVinField          = textField();

        addRow(g, 0, "Make",          vehicleMakeField,         "Model",         vehicleModelField);
        addRow(g, 1, "Year",          vehicleYearField,         "License Plate", vehicleLicensePlateField);
        addRow(g, 2, "VIN",           vehicleVinField,          null,            null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildWeaponPanel() {
        GridPane g = grid();
        weaponTypeCombo  = comboField();
        weaponMakeField  = textField();
        weaponModelField = textField();
        weaponSerialField = textField();
        try { weaponTypeCombo.getItems().addAll(lookupRepo.getWeaponTypes()); } catch (Exception e) { showError(e); }

        addRow(g, 0, "Weapon Type", weaponTypeCombo,  "Make",     weaponMakeField);
        addRow(g, 1, "Model",       weaponModelField, "Serial #", weaponSerialField);
        dynamicPanel.getChildren().add(g);
    }

    // ──────────────────────────────── SAVE ────────────────────────────────

    @FXML
    private void onSave() {
        String type = evidenceTypeCombo.getValue();
        if (type == null) { new Alert(Alert.AlertType.WARNING, "Please select an evidence type.").showAndWait(); return; }
        if ("Vehicle".equals(type)) {
            new Alert(Alert.AlertType.WARNING,
                    "Vehicles must be entered from the separate Impound Vehicle screen.").showAndWait();
            return;
        }
        String storageLocation = storageLocationField.getText() == null ? "" : storageLocationField.getText().trim();
        if (storageLocation.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Storage location is required.").showAndWait();
            return;
        }

        if (!isApprovedDropboxLocation(storageLocation)) {
            String approved = getApprovedDropboxLocations().isEmpty()
                    ? "No approved dropbox locations are configured in Storage Locations."
                    : "Approved dropbox locations: " + String.join(", ", getApprovedDropboxLocations());
            new Alert(Alert.AlertType.WARNING,
                    "Officers can only intake evidence into approved dropbox locations from this screen.\n\n" + approved)
                    .showAndWait();
            return;
        }

        Evidence ev = new Evidence();
        ev.setCaseId(currentCase.getId());
        ev.setEvidenceType(type);
        ev.setDescription(descriptionField.getText().trim());
        ev.setStorageLocation(storageLocation);
        ev.setStatus("In Dropbox");

        LocalDate date = collectionDate.getValue();
        ev.setCollectionDate(date != null ? date.toString() : LocalDate.now().toString());

        Officer off = collectedByCombo.getValue();
        if (off != null) ev.setCollectedByOfficerId(off.getId());

        if (collectedFromPersonCheck.isSelected()) {
            Person p = collectedFromPersonCombo.getValue();
            if (p != null) ev.setCollectedFromPersonId(p.getId());
        }
        ev.setSpecificLocation(specificLocationField.getText());
        ev.setAddress(addressField.getText());
        ev.setCity(cityField.getText());
        ev.setState(stateField.getText());
        ev.setZip(zipField.getText());

        populateTypeFields(ev, type);

        try {
            evidenceRepo.save(ev);
            Navigator.get().showCaseDetail(currentCase);
        } catch (Exception e) { showError(e); }
    }

    private void populateTypeFields(Evidence ev, String type) {
        switch (type) {
            case "Ammunition" -> {
                if (caliberCombo != null) ev.setAmmoCallber(caliberCombo.getValue());
                if (quantityField != null) ev.setAmmoQuantity(quantityField.getText());
            }
            case "Biological / DNA" -> {
                if (bioSourceCombo != null) ev.setBioSampleType(bioSourceCombo.getValue());
            }
            case "Currency" -> {
                if (currencyAmountField != null) ev.setCurrencyAmount(currencyAmountField.getText());
            }
            case "Electronics" -> {
                if (electronicTypeCombo != null) ev.setElecDeviceType(electronicTypeCombo.getValue());
                if (electronicMakeField != null) ev.setElecMake(electronicMakeField.getText());
                if (electronicModelField != null) ev.setElecModel(electronicModelField.getText());
                if (electronicSerialField != null) ev.setElecSerialNumber(electronicSerialField.getText());
                if (electronicUsernameField != null) ev.setElecDeviceUsername(electronicUsernameField.getText());
                if (electronicPasswordField != null) ev.setElecDevicePassword(electronicPasswordField.getText());
            }
            case "Firearm" -> {
                if (firearmMakeField != null) ev.setFirearmMake(firearmMakeField.getText());
                if (firearmModelField != null) ev.setFirearmModel(firearmModelField.getText());
                if (firearmSerialField != null) ev.setFirearmSerialNumber(firearmSerialField.getText());
                if (firearmCaliberCombo != null) ev.setFirearmCaliber(firearmCaliberCombo.getValue());
            }
            case "Jewelry" -> {
                if (jewelryMakeField != null) ev.setJewelryMaterial(jewelryMakeField.getText());
                if (jewelryModelField != null) ev.setJewelryType(jewelryModelField.getText());
                if (jewelrySerialField != null) ev.setJewelryEngravingOrId(jewelrySerialField.getText());
            }
            case "Narcotic Equipment" -> {
                if (narcEquipMakeField != null) ev.setNarcEquipType(narcEquipMakeField.getText());
                if (narcEquipModelField != null) ev.setNarcEquipDescription(narcEquipModelField.getText());
                if (narcEquipSerialField != null) ev.setNarcEquipSuspectedResidue(narcEquipSerialField.getText());
            }
            case "Narcotics" -> {
                if (narcoticDrugTypeCombo != null) ev.setNarcDrugType(narcoticDrugTypeCombo.getValue());
                if (narcoticUnitTypeCombo != null) ev.setNarcForm(narcoticUnitTypeCombo.getValue());
                if (narcoticUnitNumberField != null) ev.setNarcNetWeight(narcoticUnitNumberField.getText());
            }
            case "Vehicle" -> {
                if (vehicleMakeField != null) ev.setVehicleMake(vehicleMakeField.getText());
                if (vehicleModelField != null) ev.setVehicleModel(vehicleModelField.getText());
                if (vehicleYearField != null) ev.setVehicleYear(vehicleYearField.getText());
                if (vehicleLicensePlateField != null) ev.setVehicleLicensePlate(vehicleLicensePlateField.getText());
                if (vehicleVinField != null) ev.setVehicleVin(vehicleVinField.getText());
            }
            case "Weapon" -> {
                if (weaponTypeCombo != null) ev.setWeaponType(weaponTypeCombo.getValue());
                if (weaponMakeField != null) ev.setWeaponMake(weaponMakeField.getText());
                if (weaponModelField != null) ev.setWeaponModel(weaponModelField.getText());
                if (weaponSerialField != null) ev.setWeaponSerialNumber(weaponSerialField.getText());
            }
        }
    }

    @FXML private void onBack() { Navigator.get().showCaseDetail(currentCase); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }
    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onQuartermaster() { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    // ──────────────────────────────── HELPERS ────────────────────────────────

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(24); g.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    private TextField textField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field-default");
        return tf;
    }
    private TextField textField() { return textField(""); }

    private ComboBox<String> comboField(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.getStyleClass().add("combo-box-default");
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    private CheckBox checkField(String text) {
        CheckBox cb = new CheckBox(text);
        cb.getStyleClass().add("check-box-default");
        return cb;
    }

    private void addRow(GridPane g, int row, String lbl1, javafx.scene.Node ctrl1, String lbl2, javafx.scene.Node ctrl2) {
        VBox col1 = new VBox(4);
        Label l1 = new Label(lbl1); l1.getStyleClass().add("field-label");
        col1.getChildren().addAll(l1, ctrl1);
        g.add(col1, 0, row);
        if (lbl2 != null && ctrl2 != null) {
            VBox col2 = new VBox(4);
            Label l2 = new Label(lbl2); l2.getStyleClass().add("field-label");
            col2.getChildren().addAll(l2, ctrl2);
            g.add(col2, 1, row);
        }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
    }

    private List<String> getApprovedDropboxLocations() {
        return storageLocations.stream()
                .filter(this::looksLikeDropboxLocation)
                .collect(Collectors.toList());
    }

    private boolean isApprovedDropboxLocation(String location) {
        String l = location.trim();
        return getApprovedDropboxLocations().stream().anyMatch(s -> s.equalsIgnoreCase(l));
    }

    private boolean looksLikeDropboxLocation(String location) {
        if (location == null) return false;
        String v = location.toLowerCase();
        return v.contains("dropbox") || v.contains("drop box") || v.contains("locker");
    }
}
