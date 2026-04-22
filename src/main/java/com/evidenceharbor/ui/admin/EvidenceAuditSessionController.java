package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.domain.EvidenceAudit;
import com.evidenceharbor.persistence.EvidenceAuditRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;

import java.util.*;

public class EvidenceAuditSessionController {

    @FXML private Label breadcrumb;
    @FXML private Label titleLabel;
    @FXML private Label metaType, metaScope, metaCreatedBy, metaCreatedAt, metaStatus;
    @FXML private Label summaryTotal, summaryFound, summaryMissing, summaryPending;
    @FXML private TextField searchField;
    @FXML private TextField scanField;
    @FXML private Button saveBtn, completeBtn, editBtn, printBtn;

    @FXML private TableView<AuditRow> itemsTable;
    @FXML private TableColumn<AuditRow, String> colBarcode, colType, colDesc, colLocation, colSysStatus, colResult, colNotes;
    @FXML private TableColumn<AuditRow, Void> colAction;

    private final EvidenceAuditRepository auditRepo = new EvidenceAuditRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();

    private EvidenceAudit audit;
    private final ObservableList<AuditRow> allRows = FXCollections.observableArrayList();
    private FilteredList<AuditRow> filtered;

    private static final List<String> RESULTS = Arrays.asList("Pending", "Found", "Missing");

    public void setAudit(EvidenceAudit a) {
        this.audit = a;
        renderMeta();
        loadItems();
        setLocked("Completed".equalsIgnoreCase(a.getStatus()));
    }

    private void setLocked(boolean locked) {
        saveBtn.setDisable(locked);
        completeBtn.setDisable(locked);
        itemsTable.setEditable(!locked);
        if (scanField != null) scanField.setDisable(locked);
        if (editBtn != null) {
            editBtn.setVisible(locked);
            editBtn.setManaged(locked);
        }
    }

