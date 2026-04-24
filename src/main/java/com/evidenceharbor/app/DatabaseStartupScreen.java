package com.evidenceharbor.app;

import com.evidenceharbor.persistence.DatabaseManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Properties;

public class DatabaseStartupScreen {

    private final Stage stage;
    private final Label statusIcon = new Label("●");
    private final Label statusText = new Label("Checking MariaDB connection...");
    private final TextArea logArea = new TextArea();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField();
    private final TextField databaseField = new TextField();
    private final TextField userField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button connectButton = new Button("Connect to Existing DB");
    private final Button createButton = new Button("Create DB + Connect");

    public DatabaseStartupScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        loadSavedValues();

        statusIcon.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:18px; -fx-font-weight:bold;");
        statusText.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:14px; -fx-font-weight:bold;");

        Label title = new Label("Evidence Harbor");
        title.getStyleClass().add("brand-title");

        Label subtitle = new Label("Database connection is required before the software can be used.");
        subtitle.getStyleClass().add("subtitle");

        HBox statusRow = new HBox(10, statusIcon, statusText);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPrefWidth(160);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        styleField(hostField);
        styleField(portField);
        styleField(databaseField);
        styleField(userField);
        styleField(passwordField);

        form.add(makeLabel("Server Host"), 0, 0); form.add(hostField, 1, 0);
        form.add(makeLabel("Port"), 0, 1); form.add(portField, 1, 1);
        form.add(makeLabel("Agency Database"), 0, 2); form.add(databaseField, 1, 2);
        form.add(makeLabel("MariaDB User"), 0, 3); form.add(userField, 1, 3);
        form.add(makeLabel("Root Password"), 0, 4); form.add(passwordField, 1, 4);

        connectButton.getStyleClass().add("btn-secondary");
        createButton.getStyleClass().add("btn-primary");
        connectButton.setOnAction(e -> browseForDbProps());
        createButton.setOnAction(e -> attemptConnection(true));

        HBox buttonRow = new HBox(12, connectButton, createButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Label logTitle = new Label("Connection Status / Errors");
        logTitle.getStyleClass().add("section-title");

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(12);
        logArea.setStyle("-fx-control-inner-background:#101827; -fx-text-fill:#dbeafe; -fx-highlight-fill:#2563eb; -fx-highlight-text-fill:white; -fx-border-color:#2d4059;");

        VBox card = new VBox(16, title, subtitle, statusRow, form, buttonRow, logTitle, logArea);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox root = new VBox(20, card, spacer);
        root.getStyleClass().add("root-pane");
        root.setPadding(new Insets(24));

        Scene scene = new Scene(root, 860, 640);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("Evidence Harbor — Database Startup");
        stage.setScene(scene);
        stage.show();

        appendLog("Startup check began.");
        if (DatabaseManager.hasConfigFile()) {
            appendLog("Saved settings found. Attempting to connect...");
            Platform.runLater(() -> attemptConnection(false));
        } else {
            appendLog("No saved configuration found.");
            appendLog("This appears to be a first-time setup.");
            appendLog("Fill in your MariaDB connection details and click \"Create DB + Connect\" to create the database and connect.");
            setStatus("First-time setup — click Create DB + Connect.", "#f59e0b");
        }
    }

    private Label makeLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void styleField(TextField field) {
        field.getStyleClass().add("text-field-default");
    }

    private void loadSavedValues() {
        try {
            Properties props = DatabaseManager.loadSavedConfigSnapshot();
            hostField.setText(props.getProperty("mariadb.host", "127.0.0.1"));
            portField.setText(props.getProperty("mariadb.port", "3306"));
            databaseField.setText(props.getProperty("mariadb.database", "evidence_harbor"));
            userField.setText(props.getProperty("mariadb.user", "root"));
            passwordField.setText(props.getProperty("mariadb.password", ""));
        } catch (Exception e) {
            hostField.setText("127.0.0.1");
            portField.setText("3306");
            databaseField.setText("evidence_harbor");
            userField.setText("root");
            passwordField.setText("");
            appendLog("Unable to read saved database settings: " + rootMessage(e));
        }
    }

