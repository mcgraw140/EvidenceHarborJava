package com.evidenceharbor.app;

import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.DatabaseManager;
import com.evidenceharbor.persistence.OfficerRepository;
import com.evidenceharbor.util.PasswordUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Single-window wizard that handles:
 *   Step 1 — Database connection (create or connect)
 *   Step 2 — Create first admin  (if no accounts exist)
 *            OR Sign in          (if accounts already exist)
 */
public class SetupWizard {

    private final Stage stage;

    // ── DB fields ──────────────────────────────────────────────────────────
    private final TextField     dbHostField     = new TextField("127.0.0.1");
    private final TextField     dbPortField     = new TextField("3306");
    private final TextField     dbNameField     = new TextField("evidence_harbor");
    private final TextField     dbUserField     = new TextField("root");
    private final PasswordField dbPassField     = new PasswordField();
    private final Button        createDbBtn     = new Button("Create DB + Connect");
    private final Button        connectDbBtn    = new Button("Connect to Existing DB");

    // ── Admin-creation fields ──────────────────────────────────────────────
    private final TextField     adminNameField  = new TextField();
    private final TextField     adminBadgeField = new TextField();
    private final TextField     adminUserField  = new TextField();
    private final PasswordField adminPassField  = new PasswordField();
    private final PasswordField adminConfField  = new PasswordField();
    private final Button        createAdminBtn  = new Button("Create Account & Sign In");

    // ── Login fields ───────────────────────────────────────────────────────
    private final TextField     loginUserField  = new TextField();
    private final PasswordField loginPassField  = new PasswordField();
    private final Button        loginBtn        = new Button("Sign In");
    private int failedAttempts = 0;

    // ── Shared UI ──────────────────────────────────────────────────────────
    private final Label stepLabel   = new Label();
    private final Label errorLabel  = new Label();
    private final Label statusLabel = new Label();
    private final VBox  contentBox  = new VBox(14);
    private       VBox  root;

    public SetupWizard(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        // Step indicator
        stepLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        Label brand = new Label("Evidence Harbor");
        brand.getStyleClass().add("brand-title");

        // Status row
        statusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#94a3b8; -fx-wrap-text:true;");
        statusLabel.setMaxWidth(480);

        // Error label
        errorLabel.setStyle("-fx-text-fill:#ef4444; -fx-font-size:13px; -fx-wrap-text:true;");
        errorLabel.setMaxWidth(480);
        errorLabel.setVisible(false);

        contentBox.setFillWidth(true);

        VBox card = new VBox(16, brand, stepLabel, new Separator(), contentBox, errorLabel, statusLabel);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(32));
        card.setMaxWidth(540);

        root = new VBox(card);
        root.getStyleClass().add("root-pane");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        VBox.setVgrow(card, Priority.NEVER);

        Scene scene = new Scene(root, 680, 580);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("Evidence Harbor — Setup");
        stage.setScene(scene);
        stage.show();

