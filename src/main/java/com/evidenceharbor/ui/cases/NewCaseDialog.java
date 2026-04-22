package com.evidenceharbor.ui.cases;

import com.evidenceharbor.app.ComboBoxHelper;
import com.evidenceharbor.app.SessionManager;
import com.evidenceharbor.domain.Case;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.CaseRepository;
import com.evidenceharbor.persistence.OfficerRepository;
import com.evidenceharbor.util.Dialogs;
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
            ComboBoxHelper.makeSearchable(officerBox, officers, Officer::getName);
            // Pre-select the currently logged-in officer
            Officer me = SessionManager.getCurrentOfficer();
            Officer preselect = officers.stream()
                    .filter(o -> me != null && o.getId() == me.getId())
                    .findFirst()
                    .orElse(officers.isEmpty() ? null : officers.get(0));
            officerBox.setValue(preselect);
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
            // Commit any typed text in the officer combo before validating
            officerBox.getParent().requestFocus();
            String num = caseNumberField.getText().trim();
            if (num.isEmpty() || datePicker.getValue() == null || officerBox.getValue() == null) {
                StringBuilder sb = new StringBuilder();
                if (num.isEmpty())                   sb.append("\u2022 Case Number\n");
                if (datePicker.getValue() == null)   sb.append("\u2022 Incident Date\n");
                if (officerBox.getValue() == null)   sb.append("\u2022 Case Officer\n");
                Dialogs.warn("All fields are required", "Please fill in:\n" + sb);
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
                Dialogs.error("Could not save", ex.getMessage());
                e.consume();
            }
        });

        dialog.showAndWait();
    }
}
