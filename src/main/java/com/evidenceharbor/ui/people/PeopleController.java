package com.evidenceharbor.ui.people;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.app.SessionManager;
import com.evidenceharbor.domain.Person;
import com.evidenceharbor.persistence.PersonRepository;
import com.evidenceharbor.util.Dialogs;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    @FXML private Button btnEdit;
    @FXML private Button btnMerge;
    @FXML private TableView<Person> peopleTable;
    @FXML private TableColumn<Person, String> colName;
    @FXML private TableColumn<Person, String> colDob;
    @FXML private TableColumn<Person, String> colSsn;
    @FXML private TableColumn<Person, String> colAddress;
    @FXML private TableColumn<Person, String> colContact;
    @FXML private TableColumn<Person, String> colCases;

    private final PersonRepository personRepo = new PersonRepository();
    private List<Person> allPeople = List.of();
    private Map<Integer, Integer> caseCounts = Map.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFullName()));
        colDob.setCellValueFactory(cd -> new SimpleStringProperty(nv(cd.getValue().getDob())));
        colSsn.setCellValueFactory(cd -> new SimpleStringProperty(nv(cd.getValue().getSsn())));
        colAddress.setCellValueFactory(cd -> new SimpleStringProperty(addressOneLine(cd.getValue())));
        colContact.setCellValueFactory(cd -> new SimpleStringProperty(nv(cd.getValue().getContact())));
        colCases.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(caseCounts.getOrDefault(cd.getValue().getId(), 0))));

        peopleTable.setRowFactory(tv -> {
            TableRow<Person> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    showPersonDetail(row.getItem());
                }
            });
            return row;
        });

        peopleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean has = newV != null;
            boolean canEdit = has && canEditPeople();
            btnEdit.setDisable(!canEdit);
            btnMerge.setDisable(!canEdit);
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearch(newVal));
        loadPeople();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);
    }

    private boolean canEditPeople() {
        return SessionManager.isAdmin()
                || "evidence_tech".equalsIgnoreCase(
                        SessionManager.getCurrentOfficer() == null ? "" : SessionManager.getCurrentOfficer().getRole())
                || SessionManager.can("can_edit_all_evidence");
    }

    private void loadPeople() {
        try {
            allPeople = personRepo.findAll();
            caseCounts = personRepo.caseCountByPerson();
            peopleTable.setItems(FXCollections.observableArrayList(allPeople));
            peopleTable.refresh();
        } catch (Exception e) {
            showError(e.getMessage());
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
                        .filter(p -> matches(p, q))
                        .collect(Collectors.toList())
        ));
    }

    private boolean matches(Person p, String q) {
        return contains(p.getFullName(), q)
                || contains(p.getDob(), q)
                || contains(p.getSsn(), q)
                || contains(p.getContact(), q)
                || contains(addressOneLine(p), q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase().contains(q);
    }

    private static String nv(String s) { return s == null ? "" : s; }

    private static String addressOneLine(Person p) {
        StringBuilder sb = new StringBuilder();
        if (p.getStreet() != null && !p.getStreet().isBlank()) sb.append(p.getStreet().trim());
        StringBuilder csz = new StringBuilder();
        if (p.getCity() != null && !p.getCity().isBlank()) csz.append(p.getCity().trim());
        if (p.getState() != null && !p.getState().isBlank()) {
            if (csz.length() > 0) csz.append(", ");
            csz.append(p.getState().trim());
        }
        if (p.getZip() != null && !p.getZip().isBlank()) {
            if (csz.length() > 0) csz.append(" ");
            csz.append(p.getZip().trim());
        }
        if (csz.length() > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(csz);
        }
        return sb.toString();
    }

    // ── Person detail dialog ──────────────────────────────────────────────

    private void showPersonDetail(Person person) {
        // Re-fetch so we always see the latest DB values.
        Person fresh = person;
        try {
            Person loaded = personRepo.findById(person.getId());
            if (loaded != null) fresh = loaded;
        } catch (Exception e) {
            showError(e.getMessage());
        }
        final Person p = fresh;

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Person — " + p.getFullName());
        dlg.setHeaderText(p.getFullName());
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setPrefSize(760, 600);

        VBox root = new VBox(12);
        root.setPadding(new Insets(10));

        // ── Person details grid ──
        GridPane info = new GridPane();
        info.setHgap(12);
        info.setVgap(6);
        info.setPadding(new Insets(4, 0, 8, 0));
        int r = 0;
        addInfoRow(info, r++, "Full Name", p.getFullName());
        addInfoRow(info, r++, "Date of Birth", p.getDob());
        addInfoRow(info, r++, "SSN", p.getSsn());
        String address = buildAddress(p);
        addInfoRow(info, r++, "Address", address);
        addInfoRow(info, r++, "Contact", p.getContact());
        addInfoRow(info, r++, "Person ID", String.valueOf(p.getId()));

        TableView<PersonRepository.PersonCaseRow> tbl = new TableView<>();
        tbl.getStyleClass().add("data-table");
        TableColumn<PersonRepository.PersonCaseRow, String> cNum = new TableColumn<>("CASE #");
        cNum.setPrefWidth(160);
        cNum.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().caseNumber));
        TableColumn<PersonRepository.PersonCaseRow, String> cDate = new TableColumn<>("DATE");
        cDate.setPrefWidth(110);
        cDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().incidentDate));
        TableColumn<PersonRepository.PersonCaseRow, String> cRole = new TableColumn<>("ROLE");
        cRole.setPrefWidth(130);
        cRole.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().role));
        TableColumn<PersonRepository.PersonCaseRow, String> cOff = new TableColumn<>("CASE OFFICER");
        cOff.setPrefWidth(220);
        cOff.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().officerName));
        tbl.getColumns().addAll(java.util.List.of(cNum, cDate, cRole, cOff));
        tbl.setPlaceholder(new Label("This person is not associated with any cases."));

        try {
            tbl.setItems(FXCollections.observableArrayList(personRepo.findCasesForPerson(p.getId())));
        } catch (Exception e) {
            showError(e.getMessage());
        }

        tbl.setRowFactory(tv -> {
            TableRow<PersonRepository.PersonCaseRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    try {
                        var caseObj = new com.evidenceharbor.persistence.CaseRepository().findById(row.getItem().caseId);
                        if (caseObj != null) {
                            dlg.close();
                            Navigator.get().showCaseDetail(caseObj);
                        }
                    } catch (Exception ex) {
                        showError(ex.getMessage());
                    }
                }
            });
            return row;
        });

        Label hint = new Label("Double-click a case to open it.");
        hint.setStyle("-fx-text-fill:#94a3b8;");

        Label detailsHdr = new Label("Details:");
        detailsHdr.setStyle("-fx-font-weight:bold;");
        Label casesHdr = new Label("Cases for this person:");
        casesHdr.setStyle("-fx-font-weight:bold;");

        root.getChildren().addAll(detailsHdr, info, new Separator(), casesHdr, tbl, hint);
        VBox.setVgrow(tbl, javafx.scene.layout.Priority.ALWAYS);

        dlg.getDialogPane().setContent(root);
        Dialogs.style(dlg);
        dlg.showAndWait();
    }

    private void addInfoRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label + ":");
        l.setStyle("-fx-text-fill:#94a3b8;");
        Label v = new Label(value == null || value.isBlank() ? "—" : value);
        v.setWrapText(true);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private String buildAddress(Person p) {
        StringBuilder sb = new StringBuilder();
        if (p.getStreet() != null && !p.getStreet().isBlank()) sb.append(p.getStreet());
        StringBuilder csz = new StringBuilder();
        if (p.getCity() != null && !p.getCity().isBlank()) csz.append(p.getCity());
        if (p.getState() != null && !p.getState().isBlank()) {
            if (csz.length() > 0) csz.append(", ");
            csz.append(p.getState());
        }
        if (p.getZip() != null && !p.getZip().isBlank()) {
            if (csz.length() > 0) csz.append(" ");
            csz.append(p.getZip());
        }
        if (csz.length() > 0) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(csz);
        }
        return sb.toString();
    }

    // ── Edit person ───────────────────────────────────────────────────────

    @FXML
    private void onEditPerson() {
        Person sel = peopleTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!canEditPeople()) { showError("You do not have permission to edit people."); return; }

        Dialog<Person> dialog = new Dialog<>();
        dialog.setTitle("Edit Person");
        dialog.setHeaderText("Update details for " + sel.getFullName());

        TextField fullNameField = new TextField(nv(sel.getFullName()));
        fullNameField.setPromptText("Full name *");

        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("MM/dd/yyyy");
        try {
            if (sel.getDob() != null && !sel.getDob().isBlank()) {
                dobPicker.setValue(java.time.LocalDate.parse(sel.getDob(),
                        java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")));
            }
        } catch (Exception ignore) {}

        TextField ssnField = new TextField(nv(sel.getSsn()));
        ssnField.setPromptText("123-45-6789");

        TextField streetField = new TextField(nv(sel.getStreet()));
        streetField.setPromptText("Street");

        TextField cityField = new TextField(nv(sel.getCity()));
        cityField.setPromptText("City");

        ComboBox<String> stateBox = new ComboBox<>(FXCollections.observableArrayList(
                "AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA",
                "ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK",
                "OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY","DC"));
        stateBox.setEditable(true);
        stateBox.setPromptText("ST");
        if (sel.getState() != null && !sel.getState().isBlank()) {
            stateBox.setValue(sel.getState());
        }

        TextField zipField = new TextField(nv(sel.getZip()));
        zipField.setPromptText("ZIP");

        TextField contactField = new TextField(nv(sel.getContact()));
        contactField.setPromptText("Phone / email");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Full Name *"), 0, 0); grid.add(fullNameField, 1, 0);
        grid.add(new Label("DOB"),         0, 1); grid.add(dobPicker,     1, 1);
        grid.add(new Label("SSN"),         0, 2); grid.add(ssnField,      1, 2);
        grid.add(new Label("Street"),      0, 3); grid.add(streetField,   1, 3);
        grid.add(new Label("City"),        0, 4); grid.add(cityField,     1, 4);
        grid.add(new Label("State"),       0, 5); grid.add(stateBox,      1, 5);
        grid.add(new Label("ZIP"),         0, 6); grid.add(zipField,      1, 6);
        grid.add(new Label("Contact"),     0, 7); grid.add(contactField,  1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e -> {
            String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
            if (fullName.isEmpty()) {
                showError("Full name is required.");
                e.consume();
                return;
            }
            try {
                sel.setFullName(fullName);
                if (dobPicker.getValue() != null) {
                    sel.setDob(dobPicker.getValue().format(
                            java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                } else {
                    sel.setDob(null);
                }
                sel.setSsn(ssnField.getText());
                sel.setStreet(streetField.getText());
                sel.setCity(cityField.getText());
                String st = stateBox.getValue();
                if ((st == null || st.isBlank()) && stateBox.getEditor() != null) {
                    st = stateBox.getEditor().getText();
                }
                sel.setState(st);
                sel.setZip(zipField.getText());
                sel.setContact(contactField.getText());
                personRepo.save(sel);
            } catch (Exception ex) {
                showError(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                e.consume();
            }
        });

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? sel : null);
        Dialogs.style(dialog);
        Optional<Person> result = dialog.showAndWait();
        if (result.isPresent()) {
            loadPeople();
        }
    }

    // ── Merge / combine ───────────────────────────────────────────────────

    @FXML
    private void onMergePeople() {
        Person left = peopleTable.getSelectionModel().getSelectedItem();
        if (left == null) return;
        if (!canEditPeople()) { showError("You do not have permission to combine people."); return; }

        // Step 1: pick the right-hand person (the duplicate).
        ChoiceDialog<Person> picker = new ChoiceDialog<>(null,
                allPeople.stream()
                        .filter(p -> p.getId() != left.getId())
                        .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                        .collect(Collectors.toList()));
        picker.setTitle("Combine People");
        picker.setHeaderText("Select the duplicate person to combine with \"" + left.getFullName() + "\"");
        picker.setContentText("Duplicate:");
        Dialogs.style(picker);
        Optional<Person> pickedOpt = picker.showAndWait();
        if (pickedOpt.isEmpty() || pickedOpt.get() == null) return;
        Person right = pickedOpt.get();

        // Step 2: field-by-field picker dialog.
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Combine People — Choose Values");
        dlg.setHeaderText("Pick which value to keep for each field.");
        dlg.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Combine", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(720);

        VBox root = new VBox(14);
        root.setPadding(new Insets(16));

        // Column headers
        GridPane headers = new GridPane();
        headers.setHgap(12);
        ColumnConstraints cLeft = new ColumnConstraints();   cLeft.setPercentWidth(45);
        ColumnConstraints cMid  = new ColumnConstraints();   cMid.setPercentWidth(10);
        ColumnConstraints cRight= new ColumnConstraints();   cRight.setPercentWidth(45);
        headers.getColumnConstraints().addAll(cLeft, cMid, cRight);
        Label hL = new Label("◀ " + left.getFullName()  + "  (ID " + left.getId()  + ")");
        Label hR = new Label(right.getFullName() + "  (ID " + right.getId() + ") ▶");
        hL.setStyle("-fx-font-weight:bold; -fx-text-fill:#38bdf8;");
        hR.setStyle("-fx-font-weight:bold; -fx-text-fill:#a78bfa;");
        headers.add(hL, 0, 0);
        headers.add(new Label("FIELD"), 1, 0);
        headers.add(hR, 2, 0);
        GridPane.setHalignment(hR, javafx.geometry.HPos.RIGHT);
        GridPane.setHalignment(headers.getChildren().get(1), javafx.geometry.HPos.CENTER);

        // Field rows
        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(10);
        fields.getColumnConstraints().addAll(cLeft, cMid, cRight);

        List<FieldChoice> choices = new ArrayList<>();
        int row = 0;
        FieldChoice nameChoice    = addFieldRow(fields, row++, "Full Name",
                left.getFullName(), right.getFullName()); choices.add(nameChoice);
        FieldChoice dobChoice     = addFieldRow(fields, row++, "Date of Birth",
                left.getDob(), right.getDob()); choices.add(dobChoice);
        FieldChoice ssnChoice     = addFieldRow(fields, row++, "SSN",
                left.getSsn(), right.getSsn()); choices.add(ssnChoice);
        FieldChoice streetChoice  = addFieldRow(fields, row++, "Street",
                left.getStreet(), right.getStreet()); choices.add(streetChoice);
        FieldChoice cityChoice    = addFieldRow(fields, row++, "City",
                left.getCity(), right.getCity()); choices.add(cityChoice);
        FieldChoice stateChoice   = addFieldRow(fields, row++, "State",
                left.getState(), right.getState()); choices.add(stateChoice);
        FieldChoice zipChoice     = addFieldRow(fields, row++, "Zip",
                left.getZip(), right.getZip()); choices.add(zipChoice);
        FieldChoice contactChoice = addFieldRow(fields, row++, "Contact",
                left.getContact(), right.getContact()); choices.add(contactChoice);
        // Case counts are informational only (they are merged by moving rows).
        int leftCases  = caseCounts.getOrDefault(left.getId(),  0);
        int rightCases = caseCounts.getOrDefault(right.getId(), 0);
        Label lc = new Label(leftCases  + " case(s)");
        Label rc = new Label(rightCases + " case(s)");
        lc.setStyle("-fx-text-fill:#94a3b8;");
        rc.setStyle("-fx-text-fill:#94a3b8;");
        Label info = new Label("cases");
        info.setStyle("-fx-text-fill:#94a3b8;");
        fields.add(lc,  0, row);
        fields.add(info,1, row);
        fields.add(rc,  2, row);
        GridPane.setHalignment(rc,   javafx.geometry.HPos.RIGHT);
        GridPane.setHalignment(info, javafx.geometry.HPos.CENTER);
        row++;
        Label infoNote = new Label(
                "All cases from both people will be combined onto the surviving record.");
        infoNote.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:11px;");
        fields.add(infoNote, 0, row, 3, 1);

        Label warn = new Label(
                "⚠ This cannot be undone. The record NOT chosen for each field will be "
              + "discarded, and the duplicate person row will be deleted.");
        warn.setStyle("-fx-text-fill:#f6ad55;");
        warn.setWrapText(true);

        root.getChildren().addAll(headers, new Separator(), fields, warn);
        dlg.getDialogPane().setContent(root);
        Dialogs.style(dlg);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() != ButtonBar.ButtonData.OK_DONE) return;

            // Apply chosen field values to the surviving (left) record.
            String newName = nameChoice.chosenValue();
            if (newName == null || newName.trim().isEmpty()) {
                showError("Full Name cannot be blank."); return;
            }

            boolean ok = Dialogs.confirm("This action cannot be undone.",
                    "Combine \"" + right.getFullName() + "\" into \"" + left.getFullName() + "\"?\n\n"
                    + "Surviving name will be: \"" + newName + "\"\n"
                    + "\"" + right.getFullName() + "\" will be permanently deleted.");
            if (!ok) return;
            try {
                left.setFullName(newName);
                left.setDob(dobChoice.chosenValue());
                left.setSsn(ssnChoice.chosenValue());
                left.setStreet(streetChoice.chosenValue());
                left.setCity(cityChoice.chosenValue());
                left.setState(stateChoice.chosenValue());
                left.setZip(zipChoice.chosenValue());
                left.setContact(contactChoice.chosenValue());
                personRepo.save(left);
                personRepo.merge(left.getId(), right.getId());
                loadPeople();
                Dialogs.info("People combined", "Combined into \"" + newName + "\".");
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
    }

    /** Adds a labeled row with two radios (left/right) to {@code grid} and returns the chooser. */
    private FieldChoice addFieldRow(GridPane grid, int rowIdx, String label,
                                    String leftVal, String rightVal) {
        ToggleGroup tg = new ToggleGroup();
        String lv = leftVal  == null ? "" : leftVal;
        String rv = rightVal == null ? "" : rightVal;

        RadioButton rbL = new RadioButton(lv.isEmpty() ? "(blank)" : lv);
        RadioButton rbR = new RadioButton(rv.isEmpty() ? "(blank)" : rv);
        rbL.setToggleGroup(tg);
        rbR.setToggleGroup(tg);
        // Default: left side selected; if identical, still fine.
        rbL.setSelected(true);

        Label fieldLbl = new Label(label);
        fieldLbl.setStyle("-fx-text-fill:#94a3b8;");

        grid.add(rbL,      0, rowIdx);
        grid.add(fieldLbl, 1, rowIdx);
        grid.add(rbR,      2, rowIdx);
        GridPane.setHalignment(rbR, javafx.geometry.HPos.RIGHT);
        GridPane.setHalignment(fieldLbl, javafx.geometry.HPos.CENTER);

        return () -> rbR.isSelected() ? rv : lv;
    }

    @FunctionalInterface
    private interface FieldChoice { String chosenValue(); }

    private void showError(String msg) {
        Dialogs.error("People", msg);
    }

    // ── Nav ───────────────────────────────────────────────────────────────
    @FXML private void onCases() { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople() { }
    @FXML private void onDropbox() { Navigator.get().showDropbox(); }
    @FXML private void onReports() { Navigator.get().showReports(); }
    @FXML private void onSettings() { Navigator.get().showSettings(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    @FXML private void onBack() { Navigator.get().showCaseList(); }
    @FXML private void onDashboard() { Navigator.get().showCaseList(); }
}
