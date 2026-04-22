package com.evidenceharbor.util;

import com.evidenceharbor.domain.AgencySettings;
import com.evidenceharbor.persistence.SettingsRepository;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds letter-sized (8.5" × 11") HTML print sheets that match the styling of
 * the Evidence Property Sheet (see EvidenceDetailController.buildPrintHtml):
 * navy header with agency block, section titles, key/value grids,
 * zebra-striped data tables, and a footer with confidentiality notice.
 *
 * Generated HTML is written to a temp file and opened in the system browser
 * for the user to preview and print. This excludes barcode-label printing,
 * which goes through a dedicated Zebra flow.
 */
public final class PrintSheetUtil {

    private PrintSheetUtil() {}

    // ──────────────────────────────────────────────────────────────────
    // Section types
    // ──────────────────────────────────────────────────────────────────

    public interface Section {}

    /** A two-column key/value grid section. Pairs are rendered two per row. */
    public static final class KVSection implements Section {
        public final String title;
        public final List<String[]> pairs; // each entry is [key, value]
        public KVSection(String title, List<String[]> pairs) {
            this.title = title;
            this.pairs = pairs == null ? List.of() : pairs;
        }
    }

    /** A tabular section — headers + rows. */
    public static final class TableSection implements Section {
        public final String title;
        public final String[] headers;
        public final List<String[]> rows;
        public TableSection(String title, String[] headers, List<String[]> rows) {
            this.title = title;
            this.headers = headers == null ? new String[0] : headers;
            this.rows = rows == null ? List.of() : rows;
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Public entry points
    // ──────────────────────────────────────────────────────────────────

    /** Print a single TableView as a letter-sized sheet. */
    public static void printTable(Window owner, String docTitle, TableView<?> table) {
        if (table == null || table.getItems().isEmpty()) {
            Dialogs.info("Nothing to print.");
            return;
        }
        String[] headers = tableHeaders(table);
        List<String[]> rows = tableRows(table);
        print(owner, docTitle, List.of(new TableSection(null, headers, rows)));
    }

    /** Print a single TableView, prefixed by a KV metadata block. */
    public static void printTable(Window owner, String docTitle,
                                  List<String[]> metaPairs, TableView<?> table) {
        if (table == null || table.getItems().isEmpty()) {
            Dialogs.info("Nothing to print.");
            return;
        }
        List<Section> sections = new ArrayList<>();
        if (metaPairs != null && !metaPairs.isEmpty())
            sections.add(new KVSection("Details", metaPairs));
        sections.add(new TableSection(null, tableHeaders(table), tableRows(table)));
        print(owner, docTitle, sections);
    }

    /** Print raw headers + row data (used by Reports which already has string rows). */
    public static void printTable(Window owner, String docTitle,
                                  String[] headers,
                                  List<ObservableList<String>> rows) {
        if (headers == null || headers.length == 0 || rows == null || rows.isEmpty()) {
            Dialogs.info("Nothing to print.");
            return;
        }
        List<String[]> flat = rows.stream()
                .map(r -> r.toArray(new String[0]))
                .collect(Collectors.toList());
        print(owner, docTitle, List.of(new TableSection(null, headers, flat)));
    }

    /** Print an arbitrary set of sections. */
    public static void print(Window owner, String docTitle, List<Section> sections) {
        try {
            AgencySettings agency = new SettingsRepository().load();
            String html = buildHtml(agency, docTitle, sections == null ? List.of() : sections);
            File tmp = File.createTempFile("eh_print_" + safeFile(docTitle) + "_", ".html",
                    new File(System.getProperty("java.io.tmpdir")));
            tmp.deleteOnExit();
            try (FileWriter fw = new FileWriter(tmp, StandardCharsets.UTF_8)) {
                fw.write(html);
            }
            java.awt.Desktop.getDesktop().browse(tmp.toURI());
        } catch (Exception ex) {
            ex.printStackTrace();
            Dialogs.error(ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // TableView helpers
    // ──────────────────────────────────────────────────────────────────

    public static String[] tableHeaders(TableView<?> t) {
        return t.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText())
                .toArray(String[]::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<String[]> tableRows(TableView<?> t) {
        List<String[]> out = new ArrayList<>();
        List<? extends TableColumn<?, ?>> cols = t.getColumns();
        for (Object item : t.getItems()) {
            String[] row = new String[cols.size()];
            for (int i = 0; i < cols.size(); i++) {
                Object val;
                try {
                    val = ((TableColumn) cols.get(i)).getCellData(item);
                } catch (Exception ex) {
                    val = null;
                }
                row[i] = val == null ? "" : val.toString();
            }
            out.add(row);
        }
        return out;
    }

    // ──────────────────────────────────────────────────────────────────
    // HTML generation
    // ──────────────────────────────────────────────────────────────────

    private static String buildHtml(AgencySettings agency, String docTitle, List<Section> sections) {
        String agencyName    = agency.getAgencyName().isBlank()    ? "Evidence Harbor" : agency.getAgencyName();
        String agencyAddr    = agency.getAgencyAddress();
        String agencyCityLine = Stream.of(agency.getAgencyCity(), agency.getAgencyState(), agency.getAgencyZip())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + ", " + b).orElse("");
        String printed = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
        String title = (docTitle == null || docTitle.isBlank()) ? "Report" : docTitle;

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<title>").append(esc(title)).append("</title>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;font-size:12px;color:#111;margin:0;padding:0}");
        sb.append(".page{width:8.5in;min-height:11in;margin:0 auto;padding:.5in .6in .5in .6in;box-sizing:border-box}");
        sb.append(".header{display:flex;align-items:center;justify-content:space-between;border-bottom:3px solid #1a3a6e;padding-bottom:10px;margin-bottom:14px}");
        sb.append(".agency-name{font-size:20px;font-weight:bold;color:#1a3a6e}");
        sb.append(".agency-sub{font-size:11px;color:#444;margin-top:2px}");
        sb.append(".doc-title{font-size:15px;font-weight:bold;color:#1a3a6e;text-align:right}");
        sb.append(".section-title{font-size:12px;font-weight:bold;color:#1a3a6e;border-bottom:1px solid #aac;margin:14px 0 6px 0;padding-bottom:2px;text-transform:uppercase;letter-spacing:.5px}");
        sb.append(".grid{display:grid;grid-template-columns:160px 1fr 160px 1fr;gap:4px 10px;margin-bottom:4px}");
        sb.append(".key{font-weight:bold;color:#333;font-size:11px}");
        sb.append(".val{color:#111;font-size:11px}");
        sb.append("table{width:100%;border-collapse:collapse;font-size:10.5px;margin-top:4px}");
        sb.append("th{background:#1a3a6e;color:#fff;padding:4px 6px;text-align:left}");
        sb.append("td{padding:3px 6px;border-bottom:1px solid #ddd;vertical-align:top}");
        sb.append("tr:nth-child(even) td{background:#f5f7fa}");
        sb.append(".footer{margin-top:20px;border-top:1px solid #ccc;padding-top:8px;font-size:10px;color:#666;display:flex;justify-content:space-between}");
        sb.append("@page{size:letter;margin:.5in .6in}");
        sb.append("@media print{body{-webkit-print-color-adjust:exact;print-color-adjust:exact}.page{width:auto;padding:0}}");
        sb.append("</style></head><body><div class='page'>");

        // ── Header ───────────────────────────────────────────────
        sb.append("<div class='header'>");
        sb.append("<div><div class='agency-name'>").append(esc(agencyName)).append("</div>");
        if (agencyAddr != null && !agencyAddr.isBlank())
            sb.append("<div class='agency-sub'>").append(esc(agencyAddr)).append("</div>");
        if (!agencyCityLine.isBlank())
            sb.append("<div class='agency-sub'>").append(esc(agencyCityLine)).append("</div>");
        sb.append("</div>");
        sb.append("<div><div class='doc-title'>").append(esc(title.toUpperCase())).append("</div>");
        sb.append("<div style='font-size:10px;color:#555;text-align:right'>Printed: ").append(printed).append("</div></div>");
        sb.append("</div>");

        // ── Sections ─────────────────────────────────────────────
        for (Section s : sections) {
            if (s instanceof KVSection kv) renderKV(sb, kv);
            else if (s instanceof TableSection ts) renderTable(sb, ts);
        }

        // ── Footer ───────────────────────────────────────────────
        sb.append("<div class='footer'>");
        sb.append("<span>").append(esc(agencyName)).append(" — Evidence Management System</span>");
        sb.append("<span>Evidence Harbor | Confidential Law Enforcement Record</span>");
        sb.append("</div>");

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private static void renderKV(StringBuilder sb, KVSection kv) {
        if (kv.title != null && !kv.title.isBlank())
            sb.append("<div class='section-title'>").append(esc(kv.title)).append("</div>");
        sb.append("<div class='grid'>");
        List<String[]> pairs = kv.pairs;
        for (int i = 0; i < pairs.size(); i += 2) {
            String k1 = safeGet(pairs.get(i), 0), v1 = safeGet(pairs.get(i), 1);
            String k2 = i + 1 < pairs.size() ? safeGet(pairs.get(i + 1), 0) : "";
            String v2 = i + 1 < pairs.size() ? safeGet(pairs.get(i + 1), 1) : "";
            sb.append("<div class='key'>").append(esc(k1)).append(k1.isBlank() ? "" : ":").append("</div>")
              .append("<div class='val'>").append(esc(v1)).append("</div>")
              .append("<div class='key'>").append(esc(k2)).append(k2.isBlank() ? "" : ":").append("</div>")
              .append("<div class='val'>").append(esc(v2)).append("</div>");
        }
        sb.append("</div>");
    }

    private static void renderTable(StringBuilder sb, TableSection ts) {
        if (ts.title != null && !ts.title.isBlank())
            sb.append("<div class='section-title'>").append(esc(ts.title))
              .append(" (").append(ts.rows.size()).append(" record")
              .append(ts.rows.size() == 1 ? "" : "s").append(")</div>");
        sb.append("<table><tr>");
        for (String h : ts.headers) sb.append("<th>").append(esc(h)).append("</th>");
        sb.append("</tr>");
        for (String[] row : ts.rows) {
            sb.append("<tr>");
            for (int c = 0; c < ts.headers.length; c++) {
                String v = c < row.length ? row[c] : "";
                sb.append("<td>").append(esc(v)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    // ──────────────────────────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────────────────────────

    private static String esc(String s) {
        if (s == null || s.isBlank()) return "&nbsp;";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String safeGet(String[] arr, int idx) {
        return arr != null && idx < arr.length && arr[idx] != null ? arr[idx] : "";
    }

    private static String safeFile(String s) {
        if (s == null || s.isBlank()) return "report";
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
