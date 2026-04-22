package com.evidenceharbor.util;

import javafx.collections.ObservableList;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Shared CSV-export and printing helpers for tabular data.
 */
public final class TableExportUtil {

    private TableExportUtil() {}

    /** Export a TableView whose items are ObservableList&lt;String&gt; rows. */
    public static void exportCsv(Window owner, String suggestedFileName,
                                 String[] headers, List<ObservableList<String>> rows) {
        if (headers == null || headers.length == 0 || rows == null || rows.isEmpty()) {
            Dialogs.info("Nothing to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export as CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName(suggestedFileName == null ? "report.csv" : suggestedFileName);
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(csvJoin(headers));
            fw.write("\n");
            for (ObservableList<String> row : rows) {
                String[] arr = row.toArray(new String[0]);
                fw.write(csvJoin(arr));
                fw.write("\n");
            }
        } catch (IOException e) {
            Dialogs.error("Export failed", e.getMessage());
        }
    }

    /** Print a table's data — opens a print preview first. */
    public static void printTable(Window owner, String title, TableView<?> source) {
        if (source == null || source.getItems().isEmpty()) {
            Dialogs.info("Nothing to print.");
            return;
        }
        Node printable = buildPrintableTable(title, source);
        showPrintPreview(owner, title, printable, PageOrientation.LANDSCAPE);
    }

    /** Print an arbitrary node — opens a print preview first. */
    public static void printNode(Window owner, Node node) {
        if (node == null) return;
        showPrintPreview(owner, "Print Preview", node, PageOrientation.PORTRAIT);
    }

    /** Show a print preview window with Print and Cancel buttons. */
    public static void showPrintPreview(Window owner, String title, Node printable, PageOrientation orientation) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Print Preview — " + (title == null ? "" : title));

        Label header = new Label("Print Preview");
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");
        Label hint = new Label("Review the output below, then click Print to send to a printer.");
        hint.setStyle("-fx-text-fill:#555;");

        ScrollPane sp = new ScrollPane(printable);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:white;");
        sp.setPrefViewportWidth(900);
        sp.setPrefViewportHeight(600);

        Button printBtn = new Button("Print");
        printBtn.setStyle("-fx-font-size:13; -fx-padding:8 18 8 18;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-font-size:13; -fx-padding:8 18 8 18;");

        printBtn.setOnAction(e -> {
            if (doPrint(dlg, printable, orientation)) {
                dlg.close();
            }
        });
        cancelBtn.setOnAction(e -> dlg.close());

        HBox buttons = new HBox(12, cancelBtn, printBtn);
        buttons.setStyle("-fx-padding:12; -fx-alignment: center-right;");

        VBox top = new VBox(4, header, hint);
        top.setStyle("-fx-padding:12 16 8 16;");

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(sp);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 980, 760);
        try {
            scene.getStylesheets().add(
                    TableExportUtil.class.getResource("/styles/theme.css").toExternalForm());
        } catch (Exception ignored) { }
        dlg.setScene(scene);
        dlg.showAndWait();
    }

    private static boolean doPrint(Window owner, Node node, PageOrientation orientation) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            Dialogs.error("No printer available.");
            return false;
        }
        if (!job.showPrintDialog(owner)) return false;
        Printer printer = job.getPrinter();
        PageLayout layout = printer.createPageLayout(
                Paper.NA_LETTER, orientation, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);

        double w = Math.max(1, node.getBoundsInParent().getWidth());
        double h = Math.max(1, node.getBoundsInParent().getHeight());
        double scaleX = layout.getPrintableWidth()  / w;
        double scaleY = layout.getPrintableHeight() / h;
        double scale = Math.min(1.0, Math.min(scaleX, scaleY));
        Scale tx = new Scale(scale, scale);
        node.getTransforms().add(tx);
        boolean ok = job.printPage(layout, node);
        node.getTransforms().remove(tx);
        if (ok) job.endJob();
        return ok;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Node buildPrintableTable(String title, TableView<?> source) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: white; -fx-padding: 16;");
        Label t = new Label(title == null ? "Report" : title);
        t.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill: black;");
        Label ts = new Label("Printed: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        ts.setStyle("-fx-font-size:10px; -fx-text-fill: #444;");
        box.getChildren().addAll(t, ts);

        // Rebuild a lightweight print-friendly copy of the table (black on white, readable)
        TableView copy = new TableView();
        copy.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        copy.getItems().addAll(source.getItems());
        for (TableColumn<?, ?> col : source.getColumns()) {
            TableColumn c = new TableColumn(col.getText());
            c.setCellValueFactory(((TableColumn) col).getCellValueFactory());
            c.setPrefWidth(col.getWidth() > 0 ? col.getWidth() : 120);
            copy.getColumns().add(c);
        }
        copy.setPrefSize(
            Math.max(640, source.getColumns().size() * 140),
            Math.min(900, 40 + source.getItems().size() * 26));
        box.getChildren().add(copy);
        // Force layout
        new Scene(box);
        box.applyCss();
        box.layout();
        return box;
    }

    private static String csvJoin(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            String v = fields[i] == null ? "" : fields[i];
            sb.append('"').append(v.replace("\"", "\"\"")).append('"');
        }
        return sb.toString();
    }
}