        // Auto-connect if saved config exists, skipping Step 1
        if (DatabaseManager.hasConfigFile()) {
            stepLabel.setText("Step 1 of 2  ·  Database Connection");
            setStatus("Connecting with saved settings...");
            contentBox.getChildren().clear();

            Task<Boolean> autoConnect = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    var props = DatabaseManager.loadSavedConfigSnapshot();
                    DatabaseManager.configureAndConnect(
                            props.getProperty("mariadb.host", "127.0.0.1"),
                            props.getProperty("mariadb.port", "3306"),
                            props.getProperty("mariadb.database", "evidence_harbor"),
                            props.getProperty("mariadb.user", "root"),
                            props.getProperty("mariadb.password", ""),
                            false);
                    OfficerRepository repo = new OfficerRepository();
                    return repo.hasPasswordAccounts();
                }
            };

            autoConnect.setOnSucceeded(e -> {
                boolean hasAccounts = autoConnect.getValue();
                if (hasAccounts) {
                    showLoginStep();
                } else {
                    showCreateAdminStep();
                }
            });

            autoConnect.setOnFailed(e -> {
                // Saved config failed — fall back to manual step 1
                showDbStep();
                showError("Auto-connect failed: " + rootMsg(autoConnect.getException()));
            });

            new Thread(autoConnect, "wizard-auto").start();
        } else {
            showDbStep();
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  STEP 1 — DATABASE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private void showDbStep() {
        stepLabel.setText("Step 1 of 2  ·  Database Connection");
        clearError();

        // Load saved values if they exist
        try {
            if (DatabaseManager.hasConfigFile()) {
                var props = DatabaseManager.loadSavedConfigSnapshot();
                dbHostField.setText(props.getProperty("mariadb.host", "127.0.0.1"));
                dbPortField.setText(props.getProperty("mariadb.port", "3306"));
                dbNameField.setText(props.getProperty("mariadb.database", "evidence_harbor"));
                dbUserField.setText(props.getProperty("mariadb.user", "root"));
                dbPassField.setText(props.getProperty("mariadb.password", ""));
            }
        } catch (Exception ignored) {}

        Label heading = new Label("Connect to MariaDB");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#dbeafe;");

        Label sub = new Label(DatabaseManager.hasConfigFile()
                ? "Saved connection found. You can adjust the details and reconnect."
                : "First-time setup. Fill in your MariaDB details and click \"Create DB + Connect\".");
        sub.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-wrap-text:true;");
        sub.setMaxWidth(480);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPrefWidth(150);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        styleField(dbHostField);  dbHostField.setPromptText("e.g. 127.0.0.1");
        styleField(dbPortField);  dbPortField.setPromptText("3306");
        styleField(dbNameField);  dbNameField.setPromptText("evidence_harbor");
        styleField(dbUserField);  dbUserField.setPromptText("root");
        styleField(dbPassField);  dbPassField.setPromptText("MariaDB root password");

        form.add(fl("Server Host"),    0, 0); form.add(dbHostField, 1, 0);
        form.add(fl("Port"),           0, 1); form.add(dbPortField, 1, 1);
        form.add(fl("Database Name"),  0, 2); form.add(dbNameField, 1, 2);
        form.add(fl("MariaDB User"),   0, 3); form.add(dbUserField, 1, 3);
        form.add(fl("Password"),       0, 4); form.add(dbPassField, 1, 4);

        createDbBtn.getStyleClass().add("btn-primary");
        connectDbBtn.getStyleClass().add("btn-secondary");
        createDbBtn.setOnAction(e -> doDbConnect(true));
        connectDbBtn.setOnAction(e -> doDbConnect(false));

        HBox btns = new HBox(10, connectDbBtn, createDbBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        contentBox.getChildren().setAll(heading, sub, form, btns);
        setStatus(DatabaseManager.hasConfigFile()
                ? "Saved settings loaded. Click Connect or update and reconnect."
                : "No saved configuration — enter your MariaDB details above.");
    }

    private void doDbConnect(boolean createDb) {
        String host = dbHostField.getText().trim();
        String port = dbPortField.getText().trim();
        String db   = dbNameField.getText().trim();
        String user = dbUserField.getText().trim();
        String pass = dbPassField.getText();

        if (host.isBlank() || port.isBlank() || db.isBlank() || user.isBlank()) {
            showError("Please fill in all connection fields.");
            return;
        }

        clearError();
        setStatus("Connecting to " + host + ":" + port + "...");
        createDbBtn.setDisable(true);
        connectDbBtn.setDisable(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                DatabaseManager.configureAndConnect(host, port, db, user, pass, createDb);
                OfficerRepository repo = new OfficerRepository();
                return repo.hasPasswordAccounts();
            }
        };

        task.setOnSucceeded(e -> {
            createDbBtn.setDisable(false);
            connectDbBtn.setDisable(false);
            boolean hasAccounts = task.getValue();
            setStatus("Connected successfully.");
            if (hasAccounts) {
                showLoginStep();
            } else {
                showCreateAdminStep();
            }
        });

        task.setOnFailed(e -> {
            createDbBtn.setDisable(false);
            connectDbBtn.setDisable(false);
            showError("Connection failed: " + rootMsg(task.getException()));
            setStatus("Check your settings and try again.");
        });

        new Thread(task, "wizard-db").start();
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  STEP 2a — CREATE FIRST ADMIN
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private void showCreateAdminStep() {
        stepLabel.setText("Step 2 of 2  ·  Create Administrator Account");
        clearError();

        Label heading = new Label("Create Your Admin Account");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#dbeafe;");

        Label sub = new Label("No accounts found. Create the first administrator to start using Evidence Harbor.");
        sub.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-wrap-text:true;");
        sub.setMaxWidth(480);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPrefWidth(160);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        styleField(adminNameField);  adminNameField.setPromptText("Full name");
        styleField(adminBadgeField); adminBadgeField.setPromptText("e.g. 1001");
        styleField(adminUserField);  adminUserField.setPromptText("Login username");
        styleField(adminPassField);  adminPassField.setPromptText("Minimum 8 characters");
        styleField(adminConfField);  adminConfField.setPromptText("Re-enter password");

        form.add(fl("Full Name"),         0, 0); form.add(adminNameField,  1, 0);
        form.add(fl("Badge Number"),      0, 1); form.add(adminBadgeField, 1, 1);
        form.add(fl("Username"),          0, 2); form.add(adminUserField,  1, 2);
        form.add(fl("Password"),          0, 3); form.add(adminPassField,  1, 3);
        form.add(fl("Confirm Password"),  0, 4); form.add(adminConfField,  1, 4);

        Button backBtn = new Button("â† Back");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> showDbStep());

        createAdminBtn.getStyleClass().add("btn-primary");
        createAdminBtn.setOnAction(e -> doCreateAdmin());

        HBox btns = new HBox(10, backBtn, createAdminBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        contentBox.getChildren().setAll(heading, sub, form, btns);
        setStatus("Database connected. Create your administrator account to continue.");
    }

    private void doCreateAdmin() {
        String name     = adminNameField.getText().trim();
        String badge    = adminBadgeField.getText().trim();
        String username = adminUserField.getText().trim();
        String password = adminPassField.getText();
        String confirm  = adminConfField.getText();

        if (name.isBlank() || badge.isBlank() || username.isBlank() || password.isBlank()) {
            showError("All fields are required."); return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters."); return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match."); return;
        }

        clearError();
        createAdminBtn.setDisable(true);
        setStatus("Creating account...");

        Task<Officer> task = new Task<>() {
            @Override
            protected Officer call() throws Exception {
                OfficerRepository repo = new OfficerRepository();
                if (repo.usernameExists(username, 0))
                    throw new RuntimeException("Username '" + username + "' is already taken.");
                Officer existing = repo.findByName(name);
                if (existing != null) {
                    existing.setBadge(badge);
                    existing.setUsername(username);
                    existing.setPasswordHash(PasswordUtils.hash(password));
                    existing.setRole("admin");
                    existing.setStatus("Active");
                    return repo.save(existing);
                }
                Officer o = new Officer();
                o.setName(name); o.setBadge(badge); o.setUsername(username);
                o.setPasswordHash(PasswordUtils.hash(password));
                o.setRole("admin"); o.setStatus("Active");
                return repo.save(o);
            }
        };

        task.setOnSucceeded(e -> {
            SessionManager.setCurrentOfficer(task.getValue());
            new Navigator(stage).showEvidenceDashboard();
        });

        task.setOnFailed(e -> {
            showError(rootMsg(task.getException()));
            createAdminBtn.setDisable(false);
            setStatus("");
        });

        new Thread(task, "wizard-admin").start();
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  STEP 2b — SIGN IN
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private void showLoginStep() {
        stepLabel.setText("Step 2 of 2  ·  Sign In");
        clearError();
        failedAttempts = 0;

        Label heading = new Label("Sign In");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#dbeafe;");

        Label sub = new Label("Enter your credentials to access Evidence Harbor.");
        sub.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPrefWidth(130);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        styleField(loginUserField); loginUserField.setPromptText("Username");
        styleField(loginPassField); loginPassField.setPromptText("Password");

        form.add(fl("Username"), 0, 0); form.add(loginUserField, 1, 0);
        form.add(fl("Password"), 0, 1); form.add(loginPassField, 1, 1);

        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> doLogin());

        HBox btns = new HBox(10, loginBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        contentBox.getChildren().setAll(heading, sub, form, btns);
        setStatus("Connected. Enter your credentials.");
        Platform.runLater(() -> loginUserField.requestFocus());
    }

    private void doLogin() {
        if (failedAttempts >= 5) {
            showError("Account locked after 5 failed attempts. Restart the application to try again.");
            return;
        }

        String username = loginUserField.getText().trim();
        String password = loginPassField.getText();

        if (username.isBlank() || password.isBlank()) {
            showError("Username and password are required."); return;
        }

        clearError();
        loginBtn.setDisable(true);
        setStatus("Signing in...");

        Task<Officer> task = new Task<>() {
            @Override
            protected Officer call() throws Exception {
                OfficerRepository repo = new OfficerRepository();
                Officer officer = repo.findByUsername(username);
                if (officer == null) return null;
                String stored = officer.getPasswordHash();
                if (stored == null || stored.isBlank()) return null;
                return PasswordUtils.verify(password, stored) ? officer : null;
            }
        };

        task.setOnSucceeded(e -> {
            Officer officer = task.getValue();
            if (officer != null) {
                SessionManager.setCurrentOfficer(officer);
                new Navigator(stage).showEvidenceDashboard();
            } else {
                failedAttempts++;
                int remaining = 5 - failedAttempts;
                if (remaining > 0) {
                    showError("Invalid username or password. " + remaining + " attempt(s) remaining.");
                } else {
                    showError("Account locked after 5 failed attempts. Restart to try again.");
                }
                loginPassField.clear();
                loginBtn.setDisable(false);
                setStatus("");
            }
        });

        task.setOnFailed(e -> {
            showError("Login error: " + rootMsg(task.getException()));
            loginBtn.setDisable(false);
            setStatus("");
        });

        new Thread(task, "wizard-login").start();
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  HELPERS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private void showError(String msg) {
        Platform.runLater(() -> { errorLabel.setText(msg); errorLabel.setVisible(true); });
    }

    private void clearError() {
        Platform.runLater(() -> errorLabel.setVisible(false));
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }

    private Label fl(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private void styleField(TextField f) {
        f.getStyleClass().add("text-field-default");
    }

    private String rootMsg(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}