    @FXML
    private void onEdit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Re-open this completed audit for editing?\n\n"
                        + "The audit will be set back to 'In Progress' until you click 'Complete Audit' again.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Edit Completed Audit");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    auditRepo.reopen(audit.getId());
                    audit.setStatus("In Progress");
                    audit.setCompletedAt(null);
                    metaStatus.setText("In Progress");
                    setLocked(false);
                } catch (Exception e) { showError(e); }
            }
        });
    }

    @FXML
    private void onPrint() {
        String title = "Evidence Audit #" + audit.getId()
                + "  —  " + nullSafe(audit.getAuditType())
                + (audit.getScope() != null && !audit.getScope().isBlank()
                        ? " (" + audit.getScope() + ")" : "")
                + "  —  Status: " + nullSafe(audit.getStatus());
        javafx.stage.Window win = itemsTable.getScene() != null
                ? itemsTable.getScene().getWindow() : null;
        com.evidenceharbor.util.TableExportUtil.printTable(win, title, itemsTable);
    }

    @FXML
    public void initialize() {
        colBarcode.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().evidence.getBarcode() != null ? cd.getValue().evidence.getBarcode() : "—"));
        colType.setCellValueFactory(cd -> new SimpleStringProperty(
                nullSafe(cd.getValue().evidence.getEvidenceType())));
        colDesc.setCellValueFactory(cd -> new SimpleStringProperty(
                nullSafe(cd.getValue().evidence.getDescription())));
        colLocation.setCellValueFactory(cd -> new SimpleStringProperty(
                nullSafe(cd.getValue().evidence.getStorageLocation())));
        colSysStatus.setCellValueFactory(cd -> new SimpleStringProperty(
                nullSafe(cd.getValue().evidence.getStatus())));

        colResult.setCellValueFactory(cd -> cd.getValue().resultProp);
        colResult.setCellFactory(col -> {
            ComboBoxTableCell<AuditRow, String> cell = new ComboBoxTableCell<>(
                    FXCollections.observableArrayList(RESULTS)) {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else if ("Found".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill:#1e7a3a; -fx-font-weight:bold;");
                    } else if ("Missing".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill:#b02020; -fx-font-weight:bold;");
                    } else {
                        setStyle("-fx-text-fill:#555;");
                    }
                }
            };
            return cell;
        });
        colResult.setOnEditCommit(ev -> {
            ev.getRowValue().resultProp.set(ev.getNewValue());
            updateSummary();
            itemsTable.refresh();
        });

        colNotes.setCellValueFactory(cd -> cd.getValue().notesProp);
        colNotes.setCellFactory(TextFieldTableCell.forTableColumn());
        colNotes.setOnEditCommit(ev -> ev.getRowValue().notesProp.set(ev.getNewValue()));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button foundBtn   = new Button("✓ Found");
            private final Button missingBtn = new Button("✗ Missing");
            private final Button notesBtn   = new Button("📝");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(8, foundBtn, missingBtn, notesBtn);
            {
                String neutral = "-fx-background-color:#e5e7eb; -fx-text-fill:#111; "
                        + "-fx-font-size:12; -fx-padding:6 14 6 14; -fx-background-radius:4;";
                foundBtn.setStyle(neutral);
                missingBtn.setStyle(neutral);
                notesBtn.setStyle("-fx-background-color:#e5e7eb; -fx-text-fill:#111; "
                        + "-fx-font-size:12; -fx-padding:6 10 6 10; -fx-background-radius:4;");
                notesBtn.setTooltip(new Tooltip("Edit notes"));
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                foundBtn.setOnAction(e -> {
                    AuditRow r = getTableView().getItems().get(getIndex());
                    r.resultProp.set("Found");
                    updateSummary();
                    itemsTable.refresh();
                });
                missingBtn.setOnAction(e -> {
                    AuditRow r = getTableView().getItems().get(getIndex());
                    r.resultProp.set("Missing");
                    updateSummary();
                    itemsTable.refresh();
                });
                notesBtn.setOnAction(e -> {
                    AuditRow r = getTableView().getItems().get(getIndex());
                    openNotesDialog(r);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        filtered = new FilteredList<>(allRows, r -> true);
        itemsTable.setItems(filtered);
    }

    private void openNotesDialog(AuditRow row) {
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Notes — " + (row.evidence.getBarcode() != null ? row.evidence.getBarcode() : "item"));
        dlg.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());
        dlg.setHeaderText(nullSafe(row.evidence.getDescription())
                + "\nLocation: " + nullSafe(row.evidence.getStorageLocation()));
        TextArea ta = new TextArea(row.notesProp.get());
        ta.setPrefColumnCount(48);
        ta.setPrefRowCount(8);
        ta.setWrapText(true);
        ta.setPromptText("Add notes about this item (condition, discrepancy, location found, etc.)");
        javafx.scene.layout.VBox vb = new javafx.scene.layout.VBox(10, ta);
        vb.setPadding(new Insets(12));
        dlg.getDialogPane().setContent(vb);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dlg.setResultConverter(bt -> bt == save ? ta.getText() : null);
        dlg.showAndWait().ifPresent(txt -> {
            row.notesProp.set(txt);
            itemsTable.refresh();
        });
    }

    @FXML
    private void onScanFound()   { applyScan("Found"); }
    @FXML
    private void onScanMissing() { applyScan("Missing"); }

    private void applyScan(String result) {
        if (scanField == null) return;
        String q = scanField.getText() == null ? "" : scanField.getText().trim();
        if (q.isEmpty()) return;
        AuditRow match = null;
        // Exact barcode match first
        for (AuditRow r : allRows) {
            if (q.equalsIgnoreCase(r.evidence.getBarcode())) { match = r; break; }
        }
        // Fall back to partial match if no exact
        if (match == null) {
            for (AuditRow r : allRows) {
                if (r.evidence.getBarcode() != null
                        && r.evidence.getBarcode().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))) {
                    match = r; break;
                }
            }
        }
        if (match == null) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "Barcode \"" + q + "\" is not in this audit's item list.\n\n"
                            + "If it should be, check the audit scope.",
                    ButtonType.OK);
            a.setHeaderText("Not in audit");
            a.showAndWait();
            return;
        }
        match.resultProp.set(result);
        updateSummary();
        itemsTable.refresh();
        // Scroll into view + select
        int idx = itemsTable.getItems().indexOf(match);
        if (idx >= 0) {
            itemsTable.getSelectionModel().select(idx);
            itemsTable.scrollTo(idx);
        }
        scanField.clear();
        scanField.requestFocus();
    }

    @FXML
    private void onResetAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Reset all items to Pending? This clears Found/Missing marks (notes are kept).",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Reset Results");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                for (AuditRow r : allRows) r.resultProp.set("Pending");
                updateSummary();
                itemsTable.refresh();
            }
        });
    }

    @FXML
    private void onFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(r -> {
            if (q.isEmpty()) return true;
            Evidence e = r.evidence;
            return contains(e.getBarcode(), q)
                    || contains(e.getEvidenceType(), q)
                    || contains(e.getDescription(), q)
                    || contains(e.getStorageLocation(), q)
                    || contains(e.getStatus(), q);
        });
    }

    private boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private void renderMeta() {
        titleLabel.setText("Audit #" + audit.getId() + " — " + audit.getAuditType());
        metaType.setText(nullSafe(audit.getAuditType()));
        metaScope.setText(nullSafe(audit.getScope()));
        metaCreatedBy.setText(nullSafe(audit.getCreatedBy()));
        metaCreatedAt.setText(nullSafe(audit.getCreatedAt()));
        metaStatus.setText(nullSafe(audit.getStatus()));
    }

    private void loadItems() {
        try {
            List<Evidence> all = evidenceRepo.findAll();
            List<Evidence> selected = filterByScope(all, audit.getAuditType(), audit.getScope());

            // Apply any previously-saved results
            Map<Integer, String[]> prior = parseItems(audit.getItemsJson());

            allRows.clear();
            for (Evidence e : selected) {
                String[] saved = prior.get(e.getId());
                String result = saved != null ? saved[0] : "Pending";
                String notes  = saved != null ? saved[1] : "";
                allRows.add(new AuditRow(e, result, notes));
            }
            updateSummary();
        } catch (Exception e) {
            showError(e);
        }
    }

    private List<Evidence> filterByScope(List<Evidence> all, String type, String scope) {
        if (type == null) return all;
        switch (type) {
            case "Random": {
                int pct = parsePercent(scope, 25);
                int count = Math.max(1, (int) Math.ceil(all.size() * (pct / 100.0)));
                List<Evidence> shuffled = new ArrayList<>(all);
                Collections.shuffle(shuffled, new Random(audit.getId()));
                return shuffled.subList(0, Math.min(count, shuffled.size()));
            }
            case "Location": {
                if (scope == null || scope.isBlank() || "Full".equalsIgnoreCase(scope)) return all;
                String needle = scope.toLowerCase(Locale.ROOT);
                List<Evidence> out = new ArrayList<>();
                for (Evidence e : all) {
                    if (e.getStorageLocation() != null
                            && e.getStorageLocation().toLowerCase(Locale.ROOT).contains(needle)) {
                        out.add(e);
                    }
                }
                return out;
            }
            case "Full":
            default:
                return all;
        }
    }

    private int parsePercent(String scope, int fallback) {
        if (scope == null) return fallback;
        String digits = scope.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return fallback;
        try { int v = Integer.parseInt(digits); return (v <= 0 || v > 100) ? fallback : v; }
        catch (NumberFormatException ex) { return fallback; }
    }

    /** Format: "id|result|notes" per line, notes have '|' -> '\u0001' and '\n' -> '\u0002'. */
    private Map<Integer, String[]> parseItems(String blob) {
        Map<Integer, String[]> map = new HashMap<>();
        if (blob == null || blob.isBlank()) return map;
        for (String line : blob.split("\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", 3);
            if (parts.length < 2) continue;
            try {
                int id = Integer.parseInt(parts[0]);
                String result = parts[1];
                String notes = parts.length >= 3
                        ? parts[2].replace('\u0001', '|').replace('\u0002', '\n')
                        : "";
                map.put(id, new String[] { result, notes });
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private String serializeItems() {
        StringBuilder sb = new StringBuilder();
        for (AuditRow r : allRows) {
            String notes = (r.notesProp.get() == null ? "" : r.notesProp.get())
                    .replace('|', '\u0001').replace('\n', '\u0002');
            sb.append(r.evidence.getId()).append('|')
              .append(r.resultProp.get() == null ? "Pending" : r.resultProp.get())
              .append('|').append(notes).append('\n');
        }
        return sb.toString();
    }

    private void updateSummary() {
        int total = allRows.size();
        int found = 0, missing = 0, pending = 0;
        for (AuditRow r : allRows) {
            String v = r.resultProp.get();
            if ("Found".equals(v)) found++;
            else if ("Missing".equals(v)) missing++;
            else pending++;
        }
        summaryTotal.setText("Total: " + total);
        summaryFound.setText("Found: " + found);
        summaryMissing.setText("Missing: " + missing);
        summaryPending.setText("Pending: " + pending);
    }

    @FXML
    private void onSave() {
        try {
            auditRepo.updateItems(audit.getId(), serializeItems());
            audit.setItemsJson(serializeItems());
            new Alert(Alert.AlertType.INFORMATION, "Progress saved.").showAndWait();
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void onComplete() {
        int pending = 0;
        for (AuditRow r : allRows) {
            if (!"Found".equals(r.resultProp.get()) && !"Missing".equals(r.resultProp.get())) pending++;
        }
        String msg = pending > 0
                ? pending + " item(s) are still pending. Complete anyway?"
                : "Mark this audit as complete?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Complete Audit");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    auditRepo.updateItems(audit.getId(), serializeItems());
                    auditRepo.complete(audit.getId());
                    Navigator.get().showEvidenceAudit();
                } catch (Exception e) { showError(e); }
            }
        });
    }

    private void showError(Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK);
        a.setHeaderText("Audit Session Error");
        a.showAndWait();
    }

    private static String nullSafe(String s) { return s == null || s.isBlank() ? "—" : s; }

    // ── Nav ───────────────────────────────────────────────────────────────────
    @FXML private void onCases()                 { Navigator.get().showCaseList(); }
    @FXML private void onImpound()               { Navigator.get().showImpoundLot(); }
    @FXML private void onAdmin()                 { Navigator.get().showAdminDashboard(); }
    @FXML private void onBack()                  { Navigator.get().showEvidenceAudit(); }
    @FXML private void onDashboard()             { Navigator.get().showAdminDashboard(); }
    @FXML private void onUserManagement()        { Navigator.get().showUserManagement(); }
    @FXML private void onLookupAdministration()  { Navigator.get().showLookupAdmin(); }
    @FXML private void onEvidenceAudit()         { Navigator.get().showEvidenceAudit(); }
    @FXML private void onAuditTrail()            { Navigator.get().showAuditTrail(); }
    @FXML private void onBankAccountLedger()     { Navigator.get().showBankAccountLedger(); }
    @FXML private void onSettings()              { Navigator.get().showSettings(); }

    // ── Row wrapper ───────────────────────────────────────────────────────────
    public static class AuditRow {
        final Evidence evidence;
        final SimpleStringProperty resultProp;
        final SimpleStringProperty notesProp;
        AuditRow(Evidence e, String result, String notes) {
            this.evidence = e;
            this.resultProp = new SimpleStringProperty(result == null ? "Pending" : result);
            this.notesProp  = new SimpleStringProperty(notes == null ? "" : notes);
        }
    }
}
