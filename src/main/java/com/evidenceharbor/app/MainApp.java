package com.evidenceharbor.app;

import com.evidenceharbor.persistence.DatabaseManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize DB
        DatabaseManager.getInstance();

        Navigator nav = new Navigator(primaryStage);
        nav.showCaseList();
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
