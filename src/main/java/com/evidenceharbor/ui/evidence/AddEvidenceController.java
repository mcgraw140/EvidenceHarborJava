package com.evidenceharbor.ui.evidence;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.app.CurrentOfficerResolver;
import com.evidenceharbor.domain.*;
import com.evidenceharbor.persistence.*;
import com.evidenceharbor.util.Dialogs;
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

    @FXML private Label formTitle;
    @FXML private Button saveButton;

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

    @FXML private ComboBox<String> storageLocationCombo;
    @FXML private ComboBox<String> statusCombo;

    private Case currentCase;
    private final OfficerRepository officerRepo = new OfficerRepository();
    @SuppressWarnings("unused")
    private final PersonRepository personRepo = new PersonRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final LookupRepository lookupRepo = new LookupRepository();
    /** Approved dropbox locations come from the intake_locations lookup. */
    private List<String> dropboxLocations = List.of();
    private Evidence editEvidence;

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
    private CheckBox firearmReportedStolenCheck;
    private CheckBox firearmLoadedWhenRecoveredCheck;

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

            dropboxLocations = lookupRepo.getIntakeLocations();
            storageLocationCombo.setItems(FXCollections.observableArrayList(dropboxLocations));
            if (!dropboxLocations.isEmpty()) {
                storageLocationCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            showError(e);
            dropboxLocations = List.of();
        }

        collectionDate.setValue(LocalDate.now());
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);
    }

    public void initForCase(Case c) {
        this.editEvidence = null;
        this.currentCase = c;
        breadcrumbCase.setText("Case #" + c.getCaseNumber());
        caseNumberLabel.setText("Case #" + c.getCaseNumber());
        try {
            collectedByCombo.setItems(FXCollections.observableArrayList(officerRepo.findAll()));

            // Only people already associated with this case may be selected as the
            // "collected from" person — associate them on the Case page first.
            List<Person> casePersons = (c.getPersons() == null) ? List.of()
                    : c.getPersons().stream()
                            .map(cp -> cp.getPerson())
                            .filter(p -> p != null)
                            .distinct()
                            .collect(Collectors.toList());
            collectedFromPersonCombo.setItems(FXCollections.observableArrayList(casePersons));
            if (casePersons.isEmpty()) {
                collectedFromPersonCombo.setPromptText("No persons associated with this case");
                collectedFromPersonCombo.setDisable(true);
            }

            Officer defaultOfficer = CurrentOfficerResolver.resolveDefaultOfficer(officerRepo);
            if (defaultOfficer != null) {
                collectedByCombo.getItems().stream()
                        .filter(o -> o.getId() == defaultOfficer.getId())
                        .findFirst()
                        .ifPresent(o -> collectedByCombo.getSelectionModel().select(o));
            }
        } catch (Exception e) { showError(e); }
    }

    public void initForEdit(Case c, Evidence existing) {
        this.currentCase = c;
        this.editEvidence = existing;
        breadcrumbCase.setText("Case #" + c.getCaseNumber());
        caseNumberLabel.setText("Case #" + c.getCaseNumber());
        updateFormMode(true);

        try {
            collectedByCombo.setItems(FXCollections.observableArrayList(officerRepo.findAll()));

            List<Person> casePersons = (c.getPersons() == null) ? List.of()
                    : c.getPersons().stream()
                            .map(cp -> cp.getPerson())
                            .filter(p -> p != null)
                            .distinct()
                            .collect(Collectors.toList());
            collectedFromPersonCombo.setItems(FXCollections.observableArrayList(casePersons));
            if (casePersons.isEmpty()) {
                collectedFromPersonCombo.setPromptText("No persons associated with this case");
                collectedFromPersonCombo.setDisable(true);
            }

            fillFromEvidence(existing);
        } catch (Exception e) { showError(e); }
    }

    private void updateFormMode(boolean editMode) {
        if (formTitle != null) {
            formTitle.setText(editMode ? "Edit Evidence" : "Add Evidence");
        }
        if (saveButton != null) {
            saveButton.setText(editMode ? "Save Changes" : "Save Evidence");
        }
        if (statusCombo != null) {
            statusCombo.setDisable(!editMode);
            statusCombo.setEditable(editMode);
        }
        if (storageLocationCombo != null) {
            storageLocationCombo.setEditable(editMode);
        }
    }

    private void fillFromEvidence(Evidence ev) {
        if (ev == null) return;
        if (ev.getCollectionDate() != null && !ev.getCollectionDate().isBlank()) {
            try { collectionDate.setValue(LocalDate.parse(ev.getCollectionDate())); }
            catch (Exception ignored) { collectionDate.setValue(LocalDate.now()); }
        }
        if (ev.getCollectedByOfficerId() > 0) {
            try {
                Officer off = officerRepo.findById(ev.getCollectedByOfficerId());
                if (off != null) {
                    collectedByCombo.getItems().stream()
                            .filter(o -> o.getId() == off.getId())
                            .findFirst().ifPresent(collectedByCombo.getSelectionModel()::select);
                }
            } catch (Exception ignored) {}
        }
        if (ev.getCollectedFromPersonId() > 0) {
            collectedFromPersonCheck.setSelected(true);
            onCollectedFromPersonToggle();
            try {
                Person p = personRepo.findById(ev.getCollectedFromPersonId());
                if (p != null) {
                    collectedFromPersonCombo.getItems().stream()
                            .filter(x -> x.getId() == p.getId())
                            .findFirst().ifPresent(collectedFromPersonCombo.getSelectionModel()::select);
                }
            } catch (Exception ignored) {}
        } else {
            collectedFromPersonCheck.setSelected(false);
            onCollectedFromPersonToggle();
        }

        specificLocationField.setText(nz(ev.getSpecificLocation()));
        addressField.setText(nz(ev.getAddress()));
        cityField.setText(nz(ev.getCity()));
        stateField.setText(nz(ev.getState()));
        zipField.setText(nz(ev.getZip()));
        evidenceTypeCombo.setValue(nz(ev.getEvidenceType()));
        descriptionField.setText(nz(ev.getDescription()));
        statusCombo.setValue(nz(ev.getStatus()));
        storageLocationCombo.setValue(nz(ev.getStorageLocation()));

        onEvidenceTypeChanged();

        switch (nz(ev.getEvidenceType())) {
            case "Ammunition" -> {
                if (caliberCombo != null) caliberCombo.setValue(nz(ev.getAmmoCallber()));
                if (quantityField != null) quantityField.setText(nz(ev.getAmmoQuantity()));
            }
            case "Biological / DNA" -> {
                if (bioSourceCombo != null) bioSourceCombo.setValue(nz(ev.getBioSampleType()));
            }
            case "Currency" -> {
                if (currencyAmountField != null) currencyAmountField.setText(nz(ev.getCurrencyAmount()));
            }
            case "Electronics" -> {
                if (electronicTypeCombo != null) electronicTypeCombo.setValue(nz(ev.getElecDeviceType()));
                if (electronicMakeField != null) electronicMakeField.setText(nz(ev.getElecMake()));
                if (electronicModelField != null) electronicModelField.setText(nz(ev.getElecModel()));
                if (electronicSerialField != null) electronicSerialField.setText(nz(ev.getElecSerialNumber()));
                if (electronicUsernameField != null) electronicUsernameField.setText(nz(ev.getElecDeviceUsername()));
                if (electronicPasswordField != null) electronicPasswordField.setText(nz(ev.getElecDevicePassword()));
            }
            case "Firearm" -> {
                if (firearmMakeField != null) firearmMakeField.setText(nz(ev.getFirearmMake()));
                if (firearmModelField != null) firearmModelField.setText(nz(ev.getFirearmModel()));
                if (firearmSerialField != null) firearmSerialField.setText(nz(ev.getFirearmSerialNumber()));
                if (firearmCaliberCombo != null) firearmCaliberCombo.setValue(nz(ev.getFirearmCaliber()));
                if (firearmReportedStolenCheck != null) firearmReportedStolenCheck.setSelected(ev.isFirearmReportedStolen());
                if (firearmLoadedWhenRecoveredCheck != null) firearmLoadedWhenRecoveredCheck.setSelected(ev.isFirearmLoadedWhenRecovered());
            }
            case "Jewelry" -> {
                if (jewelryMakeField != null) jewelryMakeField.setText(nz(ev.getJewelryMaterial()));
                if (jewelryModelField != null) jewelryModelField.setText(nz(ev.getJewelryType()));
                if (jewelrySerialField != null) jewelrySerialField.setText(nz(ev.getJewelryEngravingOrId()));
            }
            case "Narcotic Equipment" -> {
                if (narcEquipMakeField != null) narcEquipMakeField.setText(nz(ev.getNarcEquipType()));
                if (narcEquipModelField != null) narcEquipModelField.setText(nz(ev.getNarcEquipDescription()));
                if (narcEquipSerialField != null) narcEquipSerialField.setText(nz(ev.getNarcEquipSuspectedResidue()));
            }
            case "Narcotics" -> {
                if (narcoticDrugTypeCombo != null) narcoticDrugTypeCombo.setValue(nz(ev.getNarcDrugType()));
                if (narcoticUnitTypeCombo != null) narcoticUnitTypeCombo.setValue(nz(ev.getNarcForm()));
                if (narcoticUnitNumberField != null) narcoticUnitNumberField.setText(nz(ev.getNarcNetWeight()));
            }
            case "Vehicle" -> {
                if (vehicleMakeField != null) vehicleMakeField.setText(nz(ev.getVehicleMake()));
                if (vehicleModelField != null) vehicleModelField.setText(nz(ev.getVehicleModel()));
                if (vehicleYearField != null) vehicleYearField.setText(nz(ev.getVehicleYear()));
                if (vehicleLicensePlateField != null) vehicleLicensePlateField.setText(nz(ev.getVehicleLicensePlate()));
                if (vehicleVinField != null) vehicleVinField.setText(nz(ev.getVehicleVin()));
            }
            case "Weapon" -> {
                if (weaponTypeCombo != null) weaponTypeCombo.setValue(nz(ev.getWeaponType()));
                if (weaponMakeField != null) weaponMakeField.setText(nz(ev.getWeaponMake()));
                if (weaponModelField != null) weaponModelField.setText(nz(ev.getWeaponModel()));
                if (weaponSerialField != null) weaponSerialField.setText(nz(ev.getWeaponSerialNumber()));
            }
        }
    }

    @FXML
    private void onCollectedFromPersonToggle() {
        boolean show = collectedFromPersonCheck.isSelected();
        collectedFromPersonBox.setVisible(show);
        collectedFromPersonBox.setManaged(show);
        if (show && collectedFromPersonCombo.getItems().isEmpty()) {
            Dialogs.warn("No persons on this case",
                    "Associate a person with this case from the Case page before selecting one here.");
        }
    }

    @FXML
    private void onEvidenceTypeChanged() {
        dynamicPanel.getChildren().clear();
        String type = evidenceTypeCombo.getValue();
        if (type == null) return;

        if (editEvidence == null) {
            statusCombo.setValue("In Dropbox");
        }

        switch (type) {
            case "Ammunition"         -> buildAmmunitionPanel();
            case "Biological / DNA"   -> buildBiologicalPanel();
            case "Currency"           -> buildCurrencyPanel();
            case "Electronics"        -> buildElectronicsPanel();
            case "Firearm"            -> buildFirearmPanel();
            case "Jewelry"            -> buildJewelryPanel();
            case "Narcotic Equipment" -> buildNarcEquipPanel();
            case "Narcotics"          -> buildNarcoticsPanel();
            case "Vehicle"            -> buildVehiclePanel();
            case "Weapon"             -> buildWeaponPanel();
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ PANEL BUILDERS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        firearmReportedStolenCheck = new CheckBox("Reported Stolen");
        firearmLoadedWhenRecoveredCheck = new CheckBox("Loaded When Recovered");
        HBox yesNoRow = new HBox(24, firearmReportedStolenCheck, firearmLoadedWhenRecoveredCheck);
        g.add(yesNoRow, 0, 2, 2, 1);
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

    @SuppressWarnings("unused")
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ SAVE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    private void onSave() {
        String type = evidenceTypeCombo.getValue();
        if (type == null) { Dialogs.warn("Evidence type required", "Please select an evidence type."); return; }
        if ("Vehicle".equals(type) && editEvidence == null) {
            Dialogs.warn("Wrong screen",
                    "Vehicles must be entered from the separate Impound Vehicle screen.");
            return;
        }
        String storageLocation = comboText(storageLocationCombo);
        if (storageLocation == null || storageLocation.isBlank()) {
            if (dropboxLocations.isEmpty()) {
                Dialogs.warn("No drop box locations configured",
                        "An administrator must add at least one Intake (Drop Box) location in Lookup Admin before evidence can be intaken.");
            } else {
                Dialogs.warn("Drop box required", "Choose the drop box you placed this item into.");
            }
            return;
        }

        Evidence ev = editEvidence != null ? editEvidence : new Evidence();
        ev.setCaseId(currentCase.getId());
        ev.setEvidenceType(type);
        ev.setDescription(descriptionField.getText().trim());
        ev.setStorageLocation(storageLocation);
        ev.setStatus(editEvidence == null ? "In Dropbox" : comboText(statusCombo));

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

        if (editEvidence != null) {
            ev.setBarcode(editEvidence.getBarcode());
            ev.setScanCode(editEvidence.getScanCode());
        }

        try {
            if (editEvidence == null) {
                evidenceRepo.save(ev);
            } else {
                ev.setId(editEvidence.getId());
                evidenceRepo.update(ev);
            }
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
                if (firearmReportedStolenCheck != null) ev.setFirearmReportedStolen(firearmReportedStolenCheck.isSelected());
                if (firearmLoadedWhenRecoveredCheck != null) ev.setFirearmLoadedWhenRecovered(firearmLoadedWhenRecoveredCheck.isSelected());
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
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ HELPERS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    @SuppressWarnings("unused")
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
        Dialogs.error(e);
    }

    private String comboText(ComboBox<String> combo) {
        if (combo == null) return null;
        String value = combo.getValue();
        if (value != null && !value.isBlank()) return value.trim();
        return combo.getEditor() == null ? null : combo.getEditor().getText();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

}
