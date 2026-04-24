package com.evidenceharbor.ui.inventory;

import com.evidenceharbor.app.CurrentOfficerResolver;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Case;
import com.evidenceharbor.domain.ChainOfCustody;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.ChainOfCustodyRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import com.evidenceharbor.persistence.LookupRepository;
import com.evidenceharbor.persistence.OfficerRepository;
import com.evidenceharbor.util.Dialogs;
import com.evidenceharbor.util.PrintSheetUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class BatchCocTransferController implements Initializable {

    // Action → new status mapping (subset of single-item transfer; bank actions excluded)
    private static final Map<String, String> ACTION_STATUS = Map.ofEntries(
            Map.entry("Check In",             "In Custody"),
            Map.entry("Check Out to Person",  "Checked Out"),
            Map.entry("Submit for Analysis",  "Checked Out"),
            Map.entry("Agency Transfer",      "Checked Out"),
            Map.entry("Destroy",              "Destroyed"),
            Map.entry("Disburse",             "Disbursed"),
            Map.entry("Move to New Location", "In Storage"),
            Map.entry("Return to Owner",      "Returned to Owner")
    );

    @FXML private TextField scanField;
    @FXML private Label countLabel;
    @FXML private TableView<Evidence> batchTable;
    @FXML private TableColumn<Evidence, String> colBarcode;
    @FXML private TableColumn<Evidence, String> colCase;
    @FXML private TableColumn<Evidence, String> colDescription;
    @FXML private TableColumn<Evidence, String> colType;
    @FXML private TableColumn<Evidence, String> colStatus;
    @FXML private TableColumn<Evidence, String> colFrom;

    @FXML private ComboBox<String> actionCombo;
    @FXML private ComboBox<Officer> performedByCombo;
    @FXML private Label toLocationLabel;
    @FXML private ComboBox<String> toLocationCombo;
    @FXML private Label toPersonLabel;
    @FXML private TextField toPersonField;
    @FXML private TextField reasonField;
    @FXML private TextArea notesArea;

    private final EvidenceRepository evidenceRepo = new EvidenceRepository();
    private final ChainOfCustodyRepository cocRepo = new ChainOfCustodyRepository();
    private final OfficerRepository officerRepo = new OfficerRepository();
    private final LookupRepository lookupRepo = new LookupRepository();
    private final CaseRepository caseRepo = new CaseRepository();

    private final ObservableList<Evidence> items = FXCollections.observableArrayList();
    private final Map<Integer, String> caseNumberCache = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colBarcode.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getBarcode())));
        colCase.setCellValueFactory(cd -> new SimpleStringProperty(resolveCaseNumber(cd.getValue().getCaseId())));
        colDescription.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getDescription())));
        colType.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getEvidenceType())));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getStatus())));
        colFrom.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getStorageLocation())));

        batchTable.setItems(items);
        batchTable.setPlaceholder(new Label("Scan a barcode above to start building the batch."));
        items.addListener((javafx.collections.ListChangeListener<Evidence>) c -> updateCount());
        updateCount();

        // Action combo (filter out Bank actions; those need per-item amounts)
        try {
            List<String> allActions = lookupRepo.getTransferActions();
            List<String> filtered = new ArrayList<>();
            for (String a : allActions) {
                if (a == null) continue;
                if (a.startsWith("Bank ")) continue;
                filtered.add(a);
            }
            actionCombo.setItems(FXCollections.observableArrayList(filtered));
        } catch (Exception ignore) {
            actionCombo.setItems(FXCollections.observableArrayList(ACTION_STATUS.keySet()));
        }

        try {
            List<Officer> officers = officerRepo.findAll();
            performedByCombo.setItems(FXCollections.observableArrayList(officers));
            Officer def = CurrentOfficerResolver.resolveDefaultOfficer(officerRepo);
            if (def != null) {
                performedByCombo.getItems().stream()
                        .filter(o -> o.getId() == def.getId()).findFirst()
                        .ifPresent(o -> performedByCombo.getSelectionModel().select(o));
            }
        } catch (Exception ignore) {}

        actionCombo.valueProperty().addListener((o, a, b) -> updateConditionalFields(b));
        updateConditionalFields(null);

        // Enter key in scan field
        scanField.setOnAction(e -> onAddScan());
    }

    private void updateCount() {
        countLabel.setText(items.size() + " item" + (items.size() == 1 ? "" : "s") + " in batch");
    }

    private void updateConditionalFields(String action) {
        boolean showLocation = action == null || action.isEmpty()
                || "Check In".equals(action)
                || "Move to New Location".equals(action)
                || "Submit for Analysis".equals(action)
                || "Agency Transfer".equals(action);
        boolean showPerson = action != null && (
                "Check Out to Person".equals(action)
                || "Return to Owner".equals(action)
                || "Agency Transfer".equals(action));

        String locLabel = "To Location";
        String locPrompt = "Select location";
        List<String> options = java.util.Collections.emptyList();
        try {
            if ("Submit for Analysis".equals(action)) {
                locLabel = "Analysis Lab";
                locPrompt = "Select analysis lab";
                options = lookupRepo.getAnalysisLabs();
            } else if ("Agency Transfer".equals(action)) {
                locLabel = "Receiving Agency";
                locPrompt = "Select agency";
                options = lookupRepo.getOtherAgencies();
            } else {
                options = lookupRepo.getStorageLocations();
            }
        } catch (Exception ignored) {}
        toLocationLabel.setText(locLabel);
        toLocationCombo.setPromptText(locPrompt);
        toLocationCombo.setEditable(true);
        toLocationCombo.getItems().setAll(options);
        toLocationCombo.getEditor().clear();
        toLocationCombo.setValue(null);

        if ("Agency Transfer".equals(action)) {
            toPersonLabel.setText("Agency Contact");
            toPersonField.setPromptText("Contact name / badge #");
        } else if ("Return to Owner".equals(action)) {
            toPersonLabel.setText("Owner Name");
            toPersonField.setPromptText("Recipient");
        } else {
            toPersonLabel.setText("To Person");
            toPersonField.setPromptText("Recipient name (officer / person)");
        }

        toLocationLabel.setVisible(showLocation); toLocationLabel.setManaged(showLocation);
        toLocationCombo.setVisible(showLocation); toLocationCombo.setManaged(showLocation);
        toPersonLabel.setVisible(showPerson);     toPersonLabel.setManaged(showPerson);
        toPersonField.setVisible(showPerson);     toPersonField.setManaged(showPerson);
    }

    // ── Scan handling ────────────────────────────────────────────────────

    @FXML
    private void onAddScan() {
        String code = scanField.getText() == null ? "" : scanField.getText().trim();
        if (code.isEmpty()) return;
        try {
            Evidence ev = evidenceRepo.findByBarcode(code);
            if (ev == null) {
                Dialogs.warn("Not found", "No evidence item found with barcode: " + code);
                scanField.selectAll();
                scanField.requestFocus();
                return;
            }
            for (Evidence existing : items) {
                if (existing.getId() == ev.getId()) {
                    Dialogs.info("Already added", "That item is already in the batch.");
                    scanField.clear();
                    scanField.requestFocus();
                    return;
                }
            }
            items.add(ev);
            scanField.clear();
            scanField.requestFocus();
        } catch (Exception ex) {
            ex.printStackTrace();
            Dialogs.error(ex);
        }
    }

    @FXML
    private void onRemoveSelected() {
        Evidence sel = batchTable.getSelectionModel().getSelectedItem();
        if (sel != null) items.remove(sel);
    }

    @FXML
    private void onClearAll() {
        if (items.isEmpty()) return;
        if (Dialogs.confirm("Clear batch?", "Remove all " + items.size() + " items from the batch?"))
            items.clear();
    }

    // ── Apply transfer ───────────────────────────────────────────────────

    @FXML
    private void onApply() {
        if (items.isEmpty()) { Dialogs.warn("Empty batch", "Add at least one item."); return; }
        String action = actionCombo.getValue();
        if (action == null || action.isBlank()) {
            Dialogs.warn("Action required", "Please select an action."); return;
        }

        String toLocation = null;
        if (toLocationCombo.isVisible()) {
            String txt = toLocationCombo.getEditor().getText();
            if (txt == null || txt.isBlank()) txt = toLocationCombo.getValue();
            toLocation = (txt == null || txt.isBlank()) ? null : txt.trim();
        }
        String toPerson = null;
        if (toPersonField.isVisible()) {
            String txt = toPersonField.getText();
            toPerson = (txt == null || txt.trim().isEmpty()) ? null : txt.trim();
        }

        if ("Submit for Analysis".equals(action) && toLocation == null) {
            Dialogs.warn("Analysis lab required", "Please select an analysis lab."); return;
        }
        if ("Agency Transfer".equals(action) && toLocation == null) {
            Dialogs.warn("Agency required", "Please select the receiving agency."); return;
        }

        Officer officer = performedByCombo.getValue();
        String performedBy = officer == null ? "" : officer.getName();
        String reason = reasonField.getText() == null ? "" : reasonField.getText().trim();
        String notes = notesArea.getText() == null ? "" : notesArea.getText().trim();

        if (!Dialogs.confirm("Apply to batch?",
                "Apply \"" + action + "\" to " + items.size() + " item"
                        + (items.size() == 1 ? "" : "s") + "?")) return;

        int ok = 0;
        List<String> failed = new ArrayList<>();
        for (Evidence ev : new ArrayList<>(items)) {
            try {
                ChainOfCustody entry = new ChainOfCustody();
                entry.setEvidenceId(ev.getId());
                entry.setAction(action);
                entry.setPerformedBy(performedBy);
                entry.setPerformedByName(performedBy);
                entry.setFromLocation(ns(ev.getStorageLocation()));
                entry.setToLocation(toLocation);
                entry.setToPerson(toPerson);
                entry.setReason(reason);
                entry.setNotes(notes);
                cocRepo.addEntry(entry);

                String newStatus = ACTION_STATUS.getOrDefault(action, ev.getStatus());
                String newLocation = (toLocation != null && !toLocation.isBlank())
                        ? toLocation : ev.getStorageLocation();
                evidenceRepo.updateStatus(ev.getId(), newStatus, newLocation);
                ev.setStatus(newStatus);
                ev.setStorageLocation(newLocation);
                ok++;
            } catch (Exception ex) {
                ex.printStackTrace();
                failed.add(ns(ev.getBarcode()) + " — " + ex.getMessage());
            }
        }
        batchTable.refresh();

        StringBuilder msg = new StringBuilder();
        msg.append("Applied to ").append(ok).append(" of ").append(items.size()).append(" items.");
        if (!failed.isEmpty()) {
            msg.append("\n\nFailures:\n");
            for (String f : failed) msg.append(" • ").append(f).append("\n");
            Dialogs.warn("Batch complete with errors", msg.toString());
        } else {
            Dialogs.info("Batch applied", msg.toString());
        }
    }

    // ── Print barcode labels (ZPL to Zebra) ──────────────────────────────

    @FXML
    private void onPrintLabels() {
        if (items.isEmpty()) { Dialogs.warn("Empty batch", "Add at least one item."); return; }

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            Dialogs.warn("No Printers", "No print services found on this computer.");
            return;
        }
        List<PrintService> sorted = new ArrayList<>();
        for (PrintService ps : services) {
            String n = ps.getName().toLowerCase();
            if (n.contains("zebra") || n.contains("zlp") || n.contains("zpl")) sorted.add(0, ps);
            else sorted.add(ps);
        }

        Dialog<PrintService> dlg = new Dialog<>();
        dlg.setTitle("Print Barcode Labels");
        dlg.setHeaderText("Select Zebra printer (2\" × 1\" label) — will print "
                + items.size() + " label" + (items.size() == 1 ? "" : "s"));
        Dialogs.style(dlg);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<PrintService> combo = new ComboBox<>();
        combo.getItems().addAll(sorted);
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(PrintService item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(PrintService item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        combo.setPrefWidth(380);
        if (!sorted.isEmpty()) combo.getSelectionModel().selectFirst();

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10, combo);
        content.setPadding(new javafx.geometry.Insets(12));
        dlg.getDialogPane().setContent(content);
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? combo.getValue() : null);
        dlg.showAndWait().ifPresent(service -> {
            int ok = 0;
            List<String> failed = new ArrayList<>();
            for (Evidence ev : items) {
                try {
                    byte[] zpl = buildZpl(ev);
                    DocPrintJob job = service.createPrintJob();
                    Doc doc = new SimpleDoc(zpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                    job.print(doc, new HashPrintRequestAttributeSet());
                    ok++;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    failed.add(ns(ev.getBarcode()) + " — " + ex.getMessage());
                }
            }
            if (failed.isEmpty())
                Dialogs.info("Labels Sent", "Sent " + ok + " label" + (ok == 1 ? "" : "s")
                        + " to: " + service.getName());
            else
                Dialogs.warn("Some labels failed",
                        "Sent " + ok + " of " + items.size() + ". Failures:\n"
                                + String.join("\n", failed));
        });
    }

    private byte[] buildZpl(Evidence ev) {
        return com.evidenceharbor.util.LabelPrintUtil.build(
                ev.getScanCode(),
                ev.getBarcode(),
                resolveCaseNumber(ev.getCaseId()),
                ev.getCollectionDate(),
                ev.getStorageLocation());
    }

    // ── Print CoC sheet (combined summary HTML) ──────────────────────────

    @FXML
    private void onPrintSheet() {
        if (items.isEmpty()) { Dialogs.warn("Empty batch", "Add at least one item."); return; }

        String action = actionCombo.getValue() == null ? "(not set)" : actionCombo.getValue();
        Officer officer = performedByCombo.getValue();
        String performedBy = officer == null ? "" : officer.getName();
        String toLocation = toLocationCombo.getValue() == null
                ? (toLocationCombo.getEditor() == null ? "" : toLocationCombo.getEditor().getText())
                : toLocationCombo.getValue();
        String toPerson = toPersonField.getText() == null ? "" : toPersonField.getText();
        String reason = reasonField.getText() == null ? "" : reasonField.getText();
        String notes = notesArea.getText() == null ? "" : notesArea.getText();

        List<String[]> meta = new ArrayList<>();
        meta.add(new String[]{"Action", action});
        meta.add(new String[]{"Performed By", performedBy});
        if (toLocation != null && !toLocation.isBlank()) meta.add(new String[]{"To Location", toLocation});
        if (toPerson != null && !toPerson.isBlank())     meta.add(new String[]{"To Person", toPerson});
        if (reason != null && !reason.isBlank())         meta.add(new String[]{"Reason", reason});
        if (notes != null && !notes.isBlank())           meta.add(new String[]{"Notes", notes});
        meta.add(new String[]{"Item Count", String.valueOf(items.size())});

        String[] headers = {"Barcode", "Case #", "Description", "Type", "From", "To"};
        List<String[]> rows = new ArrayList<>();
        String toCol = (toLocation != null && !toLocation.isBlank()) ? toLocation
                : (toPerson != null && !toPerson.isBlank()) ? toPerson : "";
        for (Evidence ev : items) {
            rows.add(new String[]{
                    ns(ev.getBarcode()),
                    resolveCaseNumber(ev.getCaseId()),
                    ns(ev.getDescription()),
                    ns(ev.getEvidenceType()),
                    ns(ev.getStorageLocation()),
                    toCol
            });
        }

        List<PrintSheetUtil.Section> sections = new ArrayList<>();
        sections.add(new PrintSheetUtil.KVSection("Transfer Details", meta));
        sections.add(new PrintSheetUtil.TableSection("Items", headers, rows));

        javafx.stage.Window w = batchTable.getScene() != null ? batchTable.getScene().getWindow() : null;
        PrintSheetUtil.print(w, "Batch Chain of Custody — " + action, sections);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String resolveCaseNumber(int caseId) {
        if (caseId <= 0) return "";
        Integer key = caseId;
        if (caseNumberCache.containsKey(key)) return caseNumberCache.get(key);
        try {
            Case c = caseRepo.findById(caseId);
            String num = c == null ? "" : ns(c.getCaseNumber());
            caseNumberCache.put(key, num);
            return num;
        } catch (Exception ex) {
            caseNumberCache.put(key, "");
            return "";
        }
    }

    private static String ns(String s) { return s == null ? "" : s; }

    // ── Nav handlers ─────────────────────────────────────────────────────
    @FXML private void onCases()             { Navigator.get().showCaseList(); }
    @FXML private void onInventory()         { Navigator.get().showInventory(); }
    @FXML private void onPeople()            { Navigator.get().showPeople(); }
    @FXML private void onDropbox()           { Navigator.get().showDropbox(); }
    @FXML private void onReports()           { Navigator.get().showReports(); }
    @FXML private void onSettings()          { Navigator.get().showSettings(); }
    @FXML private void onAdmin()             { Navigator.get().showAdminDashboard(); }
    @FXML private void onImpound()           { Navigator.get().showImpoundLot(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onBack()              { Navigator.get().showInventory(); }
    @FXML private void onDashboard()         { Navigator.get().showEvidenceDashboard(); }
}
