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
        // Prompt for storage location
        ChoiceDialog<String> dialog = new ChoiceDialog<>(storageLocations.isEmpty() ? null : storageLocations.get(0), storageLocations);
        dialog.setTitle("Check In Evidence");
        dialog.setHeaderText("Select storage location for: " + item.evidence.getBarcode());
        dialog.setContentText("Storage Location:");
        Dialogs.style(dialog);
        dialog.showAndWait().ifPresent(loc -> {
            try {
                evidenceRepo.updateStatus(item.evidence.getId(), "In Storage", loc);

                ChainOfCustody coc = new ChainOfCustody();
                coc.setEvidenceId(item.evidence.getId());
                coc.setAction("Check In");
                coc.setPerformedBy(getSelectedOfficerName());
                coc.setPerformedByName(getSelectedOfficerName());
                coc.setFromLocation(nullToEmpty(item.evidence.getStorageLocation()));
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
            saveSession(officer, sessionItems.size());
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
                            rs.getString("officer_name") != null ? rs.getString("officer_name") : "â€”",
                            rs.getInt("item_count"),
                            rs.getString("created_at")));
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
        DropboxSession(int id, String officerName, int itemCount, String createdAt) {
            this.id = id; this.officerName = officerName;
            this.itemCount = itemCount; this.createdAt = createdAt;
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

