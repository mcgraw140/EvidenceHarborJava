package com.evidenceharbor.ui.reports;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.persistence.ReportRepository;
import com.evidenceharbor.util.PrintSheetUtil;
import com.evidenceharbor.util.TableExportUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class ReportsController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navDropboxBtn;

    @FXML private Label statTotalEvidence;
    @FXML private Label statInCustody;
    @FXML private Label statTotalCases;
    @FXML private Label statDisposed;

    @FXML private Button btnAllReport;
    @FXML private Button btnStatusReport;
    @FXML private Button btnTypeReport;
    @FXML private Button btnOfficerReport;
    @FXML private Button btnLocationReport;
    @FXML private Button btnCasesReport;
    @FXML private Button btnCaseReport;

    @FXML private Label reportTitleLabel;
    @FXML private HBox dateFilterBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label reportCountLabel;
    @FXML private TableView<ObservableList<String>> reportTable;

    private final ReportRepository reportRepo = new ReportRepository();
    private String selectedReport = null;
    private String[] currentHeaders = new String[0];

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, navDropboxBtn);
        loadSummaryStats();
        // Double-click on a report row to drill into details
        reportTable.setRowFactory(tv -> {
            TableRow<ObservableList<String>> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) onDrillIntoRow(row.getItem());
            });
            return row;
        });
        // Auto-select status report on load
        onSelectStatusReport();
    }

    private void loadSummaryStats() {
        try {
            int[] stats = reportRepo.getSummaryStats();
            statTotalEvidence.setText(String.valueOf(stats[0]));
            statInCustody.setText(String.valueOf(stats[1]));
            statTotalCases.setText(String.valueOf(stats[2]));
            statDisposed.setText(String.valueOf(stats[3]));
        } catch (Exception e) {
            System.err.println("Failed to load summary stats: " + e.getMessage());
        }
    }

    @FXML private void onSelectAllReport()      { selectReport("all",       "All Evidence (Detail)", false); }
    @FXML private void onSelectStatusReport()   { selectReport("status",    "Evidence by Status",   false); }
    @FXML private void onSelectTypeReport()     { selectReport("type",      "Evidence by Type",     false); }
    @FXML private void onSelectOfficerReport()  { selectReport("officer",   "Evidence by Officer",  false); }
    @FXML private void onSelectLocationReport() { selectReport("location",  "Evidence by Location", false); }
    @FXML private void onSelectCasesReport()    { selectReport("cases",     "Cases Summary",        true);  }
    @FXML private void onSelectCaseReport()     { selectReport("casedetail","Case Detail Report",   false); }

    private void selectReport(String type, String title, boolean showDateFilter) {
        selectedReport = type;
        reportTitleLabel.setText(title);
        dateFilterBox.setVisible(showDateFilter);
        dateFilterBox.setManaged(showDateFilter);
        if (showDateFilter) {
            if (fromDatePicker.getValue() == null) fromDatePicker.setValue(LocalDate.now().minusMonths(3));
            if (toDatePicker.getValue() == null) toDatePicker.setValue(LocalDate.now());
        }
        reportTable.getItems().clear();
        reportTable.getColumns().clear();
        reportCountLabel.setText("");
        highlightSelected(type);
        onRunReport();
    }

    private void highlightSelected(String type) {
        for (Button b : new Button[]{btnAllReport, btnStatusReport, btnTypeReport, btnOfficerReport, btnLocationReport, btnCasesReport, btnCaseReport}) {
            if (b == null) continue;
            b.getStyleClass().removeAll("btn-primary");
            if (!b.getStyleClass().contains("btn-secondary")) b.getStyleClass().add("btn-secondary");
        }
        Button active = switch (type) {
            case "all"        -> btnAllReport;
            case "status"     -> btnStatusReport;
            case "type"       -> btnTypeReport;
            case "officer"    -> btnOfficerReport;
            case "location"   -> btnLocationReport;
            case "cases"      -> btnCasesReport;
            case "casedetail" -> btnCaseReport;
            default           -> null;
        };
        if (active != null) {
            active.getStyleClass().remove("btn-secondary");
            active.getStyleClass().add("btn-primary");
        }
    }

    @FXML
    private void onRunReport() {
        if (selectedReport == null) return;
        try {
            List<String[]> data;
            switch (selectedReport) {
                case "all" -> {
                    currentHeaders = ReportRepository.EVIDENCE_DETAIL_HEADERS;
                    data = reportRepo.evidenceDetailsAll();
                }
                case "status" -> {
                    currentHeaders = new String[]{"Status", "Count"};
                    data = reportRepo.evidenceByStatus();
                }
                case "type" -> {
                    currentHeaders = new String[]{"Evidence Type", "Count"};
                    data = reportRepo.evidenceByType();
                }
                case "officer" -> {
                    currentHeaders = new String[]{"Officer", "Badge", "Evidence Count"};
                    data = reportRepo.evidenceByOfficer();
                }
                case "location" -> {
                    currentHeaders = new String[]{"Storage Location", "Count"};
                    data = reportRepo.evidenceByLocation();
                }
                case "cases" -> {
                    LocalDate from = fromDatePicker.getValue() != null ? fromDatePicker.getValue() : LocalDate.now().minusMonths(3);
                    LocalDate to   = toDatePicker.getValue()   != null ? toDatePicker.getValue()   : LocalDate.now();
                    currentHeaders = new String[]{"Case #", "Incident Date", "Officer", "Badge", "Evidence Count", "Persons"};
                    data = reportRepo.casesSummary(from, to);
                }
                case "casedetail" -> {
                    currentHeaders = ReportRepository.CASE_REPORT_HEADERS;
                    data = reportRepo.allCasesForReport();
                }
                default -> { return; }
            }
            buildTable(currentHeaders, data);
        } catch (Exception e) {
            System.err.println("Report failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildTable(String[] headers, List<String[]> data) {
        reportTable.getColumns().clear();
        reportTable.getItems().clear();
        for (int i = 0; i < headers.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> tc = new TableColumn<>(headers[i]);
            tc.setCellValueFactory(cd ->
                new SimpleStringProperty(col < cd.getValue().size() ? cd.getValue().get(col) : ""));
            tc.setPrefWidth(160);
            reportTable.getColumns().add(tc);
        }
        ObservableList<ObservableList<String>> rows = FXCollections.observableArrayList();
        for (String[] row : data) {
            rows.add(FXCollections.observableArrayList(row));
        }
        reportTable.setItems(rows);
        reportCountLabel.setText(data.size() + " record" + (data.size() == 1 ? "" : "s"));
    }

    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { }
    @FXML private void onPeople()        { Navigator.get().showPeople(); }
    @FXML private void onEvidenceDashboard() { Navigator.get().showEvidenceDashboard(); }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onBack()          { Navigator.get().showCaseList(); }
    @FXML private void onDashboard()     { Navigator.get().showCaseList(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }

    @FXML
    private void onExportCsv() {
        if (reportTable.getItems().isEmpty()) return;
        String safeName = (selectedReport == null ? "report" : selectedReport) + ".csv";
        TableExportUtil.exportCsv(
                reportTable.getScene().getWindow(), safeName, currentHeaders, reportTable.getItems());
    }

    // ── Stat card drill-downs ─────────────────────────────────────────────
    @FXML private void onCardTotalEvidence(MouseEvent e) { showEvidenceList("All Evidence", safe(reportRepo::evidenceDetailsAll)); }
    @FXML private void onCardInCustody(MouseEvent e)     { showEvidenceList("Active / In Custody Evidence", safe(reportRepo::evidenceDetailsActive)); }
    @FXML private void onCardDisposed(MouseEvent e)      { showEvidenceList("Disposed Evidence", safe(reportRepo::evidenceDetailsDisposed)); }
    @FXML private void onCardTotalCases(MouseEvent e)    { showCaseList("All Cases", safe(reportRepo::caseDetailsAll)); }

    // ── Row-level drill-down from the main report table ───────────────────
    private void onDrillIntoRow(ObservableList<String> row) {
        if (selectedReport == null || row == null || row.isEmpty()) return;
        try {
            switch (selectedReport) {
                case "all"      -> showEvidenceList("Evidence for case: " + (row.size() > 1 ? row.get(1) : ""),
                                       reportRepo.evidenceDetailsForCase(row.size() > 1 ? row.get(1) : ""));
                case "status"   -> showEvidenceList("Evidence with status: " + row.get(0),
                                       reportRepo.evidenceDetailsByStatus(row.get(0)));
                case "type"     -> showEvidenceList("Evidence of type: " + row.get(0),
                                       reportRepo.evidenceDetailsByType(row.get(0)));
                case "location" -> showEvidenceList("Evidence at: " + row.get(0),
                                       reportRepo.evidenceDetailsByLocation(row.get(0)));
                case "officer"  -> showEvidenceList("Evidence collected by: " + row.get(0),
                                       reportRepo.evidenceDetailsByOfficer(row.get(0), row.size() > 1 ? row.get(1) : ""));
                case "cases"    -> showEvidenceList("Evidence for case: " + row.get(0),
                                       reportRepo.evidenceDetailsForCase(row.get(0)));
                case "casedetail" -> showCaseDetailReport(row.get(0));
                default -> { }
            }
        } catch (Exception ex) {
            System.err.println("Drill-down failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ── Detail dialog helpers ─────────────────────────────────────────────
    private void showEvidenceList(String title, List<String[]> data) {
        showDetailDialog(title, ReportRepository.EVIDENCE_DETAIL_HEADERS, data);
    }

    private void showCaseList(String title, List<String[]> data) {
        showDetailDialog(title, ReportRepository.CASE_DETAIL_HEADERS, data);
    }

    private void showDetailDialog(String title, String[] headers, List<String[]> data) {
        if (data == null) data = List.of();
        TableView<ObservableList<String>> table = new TableView<>();
        table.setPlaceholder(new Label("No records"));
        for (int i = 0; i < headers.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> tc = new TableColumn<>(headers[i]);
            tc.setCellValueFactory(cd ->
                new SimpleStringProperty(col < cd.getValue().size() ? cd.getValue().get(col) : ""));
            tc.setPrefWidth(150);
            table.getColumns().add(tc);
        }
        ObservableList<ObservableList<String>> rows = FXCollections.observableArrayList();
        for (String[] r : data) rows.add(FXCollections.observableArrayList(r));
        table.setItems(rows);

        Label header = new Label(title);
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#e2e8f0;");
        Label count = new Label(data.size() + " record" + (data.size() == 1 ? "" : "s"));
        count.getStyleClass().add("subtitle");

        Button exportBtn = new Button("Export CSV");
        exportBtn.getStyleClass().add("btn-secondary");
        Button printBtn = new Button("Print");
        printBtn.getStyleClass().add("btn-secondary");
        Button close = new Button("Close");
        close.getStyleClass().add("btn-secondary");

        HBox footer = new HBox(10, count, new javafx.scene.layout.Region(), exportBtn, printBtn, close);
        HBox.setHgrow(footer.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox content = new VBox(12, header, table, footer);
        content.getStyleClass().add("root-pane");
        content.setPadding(new Insets(16));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        Window owner = reportTable.getScene() != null ? reportTable.getScene().getWindow() : null;
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(title);
        Scene scene = new Scene(content, 900, 560);
        if (reportTable.getScene() != null) {
            scene.getStylesheets().addAll(reportTable.getScene().getStylesheets());
        }
        stage.setScene(scene);

        String safeName = title.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "") + ".csv";
        exportBtn.setOnAction(e ->
            TableExportUtil.exportCsv(stage, safeName, headers, table.getItems()));
        printBtn.setOnAction(e -> PrintSheetUtil.printTable(stage, title, table));
        close.setOnAction(e -> stage.close());
        stage.show();
    }

    @FunctionalInterface
    private interface SqlSupplier<T> { T get() throws Exception; }

    private <T> T safe(SqlSupplier<T> s) {
        try { return s.get(); }
        catch (Exception ex) {
            System.err.println("Query failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    // ── Case Detail Report dialog ─────────────────────────────────────────
    private void showCaseDetailReport(String caseNumber) {
        List<String[]> evidenceRows  = safe(() -> reportRepo.caseEvidenceDetail(caseNumber));
        List<String[]> personsRows   = safe(() -> reportRepo.casePersonsDetail(caseNumber));
        List<String[]> vehicleRows   = safe(() -> reportRepo.caseVehiclesDetail(caseNumber));
        if (evidenceRows == null) evidenceRows = List.of();
        if (personsRows  == null) personsRows  = List.of();
        if (vehicleRows  == null) vehicleRows  = List.of();

        // Title
        Label titleLabel = new Label("Case Report: " + caseNumber);
        titleLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#e2e8f0;");

        // Build section: label + table
        VBox content = new VBox(14, titleLabel);
        content.getStyleClass().add("root-pane");
        content.setPadding(new Insets(16));

        content.getChildren().add(sectionTable("Associated People (" + personsRows.size() + ")",
                ReportRepository.CASE_PERSONS_HEADERS, personsRows));
        content.getChildren().add(sectionTable("Evidence (" + evidenceRows.size() + ")",
                ReportRepository.CASE_EVIDENCE_HEADERS, evidenceRows));
        content.getChildren().add(sectionTable("Vehicles (" + vehicleRows.size() + ")",
                ReportRepository.CASE_VEHICLES_HEADERS, vehicleRows));

        Button exportEvidenceBtn = new Button("Export Evidence CSV");
        exportEvidenceBtn.getStyleClass().add("btn-secondary");
        Button exportPersonsBtn = new Button("Export Persons CSV");
        exportPersonsBtn.getStyleClass().add("btn-secondary");
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");

        HBox footer = new HBox(10, exportEvidenceBtn, exportPersonsBtn,
                new javafx.scene.layout.Region(), closeBtn);
        HBox.setHgrow(footer.getChildren().get(2), javafx.scene.layout.Priority.ALWAYS);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        content.getChildren().add(footer);

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        Window owner = reportTable.getScene() != null ? reportTable.getScene().getWindow() : null;
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Case Report — " + caseNumber);
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(content);
        scroll.setFitToWidth(true);
        Scene scene = new Scene(scroll, 1000, 680);
        if (reportTable.getScene() != null) scene.getStylesheets().addAll(reportTable.getScene().getStylesheets());
        stage.setScene(scene);

        final List<String[]> finalEvidenceRows = evidenceRows;
        final List<String[]> finalPersonsRows  = personsRows;
        exportEvidenceBtn.setOnAction(e ->
                TableExportUtil.exportCsv(stage, caseNumber + "_evidence.csv",
                        ReportRepository.CASE_EVIDENCE_HEADERS, toObservable(finalEvidenceRows)));
        exportPersonsBtn.setOnAction(e ->
                TableExportUtil.exportCsv(stage, caseNumber + "_persons.csv",
                        ReportRepository.CASE_PERSONS_HEADERS, toObservable(finalPersonsRows)));
        closeBtn.setOnAction(e -> stage.close());
        stage.show();
    }

    private VBox sectionTable(String sectionTitle, String[] headers, List<String[]> data) {
        Label lbl = new Label(sectionTitle);
        lbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#94a3b8; -fx-padding: 0 0 4 0;");
        TableView<ObservableList<String>> table = new TableView<>();
        table.setPlaceholder(new Label("No records"));
        table.setPrefHeight(data.isEmpty() ? 80 : Math.min(200, 35 + data.size() * 28));
        for (int i = 0; i < headers.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> tc = new TableColumn<>(headers[i]);
            tc.setCellValueFactory(cd ->
                    new SimpleStringProperty(col < cd.getValue().size() ? cd.getValue().get(col) : ""));
            tc.setPrefWidth(130);
            table.getColumns().add(tc);
        }
        table.setItems(toObservable(data));
        return new VBox(4, lbl, table);
    }

    private ObservableList<ObservableList<String>> toObservable(List<String[]> data) {
        ObservableList<ObservableList<String>> result = FXCollections.observableArrayList();
        for (String[] r : data) result.add(FXCollections.observableArrayList(r));
        return result;
    }
}
