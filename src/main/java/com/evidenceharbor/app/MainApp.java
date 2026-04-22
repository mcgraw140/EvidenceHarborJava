package com.evidenceharbor.app;

import com.evidenceharbor.persistence.DatabaseManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainApp extends Application {

    /** Sentinel file path the external splash (from the .bat) watches for. */
    private static final Path SPLASH_READY = Paths.get(
            System.getProperty("java.io.tmpdir"), "evidence-harbor-ready.flag");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setOnShown(ev -> signalSplashDismiss());
        new SetupWizard(primaryStage).show();
    }

    @Override
    public void stop() {
        DatabaseManager.shutdown();
    }

    private static void signalSplashDismiss() {
        try {
            Files.writeString(SPLASH_READY, String.valueOf(System.currentTimeMillis()));
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
