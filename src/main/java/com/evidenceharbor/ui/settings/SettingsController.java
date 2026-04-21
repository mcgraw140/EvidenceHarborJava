package com.evidenceharbor.ui.settings;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.AgencySettings;
import com.evidenceharbor.persistence.DatabaseManager;
import com.evidenceharbor.persistence.SettingsRepository;
import com.evidenceharbor.util.TailscaleManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import java.awt.Desktop;
import java.io.File;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SettingsController implements Initializable {

    // Agency info
    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;

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

    // Tailscale tab
    @FXML private TabPane      tabPane;
    @FXML private Label        tsStatusDot;
    @FXML private Label        tsStatusLabel;
    @FXML private Label        tsIpLabel;
    @FXML private VBox         tsInstallBox;
    @FXML private VBox         tsConnectBox;
    @FXML private PasswordField tsAuthKeyField;
    @FXML private TextArea     tsLogArea;

    // Database connection tab
    @FXML private Label        dbStatusDot;
    @FXML private Label        dbStatusLabel;
    @FXML private TextField    dbHostField;
    @FXML private TextField    dbPortField;
    @FXML private TextField    dbNameField;
    @FXML private TextField    dbUserField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Label        dbTestResultLabel;
    @FXML private TextArea     dbLogArea;
    @FXML private TextField    dbDataDirField;

    private final SettingsRepository settingsRepo = new SettingsRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAgencySettings();
        wirePatternValidation();
        wireBarcodePreview();
        loadDbConnectionSettings();
        refreshDbStatus();
        refreshTailscaleStatus();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, null);
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

    // ──────────────────────── TAILSCALE ────────────────────────

    private void refreshTailscaleStatus() {
        runTailscaleTask(() -> TailscaleManager.getStatus(), result -> {
            TailscaleManager.TailscaleStatus s = (TailscaleManager.TailscaleStatus) result;
            if (!TailscaleManager.isInstalled()) {
                tsStatusDot.setStyle("-fx-text-fill:#ef4444; -fx-font-size:20px; -fx-font-weight:bold;");
                tsStatusLabel.setText("Tailscale not installed");
                tsIpLabel.setText("");
                tsInstallBox.setVisible(true);
                tsInstallBox.setManaged(true);
                tsConnectBox.setVisible(false);
                tsConnectBox.setManaged(false);
            } else if (s.running) {
                tsStatusDot.setStyle("-fx-text-fill:#22c55e; -fx-font-size:20px; -fx-font-weight:bold;");
                tsStatusLabel.setText("Tailscale connected");
                tsIpLabel.setText(s.selfIp != null ? "Your IP: " + s.selfIp : "IP unavailable");
                tsInstallBox.setVisible(false);
                tsInstallBox.setManaged(false);
                tsConnectBox.setVisible(true);
                tsConnectBox.setManaged(true);
            } else {
                tsStatusDot.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:20px; -fx-font-weight:bold;");
                tsStatusLabel.setText("Tailscale installed but not connected");
                tsIpLabel.setText("");
                tsInstallBox.setVisible(false);
                tsInstallBox.setManaged(false);
                tsConnectBox.setVisible(true);
                tsConnectBox.setManaged(true);
            }
            appendTsLog("[" + timestamp() + "] Status: " + (s.rawOutput.isBlank() ? "No output" : s.rawOutput));
        });
    }

    @FXML
    private void onTailscaleRefresh() {
        refreshTailscaleStatus();
    }

    @FXML
    private void onInstallTailscale() {
        appendTsLog("[" + timestamp() + "] Launching Tailscale installer (silent)...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Process proc = TailscaleManager.launchInstaller();
                proc.waitFor();
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            appendTsLog("[" + timestamp() + "] Installer finished. Refreshing status...");
            refreshTailscaleStatus();
        });
        task.setOnFailed(e -> appendTsLog("[" + timestamp() + "] Install error: " + rootMsg(task.getException())));
        new Thread(task, "ts-installer").start();
    }

    @FXML
    private void onTailscaleConnect() {
        String key = tsAuthKeyField.getText();
        appendTsLog("[" + timestamp() + "] Connecting Tailscale" + (key.isBlank() ? " (browser login)..." : " with auth key..."));
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return TailscaleManager.connect(key);
            }
        };
        task.setOnSucceeded(e -> {
            appendTsLog("[" + timestamp() + "] " + task.getValue());
            refreshTailscaleStatus();
        });
        task.setOnFailed(e -> appendTsLog("[" + timestamp() + "] Error: " + rootMsg(task.getException())));
        new Thread(task, "ts-connect").start();
    }

    @FXML
    private void onTailscaleDisconnect() {
        appendTsLog("[" + timestamp() + "] Disconnecting Tailscale...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return TailscaleManager.disconnect();
            }
        };
        task.setOnSucceeded(e -> {
            appendTsLog("[" + timestamp() + "] " + task.getValue());
            refreshTailscaleStatus();
        });
        task.setOnFailed(e -> appendTsLog("[" + timestamp() + "] Error: " + rootMsg(task.getException())));
        new Thread(task, "ts-disconnect").start();
    }

    @FXML
    private void onUseTailscaleIpAsDbHost() {
        try {
            TailscaleManager.TailscaleStatus s = TailscaleManager.getStatus();
            if (s.selfIp != null && !s.selfIp.isBlank()) {
                dbHostField.setText(s.selfIp);
                appendTsLog("[" + timestamp() + "] DB Host set to Tailscale IP: " + s.selfIp);
                // Switch to Database Connection tab (index 2)
                tabPane.getSelectionModel().select(2);
            } else {
                appendTsLog("[" + timestamp() + "] Could not get Tailscale IP. Are you connected?");
            }
        } catch (Exception e) {
            appendTsLog("[" + timestamp() + "] Error: " + rootMsg(e));
        }
    }

    private <T> void runTailscaleTask(java.util.concurrent.Callable<T> action, java.util.function.Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> appendTsLog("[" + timestamp() + "] Error: " + rootMsg(task.getException())));
        new Thread(task, "ts-task").start();
    }

    private void appendTsLog(String msg) {
        Platform.runLater(() -> {
            if (tsLogArea != null) tsLogArea.appendText(msg + System.lineSeparator());
        });
    }

    // ──────────────────────── DATABASE CONNECTION ────────────────────────

    private void loadDbConnectionSettings() {
        try {
            Properties props = DatabaseManager.getLiveConfig();
            dbHostField.setText(props.getProperty("mariadb.host", "127.0.0.1"));
            dbPortField.setText(props.getProperty("mariadb.port", "3306"));
            dbNameField.setText(props.getProperty("mariadb.database", "evidence_harbor"));
            dbUserField.setText(props.getProperty("mariadb.user", "root"));
            dbPasswordField.setText(props.getProperty("mariadb.password", ""));
            String savedDir = props.getProperty("mariadb.data.dir", "");
            dbDataDirField.setText(savedDir);
        } catch (Exception e) {
            appendDbLog("Failed to load saved connection settings: " + e.getMessage());
        }
    }

    private void refreshDbStatus() {
        boolean connected = DatabaseManager.isConnected();
        String color = connected ? "#22c55e" : "#ef4444";
        String msg   = connected ? "Connected to MariaDB" : "Not connected";
        dbStatusDot.setStyle("-fx-text-fill:" + color + "; -fx-font-size:20px; -fx-font-weight:bold;");
        dbStatusLabel.setText(msg);
    }

    @FXML
    private void onTestConnection() {
        runDbTask(false, true);
    }

    @FXML
    private void onSaveReconnect() {
        runDbTask(false, false);
    }

    private void runDbTask(boolean createDb, boolean testOnly) {
        String host     = dbHostField.getText().trim();
        String port     = dbPortField.getText().trim();
        String db       = dbNameField.getText().trim();
        String user     = dbUserField.getText().trim();
        String password = dbPasswordField.getText();

        if (host.isBlank() || port.isBlank() || db.isBlank() || user.isBlank()) {
            dbTestResultLabel.setText("Please fill in all connection fields.");
            dbTestResultLabel.setTextFill(Color.RED);
            return;
        }

        dbTestResultLabel.setText("Connecting...");
        dbTestResultLabel.setTextFill(Color.ORANGE);
        appendDbLog("[" + timestamp() + "] Attempting " + (testOnly ? "test" : "reconnect") + " to " + host + ":" + port + "/" + db);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (!testOnly) {
                    DatabaseManager.configureAndConnect(host, port, db, user, password, createDb);
                } else {
                    // Test-only: just try opening a raw connection without switching the live instance
                    String url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                            + "?useUnicode=true&characterEncoding=utf8&connectTimeout=5000";
                    java.sql.DriverManager.getConnection(url, user, password).close();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            String msg = testOnly ? "Test successful — connection is working." : "Reconnected successfully.";
            dbTestResultLabel.setText(msg);
            dbTestResultLabel.setTextFill(Color.GREEN);
            appendDbLog("[" + timestamp() + "] " + msg);
            refreshDbStatus();
            // Persist the data directory setting alongside the connection
            String dataDir = dbDataDirField.getText().trim();
            try {
                DatabaseManager.setProperty("mariadb.data.dir", dataDir);
            } catch (Exception ex) {
                appendDbLog("[" + timestamp() + "] Warning: could not save data dir: " + ex.getMessage());
            }
        });

        task.setOnFailed(e -> {
            Throwable err = task.getException();
            String msg = rootMsg(err);
            dbTestResultLabel.setText("Failed: " + msg);
            dbTestResultLabel.setTextFill(Color.RED);
            appendDbLog("[" + timestamp() + "] ERROR: " + msg);
            refreshDbStatus();
        });

        Thread t = new Thread(task, "db-settings-connect");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onBrowseDataDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select MariaDB Data Directory");
        String current = dbDataDirField.getText().trim();
        if (!current.isBlank()) {
            File f = new File(current);
            if (f.exists()) chooser.setInitialDirectory(f);
        }
        File selected = chooser.showDialog(dbDataDirField.getScene().getWindow());
        if (selected != null) {
            dbDataDirField.setText(selected.getAbsolutePath());
            try {
                DatabaseManager.setProperty("mariadb.data.dir", selected.getAbsolutePath());
                appendDbLog("[" + timestamp() + "] Data directory saved: " + selected.getAbsolutePath());
            } catch (Exception e) {
                appendDbLog("[" + timestamp() + "] Warning: could not save data dir: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onOpenDataDir() {
        String path = dbDataDirField.getText().trim();
        if (path.isBlank()) {
            dbTestResultLabel.setText("Set a data directory path first.");
            dbTestResultLabel.setTextFill(Color.ORANGE);
            return;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dbTestResultLabel.setText("Directory not found: " + path);
            dbTestResultLabel.setTextFill(Color.RED);
            return;
        }
        try {
            Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            // Fallback: open parent
            try {
                new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
            } catch (Exception ex) {
                dbTestResultLabel.setText("Could not open Explorer: " + ex.getMessage());
                dbTestResultLabel.setTextFill(Color.RED);
            }
        }
    }

    private void appendDbLog(String msg) {
        Platform.runLater(() -> {
            if (dbLogArea != null) dbLogArea.appendText(msg + System.lineSeparator());
        });
    }

    private String timestamp() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private String rootMsg(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

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
