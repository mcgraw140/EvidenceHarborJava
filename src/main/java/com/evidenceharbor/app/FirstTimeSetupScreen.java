package com.evidenceharbor.app;

import com.evidenceharbor.domain.Officer;
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

public class FirstTimeSetupScreen {

    private final Stage stage;

    private final TextField nameField     = new TextField();
    private final TextField badgeField    = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField  = new PasswordField();
    private final PasswordField confirmField   = new PasswordField();
    private final Button createButton = new Button("Create Administrator Account");
    private final Label  errorLabel   = new Label();

    public FirstTimeSetupScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Label brand = new Label("Evidence Harbor");
        brand.getStyleClass().add("brand-title");

        Label heading = new Label("First-Time Setup");
        heading.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#dbeafe;");

        Label sub = new Label("No administrator accounts found. Create your first admin account to continue.");
        sub.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-wrap-text:true;");
        sub.setMaxWidth(440);

        GridPane form = buildForm();

        errorLabel.setStyle("-fx-text-fill:#ef4444; -fx-font-size:13px;");
        errorLabel.setVisible(false);

        createButton.getStyleClass().add("btn-primary");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(e -> attemptCreate());

        VBox card = new VBox(16, brand, heading, sub, form, errorLabel, createButton);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(32));
        card.setMaxWidth(500);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("root-pane");

        Scene scene = new Scene(root, 640, 560);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("Evidence Harbor — First-Time Setup");
        stage.setScene(scene);
        stage.show();
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPrefWidth(150);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        style(nameField);     nameField.setPromptText("Full name");
        style(badgeField);    badgeField.setPromptText("e.g. 1234");
        style(usernameField); usernameField.setPromptText("Login username");
        style(passwordField); passwordField.setPromptText("Minimum 8 characters");
        style(confirmField);  confirmField.setPromptText("Re-enter password");

        grid.add(label("Full Name"),        0, 0); grid.add(nameField,     1, 0);
        grid.add(label("Badge Number"),     0, 1); grid.add(badgeField,    1, 1);
        grid.add(label("Username"),         0, 2); grid.add(usernameField, 1, 2);
        grid.add(label("Password"),         0, 3); grid.add(passwordField, 1, 3);
        grid.add(label("Confirm Password"), 0, 4); grid.add(confirmField,  1, 4);

        return grid;
    }

    private void attemptCreate() {
        String name     = nameField.getText().trim();
        String badge    = badgeField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        if (name.isBlank() || badge.isBlank() || username.isBlank() || password.isBlank()) {
            showError("All fields are required.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        createButton.setDisable(true);
        clearError();

        Task<Officer> task = new Task<>() {
            @Override
            protected Officer call() throws Exception {
                OfficerRepository repo = new OfficerRepository();
                if (repo.usernameExists(username, 0)) {
                    throw new RuntimeException("Username '" + username + "' is already taken.");
                }
                // If an officer with this name already exists (e.g. from seed data),
                // update their record instead of inserting a duplicate.
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
                o.setName(name);
                o.setBadge(badge);
                o.setUsername(username);
                o.setPasswordHash(PasswordUtils.hash(password));
                o.setRole("admin");
                o.setStatus("Active");
                return repo.save(o);
            }
        };

        task.setOnSucceeded(e -> {
            Officer created = task.getValue();
            SessionManager.setCurrentOfficer(created);
            new LoginScreen(stage).show();
        });

        task.setOnFailed(e -> {
            showError(rootMessage(task.getException()));
            createButton.setDisable(false);
        });

        Thread t = new Thread(task, "first-time-setup");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        });
    }

    private void clearError() {
        errorLabel.setVisible(false);
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private void style(TextField f) {
        f.getStyleClass().add("text-field-default");
    }

    private String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }
}