    private void browseForDbProps() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Browse for db.properties file");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Properties Files", "*.properties"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Start in EvidenceHarbor config folder
        File initialDir = new File(System.getenv("USERPROFILE"), "EvidenceHarbor");
        if (initialDir.exists()) {
            chooser.setInitialDirectory(initialDir);
        }
        
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            try {
                // Load the selected properties file
                Properties props = new Properties();
                props.load(new java.io.FileInputStream(selected));
                hostField.setText(props.getProperty("mariadb.host", "127.0.0.1"));
                portField.setText(props.getProperty("mariadb.port", "3306"));
                databaseField.setText(props.getProperty("mariadb.database", "evidence_harbor"));
                userField.setText(props.getProperty("mariadb.user", "root"));
                passwordField.setText(props.getProperty("mariadb.password", ""));
                appendLog("Configuration loaded from: " + selected.getAbsolutePath());
                setStatus("Config loaded. Ready to connect.", "#3b82f6");
            } catch (Exception ex) {
                appendLog("Failed to load configuration: " + ex.getMessage());
                setStatus("Failed to load config file.", "#ef4444");
            }
        }
    }

    private void attemptConnection(boolean createDatabase) {
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String db = databaseField.getText().trim();
        String user = userField.getText().trim();
        String password = passwordField.getText();

        if (host.isBlank() || port.isBlank() || db.isBlank() || user.isBlank()) {
            setStatus("Missing required connection information.", "#ef4444");
            appendLog("Please complete host, port, database, and user fields.");
            return;
        }

        connectButton.setDisable(true);
        createButton.setDisable(true);
        setStatus("Connecting to MariaDB...", "#f59e0b");
        appendLog("Host: " + host + ":" + port + " | DB: " + db + " | User: " + user);
        appendLog(createDatabase ? "Create DB + Connect selected." : "Connect to Existing DB selected.");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Saving connection settings...");
                updateMessage("Opening MariaDB connection...");
                DatabaseManager.configureAndConnect(host, port, db, user, password, createDatabase);
                updateMessage("Connection successful.");
                return null;
            }
        };

        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && !newMsg.isBlank()) appendLog(newMsg);
        });

        task.setOnSucceeded(e -> {
            setStatus("Connected. Checking user accounts...", "#22c55e");
            appendLog("Connection established. Checking for user accounts...");
            promptDataDirIfNeeded();
            try {
                com.evidenceharbor.persistence.OfficerRepository repo =
                        new com.evidenceharbor.persistence.OfficerRepository();
                if (repo.hasPasswordAccounts()) {
                    appendLog("Accounts found. Showing login screen.");
                    new LoginScreen(stage).show();
                } else {
                    appendLog("No accounts found. Showing first-time setup.");
                    new FirstTimeSetupScreen(stage).show();
                }
            } catch (Exception ex) {
                appendLog("Account check failed — proceeding to login. " + ex.getMessage());
                new LoginScreen(stage).show();
            }
        });

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            setStatus("Connection failed.", "#ef4444");
            appendLog("Error: " + rootMessage(error));
            appendLog("Update the password or server details, then retry.");
            connectButton.setDisable(false);
            createButton.setDisable(false);
        });

        Thread worker = new Thread(task, "db-startup-connector");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * If this is the first successful connection and no data directory has been saved yet,
     * show a DirectoryChooser so the user can locate their MariaDB data folder.
     * Skippable — pressing Cancel just continues without setting it.
     */
    private void promptDataDirIfNeeded() {
        try {
            String saved = DatabaseManager.getSavedProperty("mariadb.data.dir", "");
            if (!saved.isBlank()) return; // already configured

            appendLog("First-time setup: please select your MariaDB data directory.");

            // Build a simple confirmation dialog first to explain what we want
            javafx.scene.control.Alert info = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            info.initOwner(stage);
            info.setTitle("Select Data Directory");
            info.setHeaderText("Where is your MariaDB data folder?");
            info.setContentText(
                    "If MariaDB is running on this PC, please select the folder where it stores database files.\n\n" +
                    "Typical path:  C:\\Program Files\\MariaDB <version>\\data\\evidence_harbor\n\n" +
                    "Click OK to browse, or Cancel to skip (you can set this later in Settings).");
            info.getButtonTypes().setAll(
                    javafx.scene.control.ButtonType.OK,
                    javafx.scene.control.ButtonType.CANCEL);
            java.util.Optional<javafx.scene.control.ButtonType> result = info.showAndWait();
            if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
                appendLog("Data directory selection skipped — set it later in Settings.");
                return;
            }

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select MariaDB Data Directory (evidence_harbor folder)");
            // Start at the most likely location
            File guess = new File("C:\\Program Files");
            if (guess.exists()) chooser.setInitialDirectory(guess);

            File chosen = chooser.showDialog(stage);
            if (chosen != null) {
                DatabaseManager.setProperty("mariadb.data.dir", chosen.getAbsolutePath());
                appendLog("Data directory saved: " + chosen.getAbsolutePath());
            } else {
                appendLog("No directory selected — set it later in Settings.");
            }
        } catch (Exception ex) {
            appendLog("Could not prompt for data directory: " + ex.getMessage());
        }
    }

    private void setStatus(String message, String color) {
        statusText.setText(message);
        statusIcon.setStyle("-fx-text-fill:" + color + "; -fx-font-size:18px; -fx-font-weight:bold;");
    }

    private void appendLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + System.lineSeparator()));
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }
}