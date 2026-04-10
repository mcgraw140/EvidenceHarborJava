package com.evidenceharbor.ui.cases;

import com.evidenceharbor.domain.Case;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.OfficerRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

public class NewCaseDialog {

    public static void show(CaseRepository caseRepo, Runnable onSaved) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("New Case");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField caseNumberField = new TextField();
        caseNumberField.setPromptText("e.g. 2026-0001");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<Officer> officerBox = new ComboBox<>();

        try {
            List<Officer> officers = new OfficerRepository().findAll();
            officerBox.getItems().addAll(officers);
            if (!officers.isEmpty()) officerBox.setValue(officers.get(0));
        } catch (Exception e) { e.printStackTrace(); }

        grid.add(new Label("Case Number:"), 0, 0);
        grid.add(caseNumberField, 1, 0);
        grid.add(new Label("Incident Date:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Case Officer:"), 0, 2);
        grid.add(officerBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                NewCaseDialog.class.getResource("/styles/theme.css").toExternalForm());

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setOnAction(e -> {
            String num = caseNumberField.getText().trim();
            if (num.isEmpty() || datePicker.getValue() == null || officerBox.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "All fields are required.").showAndWait();
                e.consume();
                return;
            }
            try {
                Case c = new Case();
                c.setCaseNumber(num);
                c.setIncidentDate(datePicker.getValue());
                c.setOfficer(officerBox.getValue());
                caseRepo.save(c);
                onSaved.run();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Could not save: " + ex.getMessage()).showAndWait();
                e.consume();
            }
        });

        dialog.showAndWait();
    }
}
