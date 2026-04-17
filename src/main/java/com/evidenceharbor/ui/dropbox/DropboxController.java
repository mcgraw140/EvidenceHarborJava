package com.evidenceharbor.ui.dropbox;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.ChainOfCustody;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.ChainOfCustodyRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import com.evidenceharbor.persistence.LookupRepository;
import com.evidenceharbor.persistence.OfficerRepository;
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
import java.util.stream.Collectors;

public class DropboxController implements Initializable {

    // ── FXML bindings ────────────────────────────────────────────────────────
    @FXML private Label dropboxCountLabel;
    @FXML private Label sessionStateLabel;
    @FXML private VBox idlePane;
    @FXML private VBox activePane;
    @FXML private VBox historyPane;
    @FXML private TextField officerNameField;
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

    // ── Repos ────────────────────────────────────────────────────────────────
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final CaseRepository caseRepo = new CaseRepository();
    private final ChainOfCustodyRepository cocRepo   = new ChainOfCustodyRepository();
    private final LookupRepository lookupRepo = new LookupRepository();
    private final OfficerRepository officerRepo = new OfficerRepository();

    private final Map<Integer, String> caseNumberById = new HashMap<>();
    private final Map<Integer, String> officerNameById = new HashMap<>();
    private List<String> storageLocations = List.of();
    private final ObservableList<DropboxItem> sessionItems = FXCollections.observableArrayList();
    private boolean sessionActive = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDropboxTable();
        setupHistoryTable();
        try {
            for (var c : caseRepo.findAll()) caseNumberById.put(c.getId(), c.getCaseNumber());
            for (var o : officerRepo.findAll()) officerNameById.put(o.getId(), o.getName());
            storageLocations = lookupRepo.getStorageLocations();
        } catch (Exception e) { throw new RuntimeException(e); }
        showIdleState();
        refreshCount();
    }

    // ── Table setup ──────────────────────────────────────────────────────────

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
                btnCheckIn.setStyle("-fx-font-size:11; -fx-padding: 2 8 2 8;");
                btnKeepHere.setStyle("-fx-font-size:11; -fx-padding: 2 8 2 8;");
                btnMissing.setStyle("-fx-font-size:11; -fx-padding: 2 8 2 8;");
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
    }

    // ── State management ─────────────────────────────────────────────────────

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
        sessionStateLabel.setText("● Session Active");
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
        activeSummaryLabel.setText(sessionItems.size() + " items in session — "
            + checkedIn + " checked in, " + verified + " verified, " + missing + " missing, " + pending + " pending");
    }

    // ── Actions ──────────────────────────────────────────────────────────────

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
            new Alert(Alert.AlertType.INFORMATION,
                "No evidence is currently assigned to an approved dropbox location.").showAndWait();
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
            coc.setPerformedBy(officerNameField.getText().trim());
            coc.setPerformedByName(officerNameField.getText().trim());
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
        // Prompt for storage location
        ChoiceDialog<String> dialog = new ChoiceDialog<>(storageLocations.isEmpty() ? null : storageLocations.get(0), storageLocations);
        dialog.setTitle("Check In Evidence");
        dialog.setHeaderText("Select storage location for: " + item.evidence.getBarcode());
        dialog.setContentText("Storage Location:");
        dialog.showAndWait().ifPresent(loc -> {
            try {
                evidenceRepo.updateStatus(item.evidence.getId(), "In Custody", loc);

                ChainOfCustody coc = new ChainOfCustody();
                coc.setEvidenceId(item.evidence.getId());
                coc.setAction("Check In");
                coc.setPerformedBy(officerNameField.getText().trim());
                coc.setPerformedByName(officerNameField.getText().trim());
                coc.setFromLocation("Dropbox");
                coc.setToLocation(loc);
                coc.setNotes("Checked in via Dropbox session");
                cocRepo.addEntry(coc);

                item.action = "Checked In";
                dropboxTable.refresh();
                updateActiveSummary();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }

    private void onMarkMissing(DropboxItem item) {
        try {
            String actorName = officerNameField.getText() == null ? "" : officerNameField.getText().trim();
            if (!isEvidenceTechOrAdmin(actorName)) {
                new Alert(Alert.AlertType.WARNING,
                        "Only Evidence Tech or Admin may mark evidence as Missing. Enter a valid officer name.")
                        .showAndWait();
                return;
            }

            TextInputDialog noteDialog = new TextInputDialog();
            noteDialog.setTitle("Mark Evidence Missing");
            noteDialog.setHeaderText("Missing evidence requires an explanation note.");
            noteDialog.setContentText("Reason / Notes:");
            Optional<String> noteOpt = noteDialog.showAndWait();
            if (noteOpt.isEmpty() || noteOpt.get().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "A note is required when marking evidence as Missing.").showAndWait();
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    pending + " item(s) are still pending. Finish session anyway?",
                    ButtonType.YES, ButtonType.NO);
            if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        try {
            int checkedIn = (int) sessionItems.stream().filter(i -> "Checked In".equals(i.action)).count();
            String officer = officerNameField.getText().trim();
            saveSession(officer, sessionItems.size());
            sessionItems.clear();
            refreshCount();
            showIdleState();
            new Alert(Alert.AlertType.INFORMATION, "Session complete. " + checkedIn + " items processed.").showAndWait();
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

    // ── DB helpers ───────────────────────────────────────────────────────────

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

    private void saveSession(String officerName, int itemCount) throws SQLException {
        Connection conn = com.evidenceharbor.persistence.DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dropbox_sessions (officer_name, item_count) VALUES (?,?)")) {
            ps.setString(1, officerName.isEmpty() ? null : officerName);
            ps.setInt(2, itemCount);
            ps.executeUpdate();
        }
    }

    private void loadHistory() {
        try {
            Connection conn = com.evidenceharbor.persistence.DatabaseManager.getInstance().getConnection();
            List<DropboxSession> sessions = new ArrayList<>();
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT id, officer_name, item_count, created_at FROM dropbox_sessions ORDER BY created_at DESC")) {
                while (rs.next()) {
                    sessions.add(new DropboxSession(rs.getInt("id"),
                            rs.getString("officer_name") != null ? rs.getString("officer_name") : "—",
                            rs.getInt("item_count"),
                            rs.getString("created_at")));
                }
            }
            historyTable.setItems(FXCollections.observableArrayList(sessions));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── Nav ──────────────────────────────────────────────────────────────────
    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onDropbox()       { }
    @FXML private void onReports()       { Navigator.get().showReports(); }
    @FXML private void onSettings()      { Navigator.get().showSettings(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onQuartermaster() { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    // ── Helper records ───────────────────────────────────────────────────────

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
        DropboxSession(int id, String officerName, int itemCount, String createdAt) {
            this.id = id; this.officerName = officerName;
            this.itemCount = itemCount; this.createdAt = createdAt;
        }
    }

    private List<String> getApprovedDropboxLocations() {
        return storageLocations.stream().filter(this::looksLikeDropboxLocation).collect(Collectors.toList());
    }

    private boolean looksLikeDropboxLocation(String location) {
        if (location == null) return false;
        String v = location.toLowerCase();
        return v.contains("dropbox") || v.contains("drop box") || v.contains("locker");
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
}

