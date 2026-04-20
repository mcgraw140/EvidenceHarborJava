package com.evidenceharbor.ui.shared;

import com.evidenceharbor.persistence.DatabaseManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class DbStatusBarController implements Initializable {

    @FXML private Label dot;
    @FXML private Label statusLabel;

    private Timeline poller;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateStatus();
        // Poll every 10 seconds
        poller = new Timeline(new KeyFrame(Duration.seconds(10), e -> updateStatus()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();

        // Stop polling when the node is removed from scene
        dot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && poller != null) {
                poller.stop();
            }
        });
    }

    private void updateStatus() {
        boolean connected = DatabaseManager.isConnected();
        if (connected) {
            dot.setStyle("-fx-font-size:13px; -fx-text-fill:#22c55e;");
            statusLabel.setText("Database: Connected");
        } else {
            dot.setStyle("-fx-font-size:13px; -fx-text-fill:#ef4444;");
            statusLabel.setText("Database: Not connected — go to Admin > Settings > Database Connection");
        }
    }
}
