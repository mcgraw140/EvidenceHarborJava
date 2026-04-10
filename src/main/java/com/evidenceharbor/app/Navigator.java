package com.evidenceharbor.app;

import com.evidenceharbor.domain.Case;
import com.evidenceharbor.ui.cases.CaseDetailController;
import com.evidenceharbor.ui.evidence.AddEvidenceController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Navigator {

    private final Stage stage;
    private static Navigator instance;

    public Navigator(Stage stage) {
        this.stage = stage;
        instance = this;
        stage.setTitle("Evidence Harbor");
        stage.setWidth(1280);
        stage.setHeight(780);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
    }

    public static Navigator get() { return instance; }

    public void showCaseList() {
        loadScene("/fxml/CaseList.fxml", null);
    }

    public void showInventory() {
        loadScene("/fxml/Inventory.fxml", null);
    }

    public void showPeople() {
        loadScene("/fxml/People.fxml", null);
    }

    public void showDropbox() {
        loadScene("/fxml/Dropbox.fxml", null);
    }

    public void showReports() {
        loadScene("/fxml/Reports.fxml", null);
    }

    public void showCaseDetail(Case c) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CaseDetail.fxml"));
        try {
            Parent root = loader.load();
            CaseDetailController ctrl = loader.getController();
            ctrl.setCase(c);
            applyScene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showAddEvidence(Case c) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddEvidence.fxml"));
        try {
            Parent root = loader.load();
            AddEvidenceController ctrl = loader.getController();
            ctrl.initForCase(c);
            applyScene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadScene(String fxml, Object unused) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            applyScene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyScene(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        stage.show();
    }
}
