package com.evidenceharbor.app;

import com.evidenceharbor.persistence.DatabaseManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        new SetupWizard(primaryStage).show();
    }

    @Override
    public void stop() {
        DatabaseManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
