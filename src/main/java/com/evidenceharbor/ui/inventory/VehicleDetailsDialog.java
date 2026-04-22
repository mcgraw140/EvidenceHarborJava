package com.evidenceharbor.ui.inventory;

import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.persistence.ChainOfCustodyRepository;
import com.evidenceharbor.persistence.EvidenceRepository;
import com.evidenceharbor.persistence.LookupRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Shared dialog to view a vehicle's impound record and change its status
 * (release to owner, mark as stolen recovery return, move to long-term storage, etc.).
 */
public final class VehicleDetailsDialog {

    private VehicleDetailsDialog() {}

    /** Shows the dialog. Returns true if the record was updated. */
    public static boolean show(Evidence vehicle) {
        if (vehicle == null) return false;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vehicle " + nvl(vehicle.getBarcode()));
        dialog.setHeaderText(vehicleHeader(vehicle));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(16));

        int r = 0;
        addReadOnlyRow(grid, r++, "Barcode:",  nvl(vehicle.getBarcode()));
        addReadOnlyRow(grid, r++, "Type:",     nvl(vehicle.getVehicleBodyType()));
        addReadOnlyRow(grid, r++, "Year:",     nvl(vehicle.getVehicleYear()));
        addReadOnlyRow(grid, r++, "Make:",     nvl(vehicle.getVehicleMake()));
        addReadOnlyRow(grid, r++, "Model:",    nvl(vehicle.getVehicleModel()));
        addReadOnlyRow(grid, r++, "Color:",    nvl(vehicle.getVehicleColor()));
        addReadOnlyRow(grid, r++, "VIN:",      nvl(vehicle.getVehicleVin()));
        addReadOnlyRow(grid, r++, "Plate:",    (nvl(vehicle.getVehicleLicensePlate()) + " " + nvl(vehicle.getVehicleLicenseState())).trim());
        addReadOnlyRow(grid, r++, "Stolen:",   vehicle.isVehicleReportedStolen() ? "Yes" : "No");
        addReadOnlyRow(grid, r++, "Notes:",    nvl(vehicle.getDescription()));

        // Separator
        Separator sep = new Separator();
        grid.add(sep, 0, r++, 2, 1);

        // Editable status + location
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "In Custody",
                "Released to Owner",
                "Released to Lienholder",
                "Released to Insurance",
                "Transferred",
                "Sold at Auction",
                "Destroyed",
                "Returned to Service"
        );
        String currentStatus = nvl(vehicle.getStatus());
        if (!currentStatus.isEmpty() && !statusBox.getItems().contains(currentStatus)) {
            statusBox.getItems().add(0, currentStatus);
        }
        statusBox.setValue(currentStatus.isEmpty() ? "In Custody" : currentStatus);
        statusBox.setEditable(true);

        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.setEditable(true);
        locationBox.setPromptText("Select location...");
        locationBox.setMaxWidth(Double.MAX_VALUE);
        try { locationBox.getItems().setAll(new LookupRepository().getImpoundLocations()); } catch (Exception ignored) {}
        if (nvl(vehicle.getStorageLocation()).length() > 0) locationBox.getEditor().setText(vehicle.getStorageLocation());

        TextField releasedToField = new TextField();
        releasedToField.setPromptText("Released to (name) - optional note appended to record");

        grid.add(new Label("Status:"), 0, r);    grid.add(statusBox, 1, r++);
        grid.add(new Label("Location:"), 0, r);  grid.add(locationBox, 1, r++);
        grid.add(new Label("Released To:"), 0, r); grid.add(releasedToField, 1, r++);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        try {
            dialog.getDialogPane().getStylesheets().add(
                    VehicleDetailsDialog.class.getResource("/styles/theme.css").toExternalForm());
        } catch (Exception ignored) {}

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        final boolean[] saved = { false };
        saveBtn.setOnAction(e -> {
            String status = statusBox.getValue() == null ? "" : statusBox.getValue().trim();
            if (status.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Status is required.").showAndWait();
                e.consume();
                return;
            }
            String location = locationBox.getEditor().getText();
            if (location == null) location = locationBox.getValue();
            location = location == null ? "" : location.trim();
            String releasedTo = releasedToField.getText() == null ? "" : releasedToField.getText().trim();
            if (!releasedTo.isEmpty()) {
                String note = nvl(vehicle.getDescription());
                String addendum = "[" + java.time.LocalDate.now() + "] " + status + " - " + releasedTo;
                vehicle.setDescription(note.isEmpty() ? addendum : (note + "\n" + addendum));
            }
            boolean stillImpounded = status.equalsIgnoreCase("In Custody");
            try {
                String fromLocation = nvl(vehicle.getStorageLocation());
                new EvidenceRepository().updateVehicleStatus(vehicle.getId(), status, location, stillImpounded);

                // Chain of custody entry for any status change
                com.evidenceharbor.domain.ChainOfCustody coc = new com.evidenceharbor.domain.ChainOfCustody();
                coc.setEvidenceId(vehicle.getId());
                coc.setAction(status);
                com.evidenceharbor.domain.Officer o = com.evidenceharbor.app.SessionManager.getCurrentOfficer();
                String actor = o == null ? "" : o.getName();
                coc.setPerformedBy(actor);
                coc.setPerformedByName(actor);
                coc.setFromLocation(fromLocation);
                coc.setToLocation(location);
                coc.setToPerson(releasedTo);
                coc.setNotes(stillImpounded ? "Impound location updated" : "Vehicle status changed");
                new ChainOfCustodyRepository().addEntry(coc);

                vehicle.setStatus(status);
                vehicle.setStorageLocation(location);
                vehicle.setVehicleImpounded(stillImpounded);
                saved[0] = true;
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
                e.consume();
            }
        });

        dialog.showAndWait();
        return saved[0];
    }

    private static void addReadOnlyRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        Label v = new Label(value == null ? "" : value);
        v.getStyleClass().add("field-value");
        v.setWrapText(true);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private static String vehicleHeader(Evidence v) {
        StringBuilder sb = new StringBuilder();
        if (v.getVehicleYear() != null) sb.append(v.getVehicleYear()).append(' ');
        if (v.getVehicleMake() != null) sb.append(v.getVehicleMake()).append(' ');
        if (v.getVehicleModel() != null) sb.append(v.getVehicleModel());
        return sb.toString().trim();
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
