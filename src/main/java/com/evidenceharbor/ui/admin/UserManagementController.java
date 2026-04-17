package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.OfficerRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<Officer> userTable;
    @FXML private TableColumn<Officer, String> colName;
    @FXML private TableColumn<Officer, String> colBadge;
    @FXML private TableColumn<Officer, String> colUsername;
    @FXML private TableColumn<Officer, String> colRole;
    @FXML private TableColumn<Officer, String> colStatus;
    @FXML private TableColumn<Officer, String> colActions;

    private final OfficerRepository officerRepo = new OfficerRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Name column — shows name + optional External badge
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        colName.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) { setGraphic(null); setText(null); return; }
                Officer o = getTableView().getItems().get(getIndex());
                HBox box = new HBox(8);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label nameLbl = new Label(name);
                nameLbl.setStyle("-fx-text-fill: white;");
                box.getChildren().add(nameLbl);
                if (o.isExternal()) {
                    Label badge = new Label("External");
                    badge.setStyle("-fx-background-color: #3a506b; -fx-text-fill: #7eb3d4; " +
                            "-fx-padding: 2 8 2 8; -fx-background-radius: 4; -fx-font-size: 11;");
                    box.getChildren().add(badge);
                }
                setGraphic(box);
                setText(null);
            }
        });

        colBadge.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getBadge() != null ? cd.getValue().getBadge() : ""));
        colUsername.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getUsername() != null ? cd.getValue().getUsername() : ""));
        colRole.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getRole() != null ? cd.getValue().getRole() : "officer"));

        // Status column — green pill for Active
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStatus() != null ? cd.getValue().getStatus() : "Active"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); setText(null); return; }
                Label lbl = new Label(status);
                String color = "Active".equals(status) ? "#27ae60" : "#c0392b";
                lbl.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 4; -fx-font-size: 11;");
                setGraphic(lbl);
                setText(null);
            }
        });

        // Actions column — Edit + Permissions + Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn  = new Button("Edit");
            private final Button permsBtn = new Button("Permissions");
            private final Button delBtn   = new Button("Delete");
            {
                editBtn.setStyle("-fx-font-size:11; -fx-padding: 2 8 2 8;");
                permsBtn.setStyle("-fx-font-size:11; -fx-padding: 2 8 2 8;");
                delBtn.setStyle("-fx-font-size:11; -fx-padding: 2 8 2 8; -fx-background-color:#c0392b; -fx-text-fill:white;");
                editBtn.setOnAction(e  -> showUserDialog(getTableView().getItems().get(getIndex())));
                permsBtn.setOnAction(e -> Navigator.get().showPermissions(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e   -> deleteUser(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, editBtn, permsBtn, delBtn);
                setGraphic(box);
            }
        });

        loadUsers(null);
    }

    @FXML
    private void onSearch() {
        loadUsers(searchField.getText().trim());
    }

    @FXML
    private void onAddUser() {
        showUserDialog(null);
    }

    private void loadUsers(String query) {
        try {
            List<Officer> list = (query == null || query.isBlank())
                    ? officerRepo.findAll()
                    : officerRepo.search(query);
            userTable.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) { showError(e); }
    }

    private void showUserDialog(Officer existing) {
        boolean isNew = existing == null;
        Officer target = isNew ? new Officer() : existing;

        Dialog<Officer> dlg = new Dialog<>();
        dlg.setTitle(isNew ? "Add User" : "Edit User");
        dlg.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField     = new TextField(target.getName() != null ? target.getName() : "");
        TextField badgeField    = new TextField(target.getBadge() != null ? target.getBadge() : "");
        TextField usernameField = new TextField(target.getUsername() != null ? target.getUsername() : "");
        PasswordField passwordField = new PasswordField();
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList(
                "officer", "supervisor", "agency_admin"));
        roleCombo.setValue(target.getRole() != null ? target.getRole() : "officer");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("Active", "Inactive"));
        statusCombo.setValue(target.getStatus() != null ? target.getStatus() : "Active");
        CheckBox externalCheck = new CheckBox("External User");
        externalCheck.setSelected(target.isExternal());

        for (javafx.scene.control.Control c : new javafx.scene.control.Control[]{nameField, badgeField, usernameField, passwordField}) {
            ((Region)c).setMaxWidth(Double.MAX_VALUE);
        }
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Full Name *"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Badge #"),     0, 1); grid.add(badgeField, 1, 1);
        grid.add(new Label("Username"),    0, 2); grid.add(usernameField, 1, 2);
        grid.add(new Label(isNew ? "Password" : "New Password"), 0, 3);
        grid.add(passwordField, 1, 3);
        if (!isNew) {
            Label hint = new Label("Leave blank to keep current password");
            hint.setStyle("-fx-font-size:10; -fx-text-fill:#888;");
            grid.add(hint, 1, 4);
        }
        grid.add(new Label("Role"),   0, 5); grid.add(roleCombo, 1, 5);
        grid.add(new Label("Status"), 0, 6); grid.add(statusCombo, 1, 6);
        grid.add(externalCheck, 1, 7);

        dlg.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        dlg.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            target.setName(nameField.getText().trim());
            target.setBadge(badgeField.getText().trim());
            target.setUsername(usernameField.getText().trim());
            String pw = passwordField.getText();
            if (!pw.isBlank()) {
                // Simple SHA-256 hash — no need for full bcrypt in desktop app
                target.setPasswordHash(sha256(pw));
            }
            target.setRole(roleCombo.getValue());
            target.setStatus(statusCombo.getValue());
            target.setExternal(externalCheck.isSelected());
            return target;
        });

        Optional<Officer> result = dlg.showAndWait();
        result.ifPresent(o -> {
            if (o.getName().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Name is required.").showAndWait();
                return;
            }
            try {
                officerRepo.save(o);
                loadUsers(searchField.getText().trim());
            } catch (Exception e) { showError(e); }
        });
    }

    private void deleteUser(Officer o) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user \"" + o.getName() + "\"? This cannot be undone.", ButtonType.YES, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    officerRepo.delete(o.getId());
                    loadUsers(searchField.getText().trim());
                } catch (Exception e) { showError(e); }
            }
        });
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return input; }
    }

    // ── Nav ────────────────────────────────────────────────────────────────────
    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onAuditTrail()     { Navigator.get().showAuditTrail(); }
    @FXML private void onQuartermaster()  { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    private void showError(Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
    }
}
