package com.evidenceharbor.ui.evidence;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.*;
import com.evidenceharbor.persistence.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AddEvidenceController implements Initializable {

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

    // Dynamic field references
    private ComboBox<String> caliberCombo;
    private TextField quantityField;
    private TextField grainWeightField;
    private TextField bulletTypeField;
    private TextField brandField;

    // Biological/DNA
    private ComboBox<String> bioSampleTypeCombo;
    private TextField bioCollectionMethodField;
    private TextField bioStorageTempField;
    private TextField bioSuspectNameField;
    private CheckBox boDnaAnalysisCheck;

    // Currency
    private TextField currencyAmountField;
    private ComboBox<String> currencyDenominationCombo;
    private TextField currencySerialField;
    private CheckBox currencyCounterfeitCheck;

    // Electronics
    private ComboBox<String> electronicTypeCombo;
    private TextField electronicMakeField;
    private TextField electronicModelField;
    private TextField electronicSerialField;
    private CheckBox electronicPasswordCheck;
    private CheckBox electronicDataExtractCheck;

    // Firearms
    private TextField firearmMakeField;
    private TextField firearmModelField;
    private TextField firearmSerialField;
    private ComboBox<String> firearmTypeCombo;
    private TextField firearmCaliberField;
    private CheckBox firearmStolenCheck;
    private CheckBox firearmLoadedCheck;

    // Jewelry
    private ComboBox<String> jewelryTypeCombo;
    private TextField jewelryMaterialField;
    private TextField jewelryEstValueField;
    private TextField jewelryEngravingField;

    // Narcotic Equipment
    private ComboBox<String> narcEquipTypeCombo;
    private TextField narcEquipDescField;
    private TextField narcEquipResidueField;
    private CheckBox narcEquipTestKitCheck;

    // Narcotics
    private ComboBox<String> narcoticDrugTypeCombo;
    private TextField narcoticWeightField;
    private ComboBox<String> narcoticFormCombo;
    private TextField narcoticPackagingField;
    private CheckBox narcoticFieldTestCheck;
    private TextField narcoticFieldTestResultField;

    // Vehicles
    private TextField vehicleMakeField;
    private TextField vehicleModelField;
    private TextField vehicleYearField;
    private TextField vehicleColorField;
    private TextField vehicleVinField;
    private TextField vehicleLicensePlateField;
    private TextField vehicleLicenseStateField;
    private CheckBox vehicleStolenCheck;
    private CheckBox vehicleImpoundedCheck;

    // Weapons (non-firearm)
    private ComboBox<String> weaponTypeCombo;
    private TextField weaponMakeField;
    private TextField weaponModelField;
    private TextField weaponSerialField;
    private TextField weaponLengthField;
    private CheckBox weaponStolenCheck;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        evidenceTypeCombo.setItems(FXCollections.observableArrayList(
                "Ammunition", "Biological / DNA", "Currency", "Electronics",
                "Firearm", "Jewelry", "Narcotic Equipment", "Narcotics",
                "Vehicle", "Weapon"));

        statusCombo.setItems(FXCollections.observableArrayList(
                "Active", "In Storage", "Submitted for Analysis",
                "Released", "Destroyed", "Transferred"));
        statusCombo.setValue("Active");

        collectionDate.setValue(LocalDate.now());
    }

    public void initForCase(Case c) {
        this.currentCase = c;
        breadcrumbCase.setText("Case #" + c.getCaseNumber());
        caseNumberLabel.setText("Case #" + c.getCaseNumber());
        try {
            collectedByCombo.setItems(FXCollections.observableArrayList(officerRepo.findAll()));
            collectedFromPersonCombo.setItems(FXCollections.observableArrayList(personRepo.findAll()));
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

    // ──────────────────────────────── PANEL BUILDERS ────────────────────────────────

    private void buildAmmunitionPanel() {
        GridPane g = grid();
        caliberCombo = comboField();
        quantityField = textField("e.g. 50");
        grainWeightField = textField("e.g. 124");
        bulletTypeField = textField("e.g. FMJ, HP");
        brandField = textField("e.g. Federal");
        try { caliberCombo.getItems().addAll(lookupRepo.getCalibers()); } catch (Exception e) { showError(e); }

        addRow(g, 0, "Caliber",      caliberCombo,    "Quantity",     quantityField);
        addRow(g, 1, "Grain Weight", grainWeightField, "Bullet Type", bulletTypeField);
        addRow(g, 2, "Brand",        brandField,       null,          null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildBiologicalPanel() {
        GridPane g = grid();
        bioSampleTypeCombo = comboField("Blood", "Saliva", "Hair", "Tissue", "Urine", "Semen", "Fingernail", "Other");
        bioCollectionMethodField = textField("e.g. Swab, Buccal");
        bioStorageTempField = textField("e.g. Refrigerated, Frozen");
        bioSuspectNameField = textField();
        boDnaAnalysisCheck = checkField("DNA Analysis Requested");

        addRow(g, 0, "Sample Type",    bioSampleTypeCombo,    "Collection Method", bioCollectionMethodField);
        addRow(g, 1, "Storage Temp",   bioStorageTempField,   "Suspect / Subject",  bioSuspectNameField);
        GridPane.setColumnSpan(boDnaAnalysisCheck, 2);
        g.add(boDnaAnalysisCheck, 0, 2);
        dynamicPanel.getChildren().add(g);
    }

    private void buildCurrencyPanel() {
        GridPane g = grid();
        currencyAmountField = textField("0.00");
        currencyDenominationCombo = comboField("Mixed", "$1", "$5", "$10", "$20", "$50", "$100", "Coin", "Foreign");
        currencySerialField = textField();
        currencyCounterfeitCheck = checkField("Suspected Counterfeit");

        addRow(g, 0, "Total Amount ($)", currencyAmountField,   "Denominations",    currencyDenominationCombo);
        addRow(g, 1, "Serial Number",    currencySerialField,   null,               null);
        g.add(currencyCounterfeitCheck, 0, 2);
        dynamicPanel.getChildren().add(g);
    }

    private void buildElectronicsPanel() {
        GridPane g = grid();
        electronicTypeCombo = comboField();
        electronicMakeField = textField("e.g. Apple, Samsung");
        electronicModelField = textField();
        electronicSerialField = textField();
        electronicPasswordCheck = checkField("Password / PIN Protected");
        electronicDataExtractCheck = checkField("Data Extraction Requested");
        try { electronicTypeCombo.getItems().addAll(lookupRepo.getElectronicTypes()); } catch (Exception e) { showError(e); }

        addRow(g, 0, "Device Type", electronicTypeCombo, "Make",  electronicMakeField);
        addRow(g, 1, "Model",       electronicModelField, "Serial #", electronicSerialField);
        g.add(electronicPasswordCheck,   0, 2); g.add(electronicDataExtractCheck, 1, 2);
        dynamicPanel.getChildren().add(g);
    }

    private void buildFirearmPanel() {
        GridPane g = grid();
        firearmMakeField   = textField("e.g. Glock, Smith & Wesson");
        firearmModelField  = textField();
        firearmSerialField = textField();
        firearmTypeCombo   = comboField("Pistol", "Revolver", "Rifle", "Shotgun", "Semi-Auto Rifle", "Full-Auto", "Other");
        firearmCaliberField = textField();
        firearmStolenCheck  = checkField("Reported Stolen");
        firearmLoadedCheck  = checkField("Was Loaded When Recovered");

        addRow(g, 0, "Make",     firearmMakeField,   "Model",   firearmModelField);
        addRow(g, 1, "Serial #", firearmSerialField, "Type",    firearmTypeCombo);
        addRow(g, 2, "Caliber",  firearmCaliberField, null,     null);
        g.add(firearmStolenCheck, 0, 3); g.add(firearmLoadedCheck, 1, 3);
        dynamicPanel.getChildren().add(g);
    }

    private void buildJewelryPanel() {
        GridPane g = grid();
        jewelryTypeCombo    = comboField("Ring", "Necklace", "Bracelet", "Earring", "Watch", "Brooch", "Other");
        jewelryMaterialField = textField("e.g. Gold, Silver, Platinum");
        jewelryEstValueField = textField("0.00");
        jewelryEngravingField = textField();

        addRow(g, 0, "Type",           jewelryTypeCombo,     "Material",       jewelryMaterialField);
        addRow(g, 1, "Est. Value ($)", jewelryEstValueField, "Engraving / ID", jewelryEngravingField);
        dynamicPanel.getChildren().add(g);
    }

    private void buildNarcEquipPanel() {
        GridPane g = grid();
        narcEquipTypeCombo   = comboField("Pipe", "Syringe", "Scale", "Spoon", "Bag/Packaging", "Rolling Papers", "Bong", "Grinder", "Other");
        narcEquipDescField   = textField();
        narcEquipResidueField = textField("e.g. White powder, Brown residue");
        narcEquipTestKitCheck = checkField("Field Test Kit Used");

        addRow(g, 0, "Equipment Type",       narcEquipTypeCombo,   "Description",  narcEquipDescField);
        addRow(g, 1, "Suspected Residue",    narcEquipResidueField, null,           null);
        g.add(narcEquipTestKitCheck, 0, 2);
        dynamicPanel.getChildren().add(g);
    }

    private void buildNarcoticsPanel() {
        GridPane g = grid();
        narcoticDrugTypeCombo  = comboField();
        narcoticWeightField    = textField("grams");
        narcoticFormCombo      = comboField("Powder", "Rock/Crystal", "Pill/Tablet", "Liquid", "Plant Material", "Resin", "Other");
        narcoticPackagingField = textField("e.g. Zip-lock bag, Foil");
        narcoticFieldTestCheck = checkField("Field Test Performed");
        narcoticFieldTestResultField = textField("e.g. Positive for cocaine");

        try { narcoticDrugTypeCombo.getItems().addAll(lookupRepo.getNarcoticsTypes()); } catch (Exception e) { showError(e); }

        addRow(g, 0, "Drug Type",   narcoticDrugTypeCombo,  "Form",             narcoticFormCombo);
        addRow(g, 1, "Net Weight",  narcoticWeightField,    "Packaging",        narcoticPackagingField);
        g.add(narcoticFieldTestCheck, 0, 2);
        addRow(g, 3, "Field Test Result", narcoticFieldTestResultField, null, null);
        dynamicPanel.getChildren().add(g);
    }

    private void buildVehiclePanel() {
        GridPane g = grid();
        vehicleMakeField         = textField("e.g. Ford, Honda");
        vehicleModelField        = textField();
        vehicleYearField         = textField("YYYY");
        vehicleColorField        = textField();
        vehicleVinField          = textField();
        vehicleLicensePlateField = textField();
        vehicleLicenseStateField = textField("e.g. TX");
        vehicleStolenCheck       = checkField("Reported Stolen");
        vehicleImpoundedCheck    = checkField("Impounded");

        addRow(g, 0, "Make",          vehicleMakeField,         "Model",         vehicleModelField);
        addRow(g, 1, "Year",          vehicleYearField,         "Color",         vehicleColorField);
        addRow(g, 2, "VIN",           vehicleVinField,          null,            null);
        addRow(g, 3, "License Plate", vehicleLicensePlateField, "Plate State",   vehicleLicenseStateField);
        g.add(vehicleStolenCheck, 0, 4); g.add(vehicleImpoundedCheck, 1, 4);
        dynamicPanel.getChildren().add(g);
    }

    private void buildWeaponPanel() {
        GridPane g = grid();
        weaponTypeCombo  = comboField("Knife", "Sword", "Machete", "Bat/Club", "Taser/Stun Gun", "Brass Knuckles", "Bow", "Other");
        weaponMakeField  = textField();
        weaponModelField = textField();
        weaponSerialField = textField();
        weaponLengthField = textField("inches");
        weaponStolenCheck = checkField("Reported Stolen");

        addRow(g, 0, "Weapon Type", weaponTypeCombo,  "Make",     weaponMakeField);
        addRow(g, 1, "Model",       weaponModelField, "Serial #", weaponSerialField);
        addRow(g, 2, "Length",      weaponLengthField, null,      null);
        g.add(weaponStolenCheck, 0, 3);
        dynamicPanel.getChildren().add(g);
    }

    // ──────────────────────────────── SAVE ────────────────────────────────

    @FXML
    private void onSave() {
        String type = evidenceTypeCombo.getValue();
        if (type == null) { new Alert(Alert.AlertType.WARNING, "Please select an evidence type.").showAndWait(); return; }

        Evidence ev = new Evidence();
        ev.setCaseId(currentCase.getId());
        ev.setEvidenceType(type);
        ev.setDescription(descriptionField.getText().trim());
        ev.setStorageLocation(storageLocationField.getText().trim());
        ev.setStatus(statusCombo.getValue());

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
                if (grainWeightField != null) ev.setAmmoGrainWeight(grainWeightField.getText());
                if (bulletTypeField != null) ev.setAmmoBulletType(bulletTypeField.getText());
                if (brandField != null) ev.setAmmoBrand(brandField.getText());
            }
            case "Biological / DNA" -> {
                if (bioSampleTypeCombo != null) ev.setBioSampleType(bioSampleTypeCombo.getValue());
                if (bioCollectionMethodField != null) ev.setBioCollectionMethod(bioCollectionMethodField.getText());
                if (bioStorageTempField != null) ev.setBioStorageTemp(bioStorageTempField.getText());
                if (bioSuspectNameField != null) ev.setBioSuspectName(bioSuspectNameField.getText());
                if (boDnaAnalysisCheck != null) ev.setBioDnaAnalysisRequested(boDnaAnalysisCheck.isSelected());
            }
            case "Currency" -> {
                if (currencyAmountField != null) ev.setCurrencyAmount(currencyAmountField.getText());
                if (currencyDenominationCombo != null) ev.setCurrencyDenominations(currencyDenominationCombo.getValue());
                if (currencySerialField != null) ev.setCurrencySerialNumbers(currencySerialField.getText());
                if (currencyCounterfeitCheck != null) ev.setCurrencySuspectedCounterfeit(currencyCounterfeitCheck.isSelected());
            }
            case "Electronics" -> {
                if (electronicTypeCombo != null) ev.setElecDeviceType(electronicTypeCombo.getValue());
                if (electronicMakeField != null) ev.setElecMake(electronicMakeField.getText());
                if (electronicModelField != null) ev.setElecModel(electronicModelField.getText());
                if (electronicSerialField != null) ev.setElecSerialNumber(electronicSerialField.getText());
                if (electronicPasswordCheck != null) ev.setElecPasswordProtected(electronicPasswordCheck.isSelected());
                if (electronicDataExtractCheck != null) ev.setElecDataExtractionRequested(electronicDataExtractCheck.isSelected());
            }
            case "Firearm" -> {
                if (firearmMakeField != null) ev.setFirearmMake(firearmMakeField.getText());
                if (firearmModelField != null) ev.setFirearmModel(firearmModelField.getText());
                if (firearmSerialField != null) ev.setFirearmSerialNumber(firearmSerialField.getText());
                if (firearmTypeCombo != null) ev.setFirearmType(firearmTypeCombo.getValue());
                if (firearmCaliberField != null) ev.setFirearmCaliber(firearmCaliberField.getText());
                if (firearmStolenCheck != null) ev.setFirearmReportedStolen(firearmStolenCheck.isSelected());
                if (firearmLoadedCheck != null) ev.setFirearmLoadedWhenRecovered(firearmLoadedCheck.isSelected());
            }
            case "Jewelry" -> {
                if (jewelryTypeCombo != null) ev.setJewelryType(jewelryTypeCombo.getValue());
                if (jewelryMaterialField != null) ev.setJewelryMaterial(jewelryMaterialField.getText());
                if (jewelryEstValueField != null) ev.setJewelryEstimatedValue(jewelryEstValueField.getText());
                if (jewelryEngravingField != null) ev.setJewelryEngravingOrId(jewelryEngravingField.getText());
            }
            case "Narcotic Equipment" -> {
                if (narcEquipTypeCombo != null) ev.setNarcEquipType(narcEquipTypeCombo.getValue());
                if (narcEquipDescField != null) ev.setNarcEquipDescription(narcEquipDescField.getText());
                if (narcEquipResidueField != null) ev.setNarcEquipSuspectedResidue(narcEquipResidueField.getText());
                if (narcEquipTestKitCheck != null) ev.setNarcEquipFieldTestKitUsed(narcEquipTestKitCheck.isSelected());
            }
            case "Narcotics" -> {
                if (narcoticDrugTypeCombo != null) ev.setNarcDrugType(narcoticDrugTypeCombo.getValue());
                if (narcoticWeightField != null) ev.setNarcNetWeight(narcoticWeightField.getText());
                if (narcoticFormCombo != null) ev.setNarcForm(narcoticFormCombo.getValue());
                if (narcoticPackagingField != null) ev.setNarcPackaging(narcoticPackagingField.getText());
                if (narcoticFieldTestCheck != null) ev.setNarcFieldTestPerformed(narcoticFieldTestCheck.isSelected());
                if (narcoticFieldTestResultField != null) ev.setNarcFieldTestResult(narcoticFieldTestResultField.getText());
            }
            case "Vehicle" -> {
                if (vehicleMakeField != null) ev.setVehicleMake(vehicleMakeField.getText());
                if (vehicleModelField != null) ev.setVehicleModel(vehicleModelField.getText());
                if (vehicleYearField != null) ev.setVehicleYear(vehicleYearField.getText());
                if (vehicleColorField != null) ev.setVehicleColor(vehicleColorField.getText());
                if (vehicleVinField != null) ev.setVehicleVin(vehicleVinField.getText());
                if (vehicleLicensePlateField != null) ev.setVehicleLicensePlate(vehicleLicensePlateField.getText());
                if (vehicleLicenseStateField != null) ev.setVehicleLicenseState(vehicleLicenseStateField.getText());
                if (vehicleStolenCheck != null) ev.setVehicleReportedStolen(vehicleStolenCheck.isSelected());
                if (vehicleImpoundedCheck != null) ev.setVehicleImpounded(vehicleImpoundedCheck.isSelected());
            }
            case "Weapon" -> {
                if (weaponTypeCombo != null) ev.setWeaponType(weaponTypeCombo.getValue());
                if (weaponMakeField != null) ev.setWeaponMake(weaponMakeField.getText());
                if (weaponModelField != null) ev.setWeaponModel(weaponModelField.getText());
                if (weaponSerialField != null) ev.setWeaponSerialNumber(weaponSerialField.getText());
                if (weaponLengthField != null) ev.setWeaponLength(weaponLengthField.getText());
                if (weaponStolenCheck != null) ev.setWeaponReportedStolen(weaponStolenCheck.isSelected());
            }
        }
    }

    @FXML private void onBack() { Navigator.get().showCaseDetail(currentCase); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }

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
}
