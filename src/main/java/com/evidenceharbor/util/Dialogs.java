package com.evidenceharbor.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.util.Optional;

/**
 * Centralized helpers for building themed (dark-mode) popups.
 * <p>
 * All factories here automatically:
 * <ul>
 *   <li>Attach {@code /styles/theme.css} so the dialog honors dark-mode colors.</li>
 *   <li>Set title, header and content text so dialogs never render blank.</li>
 * </ul>
 */
public final class Dialogs {

    private static final String THEME = "/styles/theme.css";

    private Dialogs() {}

    /** Attach the theme stylesheet to any custom {@link Dialog}. Safe to call multiple times. */
    public static void style(Dialog<?> d) {
        if (d == null) return;
        DialogPane pane = d.getDialogPane();
        if (pane == null) return;
        try {
            String url = Dialogs.class.getResource(THEME).toExternalForm();
            if (!pane.getStylesheets().contains(url)) {
                pane.getStylesheets().add(url);
            }
        } catch (Exception ignore) { /* stylesheet missing - fall back to default */ }
    }

    /** Build a themed {@link Alert} with guaranteed-visible title / header / content. */
    public static Alert alert(AlertType type, String title, String header, String content, ButtonType... buttons) {
        Alert a = (buttons == null || buttons.length == 0)
                ? new Alert(type)
                : new Alert(type, content == null ? "" : content, buttons);
        a.setTitle(title == null || title.isBlank() ? defaultTitle(type) : title);
        a.setHeaderText(header == null || header.isBlank() ? defaultTitle(type) : header);
        a.setContentText(content == null || content.isBlank()
                ? (header == null || header.isBlank() ? defaultTitle(type) : header)
                : content);
        style(a);
        return a;
    }

    private static String defaultTitle(AlertType type) {
        if (type == null) return "Notice";
        switch (type) {
            case ERROR:        return "Error";
            case WARNING:      return "Warning";
            case INFORMATION:  return "Information";
            case CONFIRMATION: return "Confirm";
            default:           return "Notice";
        }
    }

    // ── Convenience one-liners ─────────────────────────────────────────────

    public static void info(String message) {
        info("Information", message);
    }

    public static void info(String header, String message) {
        alert(AlertType.INFORMATION, "Information", header, message).showAndWait();
    }

    public static void warn(String message) {
        warn("Warning", message);
    }

    public static void warn(String header, String message) {
        alert(AlertType.WARNING, "Warning", header, message).showAndWait();
    }

    public static void error(String message) {
        error("Something went wrong", message);
    }

    public static void error(String header, String message) {
        alert(AlertType.ERROR, "Error", header, message).showAndWait();
    }

    public static void error(Throwable t) {
        String detail = (t == null || t.getMessage() == null || t.getMessage().isBlank())
                ? (t == null ? "Unknown error" : t.getClass().getSimpleName())
                : t.getMessage();
        error("Something went wrong", detail);
    }

    /** Show a Yes/No confirmation. Returns true if the user chose YES/OK. */
    public static boolean confirm(String message) {
        return confirm("Confirm", message);
    }

    public static boolean confirm(String header, String message) {
        Alert a = alert(AlertType.CONFIRMATION, "Confirm", header, message,
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && (r.get() == ButtonType.YES || r.get() == ButtonType.OK);
    }
}
