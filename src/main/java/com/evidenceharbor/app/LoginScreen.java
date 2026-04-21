package com.evidenceharbor.app;

import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.OfficerRepository;
import com.evidenceharbor.util.PasswordUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginScreen {

    private static final int MAX_ATTEMPTS = 5;

    private final Stage stage;

    private final TextField     usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button        loginButton   = new Button("Sign In");
    private final Label         errorLabel    = new Label();

    private int failedAttempts = 0;

    public LoginScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Label brand = new Label("Evidence Harbor");
        brand.getStyleClass().add("brand-title");

        Label heading = new Label("Sign In");
        heading.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#dbeafe;");

        Label sub = new Label("Enter your credentials to access the system.");
        sub.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

        GridPane form = buildForm();

        errorLabel.setStyle("-fx-text-fill:#ef4444; -fx-font-size:13px; -fx-wrap-text:true;");
        errorLabel.setMaxWidth(380);
        errorLabel.setVisible(false);

        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> attemptLogin());

        VBox card = new VBox(16, brand, heading, sub, form, errorLabel, loginButton);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(32));
        card.setMaxWidth(440);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("root-pane");

        Scene scene = new Scene(root, 560, 440);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("Evidence Harbor — Sign In");
        stage.setScene(scene);
        stage.show();
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPrefWidth(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        usernameField.getStyleClass().add("text-field-default");
        usernameField.setPromptText("Username");
        passwordField.getStyleClass().add("text-field-default");
        passwordField.setPromptText("Password");

        grid.add(label("Username"), 0, 0); grid.add(usernameField, 1, 0);
        grid.add(label("Password"), 0, 1); grid.add(passwordField, 1, 1);

        return grid;
    }

    private void attemptLogin() {
        if (failedAttempts >= MAX_ATTEMPTS) {
            showError("Account locked after " + MAX_ATTEMPTS + " failed attempts. Restart the application to try again.");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showError("Username and password are required.");
            return;
        }

        loginButton.setDisable(true);
        clearError();

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
                failedAttempts = 0;
                Navigator nav = new Navigator(stage);
                nav.showCaseList();
            } else {
                failedAttempts++;
                int remaining = MAX_ATTEMPTS - failedAttempts;
                if (remaining > 0) {
                    showError("Invalid username or password. " + remaining + " attempt(s) remaining.");
                } else {
                    showError("Account locked after " + MAX_ATTEMPTS + " failed attempts. Restart the application to try again.");
                }
                passwordField.clear();
                loginButton.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            showError("Login error: " + rootMessage(task.getException()));
            loginButton.setDisable(false);
        });

        Thread t = new Thread(task, "login-worker");
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

    private String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }
}
