package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.domain.LookupItem;
import com.evidenceharbor.persistence.LookupRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

public class LookupAdminController implements Initializable {

    @FXML private ListView<String>    categoryList;
    @FXML private Label               editorTitle;
    @FXML private TextField           newItemField;
    @FXML private ListView<LookupItem> itemList;

    private final LookupRepository repo = new LookupRepository();

    /** Ordered category display name -> table key */
    private static final LinkedHashMap<String, String> CATEGORIES = new LinkedHashMap<>();
    static {
        // Evidence
        CATEGORIES.put("Ammunition Calibers",        "ammunition_calibers");
        CATEGORIES.put("Electronic Types",           "electronic_types");
        CATEGORIES.put("Narcotics Types",            "narcotics_types");
        CATEGORIES.put("Weapon Types",               "weapon_types");
        CATEGORIES.put("Biological Sources",         "biological_sources");
        CATEGORIES.put("Narcotics Unit Types",       "narcotics_unit_types");
        CATEGORIES.put("Evidence Storage Locations", "evidence_storage_locations");
        CATEGORIES.put("Intake Locations",           "intake_locations");
        // Case Management
        CATEGORIES.put("Person Roles",               "person_roles");
        CATEGORIES.put("Case Statuses",              "case_statuses");
        // Operations
        CATEGORIES.put("Transfer Actions",           "transfer_actions");
        CATEGORIES.put("Analysis Labs",              "analysis_labs");
        CATEGORIES.put("Other Agencies",             "other_agencies");
    }

    private String activeTable = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categoryList.setItems(FXCollections.observableArrayList(CATEGORIES.keySet()));
        categoryList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, val) -> onCategorySelected(val));

        itemList.setCellFactory(lv -> new ItemCell());

        // Select first category
        if (!CATEGORIES.isEmpty()) {
            categoryList.getSelectionModel().selectFirst();
        }
    }

    private void onCategorySelected(String displayName) {
        if (displayName == null) return;
        activeTable = CATEGORIES.get(displayName);
        editorTitle.setText(displayName);
        newItemField.clear();
        loadItems();
    }

    private void loadItems() {
        if (activeTable == null) return;
        try {
            List<LookupItem> items = repo.getAllFromTable(activeTable);
            itemList.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) { showError(e); }
    }

    @FXML
    private void onAddItem() {
        String name = newItemField.getText().trim();
        if (name.isBlank() || activeTable == null) return;
        // Duplicate check (case-insensitive)
        boolean exists = itemList.getItems().stream()
                .anyMatch(i -> i.getName().equalsIgnoreCase(name));
        if (exists) {
            new Alert(Alert.AlertType.WARNING, "\"" + name + "\" already exists in this list.").showAndWait();
            return;
        }
        try {
            repo.addToTable(activeTable, name);
            newItemField.clear();
            loadItems();
        } catch (SQLException e) { showError(e); }
    }

    private void deleteItem(LookupItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + item.getName() + "\"?", ButtonType.YES, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    repo.deleteFromTable(activeTable, item.getId());
                    loadItems();
                } catch (SQLException e) { showError(e); }
            }
        });
    }

    // ── Nav ───────────────────────────────────────────────────────────────────
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

    // ── Cell factory ──────────────────────────────────────────────────────────
    private class ItemCell extends ListCell<LookupItem> {
        private final Label nameLabel = new Label();
        private final Button deleteBtn = new Button("Delete");
        private final HBox row = new HBox(8, nameLabel, new javafx.scene.layout.Region(), deleteBtn);

        ItemCell() {
            javafx.scene.layout.Region spacer = (javafx.scene.layout.Region) row.getChildren().get(1);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            nameLabel.setStyle("-fx-text-fill: white;");
            deleteBtn.setStyle("-fx-font-size:11; -fx-padding:2 8 2 8; " +
                    "-fx-background-color:#c0392b; -fx-text-fill:white;");
            deleteBtn.setOnAction(e -> {
                LookupItem item = getItem();
                if (item != null) deleteItem(item);
            });
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 8, 4, 8));
        }

        @Override
        protected void updateItem(LookupItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); return; }
            nameLabel.setText(item.getName());
            setGraphic(row);
        }
    }
}
