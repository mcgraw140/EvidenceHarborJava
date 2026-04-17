package com.evidenceharbor.ui.settings;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.AgencySettings;
import com.evidenceharbor.domain.LookupItem;
import com.evidenceharbor.persistence.LookupRepository;
import com.evidenceharbor.persistence.SettingsRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SettingsController implements Initializable {

    // Agency info
    @FXML private TextField agencyNameField;
    @FXML private TextField agencyAddressField;
    @FXML private TextField agencyCityField;
    @FXML private TextField agencyStateField;
    @FXML private TextField agencyZipField;

    // Number formats
    @FXML private TextField casePatternField;
    @FXML private TextField caseExampleField;
    @FXML private Label     casePatternStatus;
    @FXML private Label     caseValidationLabel;
    @FXML private TextField evidencePatternField;
    @FXML private TextField evidenceExampleField;
    @FXML private Label     evidencePatternStatus;
    @FXML private Label     evidenceValidationLabel;

    // Barcode
    @FXML private TextField barcodePrefixField;
    @FXML private Label     barcodePreviewLabel;

    // Lookup lists
    @FXML private TextField caliberInput;
    @FXML private ListView<LookupItem> caliberList;
    @FXML private TextField electronicInput;
    @FXML private ListView<LookupItem> electronicList;
    @FXML private TextField narcoticsInput;
    @FXML private ListView<LookupItem> narcoticsList;

    @FXML private TextField weaponTypeInput;
    @FXML private ListView<LookupItem> weaponTypeList;
    @FXML private TextField bioSourceInput;
    @FXML private ListView<LookupItem> bioSourceList;
    @FXML private TextField storageLocationInput;
    @FXML private ListView<LookupItem> storageLocationList;
    @FXML private TextField intakeLocationInput;
    @FXML private ListView<LookupItem> intakeLocationList;
    @FXML private TextField personRoleInput;
    @FXML private ListView<LookupItem> personRoleList;
    @FXML private TextField caseStatusInput;
    @FXML private ListView<LookupItem> caseStatusList;
    @FXML private TextField transferActionInput;
    @FXML private ListView<LookupItem> transferActionList;
    @FXML private TextField analysisLabInput;
    @FXML private ListView<LookupItem> analysisLabList;
    @FXML private TextField otherAgencyInput;
    @FXML private ListView<LookupItem> otherAgencyList;
    @FXML private TextField narcUnitTypeInput;
    @FXML private ListView<LookupItem> narcUnitTypeList;

    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final LookupRepository   lookupRepo   = new LookupRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAgencySettings();
        loadLookupLists();
        wirePatternValidation();
        wireBarcodePreview();
    }

    // ──────────────────────── LOAD ────────────────────────

    private void loadAgencySettings() {
        try {
            AgencySettings s = settingsRepo.load();
            agencyNameField.setText(s.getAgencyName());
            agencyAddressField.setText(s.getAgencyAddress());
            agencyCityField.setText(s.getAgencyCity());
            agencyStateField.setText(s.getAgencyState());
            agencyZipField.setText(s.getAgencyZip());
            casePatternField.setText(s.getCaseNumberPattern());
            caseExampleField.setText(s.getCaseNumberExample());
            evidencePatternField.setText(s.getEvidenceNumberPattern());
            evidenceExampleField.setText(s.getEvidenceNumberExample());
            barcodePrefixField.setText(s.getBarcodePrefix());
        } catch (Exception e) {
            showError("Failed to load settings: " + e.getMessage());
        }
    }

    private void loadLookupLists() {
        try {
            caliberList.setItems(FXCollections.observableArrayList(lookupRepo.getCaliberList()));
            electronicList.setItems(FXCollections.observableArrayList(lookupRepo.getElectronicTypeItems()));
            narcoticsList.setItems(FXCollections.observableArrayList(lookupRepo.getNarcoticsTypeItems()));
            weaponTypeList.setItems(FXCollections.observableArrayList(lookupRepo.getWeaponTypeItems()));
            bioSourceList.setItems(FXCollections.observableArrayList(lookupRepo.getBiologicalSourceItems()));
            storageLocationList.setItems(FXCollections.observableArrayList(lookupRepo.getStorageLocationItems()));
            intakeLocationList.setItems(FXCollections.observableArrayList(lookupRepo.getIntakeLocationItems()));
            personRoleList.setItems(FXCollections.observableArrayList(lookupRepo.getPersonRoleItems()));
            caseStatusList.setItems(FXCollections.observableArrayList(lookupRepo.getCaseStatusItems()));
            transferActionList.setItems(FXCollections.observableArrayList(lookupRepo.getTransferActionItems()));
            analysisLabList.setItems(FXCollections.observableArrayList(lookupRepo.getAnalysisLabItems()));
            otherAgencyList.setItems(FXCollections.observableArrayList(lookupRepo.getOtherAgencyItems()));
            narcUnitTypeList.setItems(FXCollections.observableArrayList(lookupRepo.getNarcoticsUnitTypeItems()));
        } catch (Exception e) {
            showError("Failed to load lookup lists: " + e.getMessage());
        }
    }

    // ──────────────────────── LIVE VALIDATION WIRING ────────────────────────

    private void wirePatternValidation() {
        casePatternField.textProperty().addListener((o, old, val) -> validatePattern(val, casePatternStatus));
        evidencePatternField.textProperty().addListener((o, old, val) -> validatePattern(val, evidencePatternStatus));

        caseExampleField.textProperty().addListener((o, old, val) ->
                validateExample(casePatternField.getText(), val, caseValidationLabel));
        casePatternField.textProperty().addListener((o, old, val) ->
                validateExample(val, caseExampleField.getText(), caseValidationLabel));

        evidenceExampleField.textProperty().addListener((o, old, val) ->
                validateExample(evidencePatternField.getText(), val, evidenceValidationLabel));
        evidencePatternField.textProperty().addListener((o, old, val) ->
                validateExample(val, evidenceExampleField.getText(), evidenceValidationLabel));
    }

    private void validatePattern(String pattern, Label statusLabel) {
        if (pattern == null || pattern.isBlank()) {
            statusLabel.setText("");
            return;
        }
        try {
            Pattern.compile(pattern);
            statusLabel.setText("✓ Valid regex");
            statusLabel.setTextFill(Color.GREEN);
        } catch (PatternSyntaxException e) {
            statusLabel.setText("✗ Invalid regex");
            statusLabel.setTextFill(Color.RED);
        }
    }

    private void validateExample(String pattern, String example, Label label) {
        if (pattern == null || pattern.isBlank() || example == null || example.isBlank()) {
            label.setText("");
            return;
        }
        try {
            boolean matches = example.matches(pattern);
            label.setText(matches ? "✓ Matches" : "✗ No match");
            label.setTextFill(matches ? Color.GREEN : Color.RED);
        } catch (PatternSyntaxException e) {
            label.setText("");
        }
    }

    private void wireBarcodePreview() {
        barcodePrefixField.textProperty().addListener((o, old, val) -> updateBarcodePreview(val));
    }

    private void updateBarcodePreview(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            barcodePreviewLabel.setText("");
            return;
        }
        String today = LocalDate.now().toString().replace("-", "");
        barcodePreviewLabel.setText("Preview: " + prefix + today + "00001");
    }

    // ──────────────────────── SAVE ────────────────────────

    @FXML
    private void onSaveAgency() {
        AgencySettings s = new AgencySettings();
        s.setAgencyName(agencyNameField.getText().trim());
        s.setAgencyAddress(agencyAddressField.getText().trim());
        s.setAgencyCity(agencyCityField.getText().trim());
        s.setAgencyState(agencyStateField.getText().trim());
        s.setAgencyZip(agencyZipField.getText().trim());
        s.setCaseNumberPattern(casePatternField.getText().trim());
        s.setCaseNumberExample(caseExampleField.getText().trim());
        s.setEvidenceNumberPattern(evidencePatternField.getText().trim());
        s.setEvidenceNumberExample(evidenceExampleField.getText().trim());
        s.setBarcodePrefix(barcodePrefixField.getText().trim());
        try {
            settingsRepo.save(s);
            showInfo("Settings saved successfully.");
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    // ──────────────────────── LOOKUP CRUD ────────────────────────

    @FXML
    private void onAddCaliber() {
        String name = caliberInput.getText().trim();
        if (name.isEmpty()) return;
        try {
            lookupRepo.addCaliber(name);
            caliberInput.clear();
            caliberList.setItems(FXCollections.observableArrayList(lookupRepo.getCaliberList()));
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    private void onRemoveCaliber() {
        LookupItem sel = caliberList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            lookupRepo.deleteCaliber(sel.getId());
            caliberList.setItems(FXCollections.observableArrayList(lookupRepo.getCaliberList()));
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    private void onAddElectronic() {
        String name = electronicInput.getText().trim();
        if (name.isEmpty()) return;
        try {
            lookupRepo.addElectronicType(name);
            electronicInput.clear();
            electronicList.setItems(FXCollections.observableArrayList(lookupRepo.getElectronicTypeItems()));
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    private void onRemoveElectronic() {
        LookupItem sel = electronicList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            lookupRepo.deleteElectronicType(sel.getId());
            electronicList.setItems(FXCollections.observableArrayList(lookupRepo.getElectronicTypeItems()));
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    private void onAddNarcotics() {
        String name = narcoticsInput.getText().trim();
        if (name.isEmpty()) return;
        try {
            lookupRepo.addNarcoticsType(name);
            narcoticsInput.clear();
            narcoticsList.setItems(FXCollections.observableArrayList(lookupRepo.getNarcoticsTypeItems()));
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    private void onRemoveNarcotics() {
        LookupItem sel = narcoticsList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            lookupRepo.deleteNarcoticsType(sel.getId());
            narcoticsList.setItems(FXCollections.observableArrayList(lookupRepo.getNarcoticsTypeItems()));
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML private void onAddWeaponType() { addRemoveHelper(weaponTypeInput, weaponTypeList, n -> lookupRepo.addWeaponType(n), () -> lookupRepo.getWeaponTypeItems()); }
    @FXML private void onRemoveWeaponType() { removeHelper(weaponTypeList, id -> lookupRepo.deleteWeaponType(id), () -> lookupRepo.getWeaponTypeItems()); }

    @FXML private void onAddBioSource() { addRemoveHelper(bioSourceInput, bioSourceList, n -> lookupRepo.addBiologicalSource(n), () -> lookupRepo.getBiologicalSourceItems()); }
    @FXML private void onRemoveBioSource() { removeHelper(bioSourceList, id -> lookupRepo.deleteBiologicalSource(id), () -> lookupRepo.getBiologicalSourceItems()); }

    @FXML private void onAddStorageLocation() { addRemoveHelper(storageLocationInput, storageLocationList, n -> lookupRepo.addStorageLocation(n), () -> lookupRepo.getStorageLocationItems()); }
    @FXML private void onRemoveStorageLocation() { removeHelper(storageLocationList, id -> lookupRepo.deleteStorageLocation(id), () -> lookupRepo.getStorageLocationItems()); }

    @FXML private void onAddIntakeLocation() { addRemoveHelper(intakeLocationInput, intakeLocationList, n -> lookupRepo.addIntakeLocation(n), () -> lookupRepo.getIntakeLocationItems()); }
    @FXML private void onRemoveIntakeLocation() { removeHelper(intakeLocationList, id -> lookupRepo.deleteIntakeLocation(id), () -> lookupRepo.getIntakeLocationItems()); }

    @FXML private void onAddPersonRole() { addRemoveHelper(personRoleInput, personRoleList, n -> lookupRepo.addPersonRole(n), () -> lookupRepo.getPersonRoleItems()); }
    @FXML private void onRemovePersonRole() { removeHelper(personRoleList, id -> lookupRepo.deletePersonRole(id), () -> lookupRepo.getPersonRoleItems()); }

    @FXML private void onAddCaseStatus() { addRemoveHelper(caseStatusInput, caseStatusList, n -> lookupRepo.addCaseStatus(n), () -> lookupRepo.getCaseStatusItems()); }
    @FXML private void onRemoveCaseStatus() { removeHelper(caseStatusList, id -> lookupRepo.deleteCaseStatus(id), () -> lookupRepo.getCaseStatusItems()); }

    @FXML private void onAddTransferAction() { addRemoveHelper(transferActionInput, transferActionList, n -> lookupRepo.addTransferAction(n), () -> lookupRepo.getTransferActionItems()); }
    @FXML private void onRemoveTransferAction() { removeHelper(transferActionList, id -> lookupRepo.deleteTransferAction(id), () -> lookupRepo.getTransferActionItems()); }

    @FXML private void onAddAnalysisLab() { addRemoveHelper(analysisLabInput, analysisLabList, n -> lookupRepo.addAnalysisLab(n), () -> lookupRepo.getAnalysisLabItems()); }
    @FXML private void onRemoveAnalysisLab() { removeHelper(analysisLabList, id -> lookupRepo.deleteAnalysisLab(id), () -> lookupRepo.getAnalysisLabItems()); }

    @FXML private void onAddOtherAgency() { addRemoveHelper(otherAgencyInput, otherAgencyList, n -> lookupRepo.addOtherAgency(n), () -> lookupRepo.getOtherAgencyItems()); }
    @FXML private void onRemoveOtherAgency() { removeHelper(otherAgencyList, id -> lookupRepo.deleteOtherAgency(id), () -> lookupRepo.getOtherAgencyItems()); }

    @FXML private void onAddNarcUnitType() { addRemoveHelper(narcUnitTypeInput, narcUnitTypeList, n -> lookupRepo.addNarcoticsUnitType(n), () -> lookupRepo.getNarcoticsUnitTypeItems()); }
    @FXML private void onRemoveNarcUnitType() { removeHelper(narcUnitTypeList, id -> lookupRepo.deleteNarcoticsUnitType(id), () -> lookupRepo.getNarcoticsUnitTypeItems()); }

    // ──────────────────────── NAV ────────────────────────

    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onAuditTrail()     { Navigator.get().showAuditTrail(); }
    @FXML private void onQuartermaster()  { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    // ──────────────────────── HELPERS ────────────────────────

    @FunctionalInterface
    interface ThrowingSupplier<T> { T get() throws Exception; }
    @FunctionalInterface
    interface ThrowingFunction<T, R> { R apply(T t) throws Exception; }
    @FunctionalInterface
    interface ThrowingConsumer<T> { void accept(T t) throws Exception; }

    private void addRemoveHelper(TextField input, ListView<LookupItem> list,
                                 ThrowingFunction<String, LookupItem> adder,
                                 ThrowingSupplier<java.util.List<LookupItem>> reloader) {
        String name = input.getText().trim();
        if (name.isEmpty()) return;
        try { adder.apply(name); input.clear(); list.setItems(FXCollections.observableArrayList(reloader.get())); }
        catch (Exception e) { showError(e.getMessage()); }
    }

    private void removeHelper(ListView<LookupItem> list,
                              ThrowingConsumer<Integer> deleter,
                              ThrowingSupplier<java.util.List<LookupItem>> reloader) {
        LookupItem sel = list.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try { deleter.accept(sel.getId()); list.setItems(FXCollections.observableArrayList(reloader.get())); }
        catch (Exception e) { showError(e.getMessage()); }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
