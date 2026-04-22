package com.evidenceharbor.ui.dropbox;

import com.evidenceharbor.app.CurrentOfficerResolver;
import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.ChainOfCustody;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.ChainOfCustodyRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import com.evidenceharbor.persistence.LookupRepository;
import com.evidenceharbor.persistence.OfficerRepository;
import com.evidenceharbor.util.Dialogs;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DropboxController implements Initializable {

    // â”€â”€ FXML bindings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navDropboxBtn;

    @FXML private Label dropboxCountLabel;
    @FXML private Label sessionStateLabel;
    @FXML private VBox idlePane;
    @FXML private VBox activePane;
    @FXML private VBox historyPane;
    @FXML private ComboBox<Officer> officerNameCombo;
    @FXML private Label activeSummaryLabel;

    // Dropbox active table
    @FXML private TableView<DropboxItem> dropboxTable;
    @FXML private TableColumn<DropboxItem, String> dbColBarcode;
    @FXML private TableColumn<DropboxItem, String> dbColCase;
    @FXML private TableColumn<DropboxItem, String> dbColDescription;
    @FXML private TableColumn<DropboxItem, String> dbColType;
    @FXML private TableColumn<DropboxItem, String> dbColAction;

    // History table
    @FXML private TableView<DropboxSession> historyTable;
    @FXML private TableColumn<DropboxSession, String> histColDate;
    @FXML private TableColumn<DropboxSession, String> histColOfficer;
    @FXML private TableColumn<DropboxSession, Integer> histColItems;

    // â”€â”€ Repos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final CaseRepository caseRepo = new CaseRepository();
    private final ChainOfCustodyRepository cocRepo   = new ChainOfCustodyRepository();
    private final LookupRepository lookupRepo = new LookupRepository();
    private final OfficerRepository officerRepo = new OfficerRepository();

    private final Map<Integer, String> caseNumberById = new HashMap<>();
    private final Map<Integer, String> officerNameById = new HashMap<>();
    private List<String> storageLocations = List.of();
    private List<String> intakeLocations = List.of();
    private final ObservableList<DropboxItem> sessionItems = FXCollections.observableArrayList();
    @SuppressWarnings("unused")
    private boolean sessionActive = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDropboxTable();
        setupHistoryTable();
        try {
            for (var c : caseRepo.findAll()) caseNumberById.put(c.getId(), c.getCaseNumber());
            List<Officer> officers = officerRepo.findAll();
            for (var o : officers) officerNameById.put(o.getId(), o.getName());
            officerNameCombo.getItems().setAll(officers);
            officerNameCombo.setConverter(new javafx.util.StringConverter<Officer>() {
                @Override public String toString(Officer o) { return o == null ? "" : o.getName(); }
                @Override public Officer fromString(String s) { return null; }
            });
            officerNameCombo.setEditable(false);

            Officer defaultOfficer = CurrentOfficerResolver.resolveDefaultOfficer(officerRepo);
            if (defaultOfficer != null) {
                int defaultId = defaultOfficer.getId();
                officers.stream()
                        .filter(o -> o.getId() == defaultId)
                        .findFirst()
                        .ifPresent(officerNameCombo::setValue);
            }

            storageLocations = lookupRepo.getStorageLocations();
            intakeLocations = lookupRepo.getIntakeLocations();
        } catch (Exception e) { throw new RuntimeException(e); }
        showIdleState();
        refreshCount();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);
    }

    // â”€â”€ Table setup â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void setupDropboxTable() {
        dbColBarcode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().evidence.getBarcode()));
        dbColCase.setCellValueFactory(cd -> new SimpleStringProperty(
                caseNumberById.getOrDefault(cd.getValue().evidence.getCaseId(), "")));
        dbColDescription.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().evidence.getDescription()));
        dbColType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().evidence.getEvidenceType()));
        dbColAction.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().action));

        // Action cell: "Check In" / "Mark Missing" buttons
        dbColAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnCheckIn  = new Button("Check In");
            private final Button btnKeepHere = new Button("Still In Dropbox");
            private final Button btnMissing  = new Button("Missing");
            {
                btnCheckIn.getStyleClass().add("btn-primary");
                btnKeepHere.getStyleClass().add("btn-secondary");
                btnMissing.getStyleClass().add("btn-secondary");
                btnCheckIn.setStyle("-fx-font-size:14; -fx-padding: 4 12 4 12;");
                btnKeepHere.setStyle("-fx-font-size:14; -fx-padding: 4 12 4 12;");
                btnMissing.setStyle("-fx-font-size:14; -fx-padding: 4 12 4 12;");
                btnCheckIn.setOnAction(e -> onCheckInItem(getTableView().getItems().get(getIndex())));
                btnKeepHere.setOnAction(e -> onKeepInDropbox(getTableView().getItems().get(getIndex())));
                btnMissing.setOnAction(e -> onMarkMissing(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                DropboxItem di = getTableView().getItems().get(getIndex());
                if ("Checked In".equals(di.action) || "Missing".equals(di.action) || "Verified".equals(di.action)) {
                    Label lbl = new Label(di.action);
                    String color = "#ef4444";
                    if ("Checked In".equals(di.action)) color = "#22c55e";
                    if ("Verified".equals(di.action)) color = "#3b82f6";
                    lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");
                    setGraphic(lbl);
                } else {
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnCheckIn, btnKeepHere, btnMissing);
                    setGraphic(box);
                }
            }
        });

        dropboxTable.setItems(sessionItems);
    }

    private void setupHistoryTable() {
        histColDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().createdAt));
        histColOfficer.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().officerName));
        histColItems.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().itemCount).asObject());

        historyTable.setRowFactory(tv -> {
            TableRow<DropboxSession> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && e.getClickCount() == 2 && !row.isEmpty()) {
                    showSessionDetail(row.getItem());
                }
            });
            return row;
        });
    }

    // â”€â”€ State management â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void showIdleState() {
        sessionActive = false;
        idlePane.setVisible(true);   idlePane.setManaged(true);
        activePane.setVisible(false); activePane.setManaged(false);
        historyPane.setVisible(false); historyPane.setManaged(false);
        sessionStateLabel.setText("");
    }

    private void showActiveState() {
        sessionActive = true;
        idlePane.setVisible(false);   idlePane.setManaged(false);
        activePane.setVisible(true);  activePane.setManaged(true);
        historyPane.setVisible(false); historyPane.setManaged(false);
        sessionStateLabel.setText("â— Session Active");
        sessionStateLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        updateActiveSummary();
    }

    private void showHistoryState() {
        idlePane.setVisible(false);   idlePane.setManaged(false);
        activePane.setVisible(false); activePane.setManaged(false);
        historyPane.setVisible(true); historyPane.setManaged(true);
        loadHistory();
    }

    private void updateActiveSummary() {
        long checkedIn = sessionItems.stream().filter(i -> "Checked In".equals(i.action)).count();
        long verified  = sessionItems.stream().filter(i -> "Verified".equals(i.action)).count();
        long missing   = sessionItems.stream().filter(i -> "Missing".equals(i.action)).count();
        long pending   = sessionItems.stream().filter(i -> "Pending".equals(i.action)).count();
        activeSummaryLabel.setText(sessionItems.size() + " items in session â€” "
            + checkedIn + " checked in, " + verified + " verified, " + missing + " missing, " + pending + " pending");
    }

    // â”€â”€ Actions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    private void onStartSession() {
        try {
            List<String> dropboxLocations = getApprovedDropboxLocations();
            List<Evidence> inDropbox = evidenceRepo.findAll().stream()
                    .filter(e -> "In Dropbox".equalsIgnoreCase(e.getStatus()))
                    .filter(e -> dropboxLocations.stream()
                            .anyMatch(l -> l.equalsIgnoreCase(nullToEmpty(e.getStorageLocation()))))
                    .toList();

            if (inDropbox.isEmpty()) {
            Dialogs.info("No evidence is currently assigned to an approved dropbox location.");
            refreshCount();
            showIdleState();
            return;
            }

            sessionItems.clear();
            for (Evidence e : inDropbox) sessionItems.add(new DropboxItem(e, "Pending"));
            showActiveState();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void onKeepInDropbox(DropboxItem item) {
        try {
            String existingLocation = nullToEmpty(item.evidence.getStorageLocation());
            evidenceRepo.updateStatus(item.evidence.getId(), "In Dropbox", existingLocation);

            ChainOfCustody coc = new ChainOfCustody();
            coc.setEvidenceId(item.evidence.getId());
            coc.setAction("Dropbox Verification");
            coc.setPerformedBy(getSelectedOfficerName());
            coc.setPerformedByName(getSelectedOfficerName());
            coc.setFromLocation(existingLocation);
            coc.setToLocation(existingLocation);
            coc.setNotes("Evidence Tech verified item remains in assigned dropbox.");
            cocRepo.addEntry(coc);

            item.action = "Verified";
            dropboxTable.refresh();
            updateActiveSummary();
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void onCheckInItem(DropboxItem item) {
        String chosen = pickStorageLocation("Check In Evidence",
                "Select storage location for: " + item.evidence.getBarcode());
        if (chosen == null) return;
        try {
            evidenceRepo.updateStatus(item.evidence.getId(), "In Storage", chosen);

            ChainOfCustody coc = new ChainOfCustody();
            coc.setEvidenceId(item.evidence.getId());
            coc.setAction("Check In");
            coc.setPerformedBy(getSelectedOfficerName());
            coc.setPerformedByName(getSelectedOfficerName());
            coc.setFromLocation(nullToEmpty(item.evidence.getStorageLocation()));
            coc.setToLocation(chosen);
            coc.setNotes("Checked in via Dropbox session");
            cocRepo.addEntry(coc);

            item.action = "Checked In";
            dropboxTable.refresh();
            updateActiveSummary();
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    /**
     * Searchable / sortable ListView picker for storage locations, mirroring the
     * "Associate Person with Case" dialog pattern.
     */
    private String pickStorageLocation(String title, String headerText) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        Dialogs.style(dialog);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField searchField = new TextField();
        searchField.setPromptText("Search storage locations\u2026");

        ObservableList<String> allLocations =
                FXCollections.observableArrayList(storageLocations);
        allLocations.sort(String.CASE_INSENSITIVE_ORDER);
        javafx.collections.transformation.FilteredList<String> filtered =
                new javafx.collections.transformation.FilteredList<>(allLocations, s -> true);

        ListView<String> list = new ListView<>(filtered);
        list.setPrefHeight(320);
        list.setPrefWidth(420);
        list.setPlaceholder(new Label("No storage locations match your search."));

        Label matchCount = new Label();
        Runnable updateCount = () ->
                matchCount.setText(filtered.size() + " of " + allLocations.size() + " locations");
        updateCount.run();
        filtered.addListener((javafx.collections.ListChangeListener<String>) c -> updateCount.run());

        searchField.textProperty().addListener((obs, o, n) -> {
            final String q = n == null ? "" : n.trim().toLowerCase();
            filtered.setPredicate(q.isEmpty() ? s -> true : s -> s != null && s.toLowerCase().contains(q));
            if (!filtered.isEmpty()) list.getSelectionModel().selectFirst();
        });

        if (!filtered.isEmpty()) list.getSelectionModel().selectFirst();
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
                if (ok != null) ok.fire();
            }
        });

        VBox root = new VBox(8,
                new Label("Search:"),
                searchField,
                list,
                matchCount);
        root.setPadding(new javafx.geometry.Insets(12));
        VBox.setVgrow(list, javafx.scene.layout.Priority.ALWAYS);
        dialog.getDialogPane().setContent(root);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());

        dialog.setResultConverter(bt -> bt == ButtonType.OK
                ? list.getSelectionModel().getSelectedItem() : null);

        javafx.application.Platform.runLater(searchField::requestFocus);
        return dialog.showAndWait().orElse(null);
    }

    private void onMarkMissing(DropboxItem item) {
        try {
            String actorName = getSelectedOfficerName();
            if (!isEvidenceTechOrAdmin(actorName)) {
                Dialogs.warn("Permission required",
                        "Only Evidence Tech or Admin may mark evidence as Missing. Enter a valid officer name.");
                return;
            }

            TextInputDialog noteDialog = new TextInputDialog();
            noteDialog.setTitle("Mark Evidence Missing");
            noteDialog.setHeaderText("Missing evidence requires an explanation note.");
            noteDialog.setContentText("Reason / Notes:");
            Dialogs.style(noteDialog);
            Optional<String> noteOpt = noteDialog.showAndWait();
            if (noteOpt.isEmpty() || noteOpt.get().trim().isEmpty()) {
                Dialogs.warn("Note required", "A note is required when marking evidence as Missing.");
                return;
            }

            String enteredBy = officerNameById.getOrDefault(item.evidence.getCollectedByOfficerId(), "Unknown Officer");
            String lastKnownLocation = nullToEmpty(item.evidence.getStorageLocation());
            String checkedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
            String generated = "Officer entered item as placed in " + lastKnownLocation
                    + ". Evidence Tech checked the dropbox on " + checkedAt
                    + " and item was not present.";
            String fullNotes = generated + " " + noteOpt.get().trim() + " (Entered by: " + enteredBy + ")";

            evidenceRepo.updateStatus(item.evidence.getId(), "Missing", item.evidence.getStorageLocation());

            ChainOfCustody coc = new ChainOfCustody();
            coc.setEvidenceId(item.evidence.getId());
            coc.setAction("Mark Missing");
            coc.setPerformedBy(actorName);
            coc.setPerformedByName(actorName);
            coc.setFromLocation(lastKnownLocation);
            coc.setNotes(fullNotes);
            cocRepo.addEntry(coc);

            item.action = "Missing";
            dropboxTable.refresh();
            updateActiveSummary();
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    @FXML
    private void onFinishSession() {
        long pending = sessionItems.stream().filter(i -> "Pending".equals(i.action)).count();
        if (pending > 0) {
            if (!Dialogs.confirm("Finish session?",
                    pending + " item(s) are still pending. Finish session anyway?")) {
                return;
            }
        }

        try {
            int checkedIn = (int) sessionItems.stream().filter(i -> "Checked In".equals(i.action)).count();
            String officer = getSelectedOfficerName();
            saveSession(officer, sessionItems.size(), serializeSessionItems());
            sessionItems.clear();
            refreshCount();
            showIdleState();
            Dialogs.info("Session complete", checkedIn + " items processed.");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @FXML
    private void onCancelSession() {
        sessionItems.clear();
        showIdleState();
    }

    @FXML
    private void onViewHistory() { showHistoryState(); }

    @FXML
    private void onBackFromHistory() { showIdleState(); }

    @FXML
    private void onPrintActive() {
        javafx.stage.Window w = dropboxTable.getScene() != null ? dropboxTable.getScene().getWindow() : null;
        com.evidenceharbor.util.PrintSheetUtil.printTable(w, "Dropbox Check-In Session", dropboxTable);
    }

    @FXML
    private void onPrintHistory() {
        javafx.stage.Window w = historyTable.getScene() != null ? historyTable.getScene().getWindow() : null;
        com.evidenceharbor.util.PrintSheetUtil.printTable(w, "Dropbox Session History", historyTable);
    }

    // â”€â”€ DB helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void refreshCount() {
        try {
            List<String> dropboxLocations = getApprovedDropboxLocations();
            long count = evidenceRepo.findAll().stream()
                    .filter(e -> "In Dropbox".equalsIgnoreCase(e.getStatus()))
                    .filter(e -> dropboxLocations.stream()
                            .anyMatch(l -> l.equalsIgnoreCase(nullToEmpty(e.getStorageLocation()))))
                    .filter(e -> !"Missing".equals(e.getStatus()))
                    .count();
            dropboxCountLabel.setText(String.valueOf(count));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void saveSession(String officerName, int itemCount, String itemsJson) throws SQLException {
        Connection conn = com.evidenceharbor.persistence.DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dropbox_sessions (officer_name, item_count, items_json) VALUES (?,?,?)")) {
            ps.setString(1, officerName.isEmpty() ? null : officerName);
            ps.setInt(2, itemCount);
            ps.setString(3, itemsJson);
            ps.executeUpdate();
        }
    }

    /** Format per line: barcode|case|description|type|action (pipe/newline escaped). */
    private String serializeSessionItems() {
        StringBuilder sb = new StringBuilder();
        for (DropboxItem it : sessionItems) {
            sb.append(esc(nullToEmpty(it.evidence.getBarcode()))).append('|')
              .append(esc(caseNumberById.getOrDefault(it.evidence.getCaseId(), ""))).append('|')
              .append(esc(nullToEmpty(it.evidence.getDescription()))).append('|')
              .append(esc(nullToEmpty(it.evidence.getEvidenceType()))).append('|')
              .append(esc(nullToEmpty(it.action))).append('\n');
        }
        return sb.toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace('|', '\u0001').replace('\n', '\u0002');
    }
    private static String unesc(String s) {
        return s == null ? "" : s.replace('\u0001', '|').replace('\u0002', '\n');
    }

    private List<String[]> parseSessionItems(String blob) {
        List<String[]> rows = new ArrayList<>();
        if (blob == null || blob.isBlank()) return rows;
        for (String line : blob.split("\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", -1);
            String[] row = new String[]{"", "", "", "", ""};
            for (int i = 0; i < row.length && i < parts.length; i++) row[i] = unesc(parts[i]);
            rows.add(row);
        }
        return rows;
    }

    private void showSessionDetail(DropboxSession s) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Dropbox Session — " + s.createdAt);
        dlg.setHeaderText("Session on " + s.createdAt
                + (s.officerName == null || s.officerName.isBlank() ? "" : " by " + s.officerName));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setPrefSize(820, 560);
        Dialogs.style(dlg);

        List<String[]> rows = parseSessionItems(s.itemsJson);

        TableView<String[]> tbl = new TableView<>();
        tbl.getStyleClass().add("data-table");
        String[] headers = {"BARCODE", "CASE #", "DESCRIPTION", "TYPE", "ACTION"};
        int[] widths = {140, 110, 260, 110, 120};
        for (int i = 0; i < headers.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(headers[i]);
            c.setPrefWidth(widths[i]);
            c.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue() != null && idx < cd.getValue().length ? cd.getValue()[idx] : ""));
            tbl.getColumns().add(c);
        }
        tbl.setItems(FXCollections.observableArrayList(rows));
        tbl.setPlaceholder(new Label(
                rows.isEmpty() && (s.itemsJson == null || s.itemsJson.isBlank())
                        ? "This session predates per-item history."
                        : "No items recorded."));

        // Summary label
        long checkedIn = rows.stream().filter(r -> "Checked In".equals(r[4])).count();
        long verified  = rows.stream().filter(r -> "Verified".equals(r[4])).count();
        long missing   = rows.stream().filter(r -> "Missing".equals(r[4])).count();
        long pending   = rows.stream().filter(r -> "Pending".equals(r[4])).count();
        Label summary = new Label(s.itemCount + " items — "
                + checkedIn + " checked in, " + verified + " verified, "
                + missing + " missing, " + pending + " pending");
        summary.setStyle("-fx-text-fill:#94a3b8;");

        Button printBtn = new Button("🖨 Print");
        printBtn.getStyleClass().add("btn-secondary");
        printBtn.setOnAction(ev -> printSessionDetail(s, rows));

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(12, summary, printBtn);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox root = new VBox(10, tbl, footer);
        root.setPadding(new javafx.geometry.Insets(10));
        VBox.setVgrow(tbl, javafx.scene.layout.Priority.ALWAYS);

        dlg.getDialogPane().setContent(root);
        dlg.showAndWait();
    }

    private void printSessionDetail(DropboxSession s, List<String[]> rows) {
        List<String[]> meta = new ArrayList<>();
        meta.add(new String[]{"Date", nullToEmpty(s.createdAt)});
        meta.add(new String[]{"Officer", nullToEmpty(s.officerName)});
        meta.add(new String[]{"Session ID", String.valueOf(s.id)});
        meta.add(new String[]{"Item Count", String.valueOf(s.itemCount)});

        String[] headers = {"BARCODE", "CASE #", "DESCRIPTION", "TYPE", "ACTION"};

        List<com.evidenceharbor.util.PrintSheetUtil.Section> sections = new ArrayList<>();
        sections.add(new com.evidenceharbor.util.PrintSheetUtil.KVSection("Session", meta));
        sections.add(new com.evidenceharbor.util.PrintSheetUtil.TableSection("Items", headers, rows));

        javafx.stage.Window w = historyTable.getScene() != null ? historyTable.getScene().getWindow() : null;
        com.evidenceharbor.util.PrintSheetUtil.print(w, "Dropbox Session " + s.createdAt, sections);
    }

    private void loadHistory() {
        try {
            Connection conn = com.evidenceharbor.persistence.DatabaseManager.getInstance().getConnection();
            List<DropboxSession> sessions = new ArrayList<>();
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT id, officer_name, item_count, items_json, created_at FROM dropbox_sessions ORDER BY created_at DESC")) {
                while (rs.next()) {
                    sessions.add(new DropboxSession(rs.getInt("id"),
                            rs.getString("officer_name") != null ? rs.getString("officer_name") : "—",
                            rs.getInt("item_count"),
                            rs.getString("created_at"),
                            rs.getString("items_json")));
                }
            }
            historyTable.setItems(FXCollections.observableArrayList(sessions));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // â”€â”€ Nav â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onBack()          { Navigator.get().showCaseList(); }
    @FXML private void onDashboard()     { Navigator.get().showCaseList(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    // â”€â”€ Helper records â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    static class DropboxItem {
        final Evidence evidence;
        String action;
        DropboxItem(Evidence e, String action) { this.evidence = e; this.action = action; }
    }

    static class DropboxSession {
        final int id;
        final String officerName;
        final int itemCount;
        final String createdAt;
        final String itemsJson;
        DropboxSession(int id, String officerName, int itemCount, String createdAt, String itemsJson) {
            this.id = id; this.officerName = officerName;
            this.itemCount = itemCount; this.createdAt = createdAt;
            this.itemsJson = itemsJson;
        }
    }

    private List<String> getApprovedDropboxLocations() {
        return intakeLocations;
    }

    private boolean isEvidenceTechOrAdmin(String officerName) {
        if (officerName == null || officerName.isBlank()) return false;
        try {
            Officer officer = officerRepo.findByName(officerName.trim());
            if (officer == null || officer.getRole() == null) return false;
            String role = officer.getRole().toLowerCase();
            return role.contains("admin") || role.contains("evidence tech") || role.contains("evidence_tech") || role.contains("tech");
        } catch (Exception e) {
            return false;
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String getSelectedOfficerName() {
        Officer officer = officerNameCombo == null ? null : officerNameCombo.getValue();
        return officer == null || officer.getName() == null ? "" : officer.getName().trim();
    }
}

