package com.evidenceharbor.ui.reports;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.persistence.ReportRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    @FXML private Button btnStatusReport;
    @FXML private Button btnTypeReport;
    @FXML private Button btnOfficerReport;
    @FXML private Button btnLocationReport;
    @FXML private Button btnCasesReport;

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

    @FXML private void onSelectStatusReport()   { selectReport("status",   "Evidence by Status",   false); }
    @FXML private void onSelectTypeReport()     { selectReport("type",     "Evidence by Type",     false); }
    @FXML private void onSelectOfficerReport()  { selectReport("officer",  "Evidence by Officer",  false); }
    @FXML private void onSelectLocationReport() { selectReport("location", "Evidence by Location", false); }
    @FXML private void onSelectCasesReport()    { selectReport("cases",    "Cases Summary",        true);  }

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
        for (Button b : new Button[]{btnStatusReport, btnTypeReport, btnOfficerReport, btnLocationReport, btnCasesReport}) {
            b.getStyleClass().removeAll("btn-primary");
            if (!b.getStyleClass().contains("btn-secondary")) b.getStyleClass().add("btn-secondary");
        }
        Button active = switch (type) {
            case "status"   -> btnStatusReport;
            case "type"     -> btnTypeReport;
            case "officer"  -> btnOfficerReport;
            case "location" -> btnLocationReport;
            case "cases"    -> btnCasesReport;
            default         -> null;
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

    @FXML
    private void onExportCsv() {
        if (currentHeaders.length == 0 || reportTable.getItems().isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report as CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName(selectedReport + "_report.csv");
        Window window = reportTable.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(String.join(",", currentHeaders) + "\n");
            for (ObservableList<String> row : reportTable.getItems()) {
                String line = row.stream()
                    .map(v -> "\"" + (v == null ? "" : v.replace("\"", "\"\"")) + "\"")
                    .reduce((a, b) -> a + "," + b).orElse("");
                fw.write(line + "\n");
            }
        } catch (IOException e) {
            System.err.println("CSV export failed: " + e.getMessage());
        }
    }

    @FXML private void onCases()         { Navigator.get().showCaseList(); }
    @FXML private void onInventory()     { Navigator.get().showInventory(); }
    @FXML private void onDropbox()       { Navigator.get().showDropbox(); }
    @FXML private void onReports()       { }
    @FXML private void onAdmin()         { Navigator.get().showAdminDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
}
