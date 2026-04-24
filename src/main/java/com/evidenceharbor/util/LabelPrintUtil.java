package com.evidenceharbor.util;

import java.nio.charset.StandardCharsets;

/**
 * Builds ZPL II for evidence barcode labels printed to Zebra thermal printers.
 * Default layout: 2" × 1" at 203 dpi (406 × 203 dots).
 *
 * Layout (top to bottom):
 *   - Code 128 barcode with auto-fit narrow bar width and human-readable text
 *   - Case #, Collection Date, Storage Location
 */
public final class LabelPrintUtil {

    private LabelPrintUtil() {}

    // Original layout — 2" × 1" at 203 dpi, fixed module width = 2
    private static final int LABEL_WIDTH_DOTS  = 406;
    private static final int LABEL_HEIGHT_DOTS = 203;
    private static final int X_MARGIN       = 10;
    private static final int BARCODE_Y      = 5;
    private static final int BARCODE_HEIGHT = 55;
    private static final int BARCODE_MODULE = 2;   // narrow bar width in dots
    // Standard text rows (Evidence ID, Case, Date) — larger for readability
    private static final int TEXT_FONT_H    = 22;
    private static final int TEXT_FONT_W    = 16;
    private static final int TEXT_Y_ID      = 78;
    private static final int TEXT_Y_CASE    = 102;
    private static final int TEXT_Y_DATE    = 126;
    // Location row — large and bold for visual sorting on shelves
    private static final int LOC_FONT_H     = 42;
    private static final int LOC_FONT_W     = 34;
    private static final int TEXT_Y_LOC     = 154;

    /**
     * Build ZPL for a single evidence label. Each argument may be null — empty
     * values are rendered as blank lines.
     *
     * @param scanCode       short scanner-friendly code (e.g. "26-00001"); encoded in the bars
     * @param evidenceId     long human-readable evidence number (e.g. "2026-Currency-0001")
     * @param caseNumber     case number to print
     * @param collectionDate collection date string
     * @param storageLocation storage location (printed large)
     */
    public static byte[] build(String scanCode, String evidenceId, String caseNumber,
                               String collectionDate, String storageLocation) {
        String code    = ns(scanCode);
        String idText  = ns(evidenceId);
        String caseNum = ns(caseNumber);
        String date    = ns(collectionDate);
        String loc     = ns(storageLocation);

        // Center the barcode horizontally within the label
        int chars = Math.max(code.length(), 1);
        int barcodeDots = BARCODE_MODULE * (11 * chars + 35);
        int barcodeX = Math.max(X_MARGIN, (LABEL_WIDTH_DOTS - barcodeDots) / 2);

        StringBuilder z = new StringBuilder();
        z.append("^XA\n")
         .append("^PW").append(LABEL_WIDTH_DOTS).append("\n")
         .append("^LL").append(LABEL_HEIGHT_DOTS).append("\n")
         .append("^LH0,0\n")
         .append("^CI28\n") // UTF-8
         .append("^FO").append(barcodeX).append(",").append(BARCODE_Y)
         .append("^BY").append(BARCODE_MODULE).append(",3,").append(BARCODE_HEIGHT).append("\n")
         .append("^BCN,").append(BARCODE_HEIGHT).append(",Y,N,N\n")
         .append("^FD").append(code).append("^FS\n")
         .append(textRow(X_MARGIN, TEXT_Y_ID,   TEXT_FONT_H, TEXT_FONT_W, "ID:   " + idText))
         .append(textRow(X_MARGIN, TEXT_Y_CASE, TEXT_FONT_H, TEXT_FONT_W, "Case: " + caseNum))
         .append(textRow(X_MARGIN, TEXT_Y_DATE, TEXT_FONT_H, TEXT_FONT_W, "Date: " + date))
         .append(textRow(X_MARGIN, TEXT_Y_LOC,  LOC_FONT_H,  LOC_FONT_W,  loc))
         .append("^XZ\n");
        return z.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String textRow(int x, int y, int fh, int fw, String text) {
        return "^FO" + x + "," + y
                + "^A0N," + fh + "," + fw
                + "^FD" + text + "^FS\n";
    }

    private static String ns(String s) { return s == null ? "" : s; }

    // ──────────────────────────────────────────────────────────────────────
    // Calibration / test label — prints a dot grid with coordinate labels so
    // you can see exactly where the printer's physical printable area is and
    // whether the declared label size matches the label stock.
    //
    // Features:
    //   • Solid border at the declared edges
    //   • Dots every 20 dots (a "dot matrix")
    //   • Coordinate tick labels every 50 dots on top + left edges
    //   • Corner crosshairs + center crosshair
    //   • Dimension banner (W×H in dots and inches) centered
    //
    // If the grid runs off the edge of the physical label, the printer is
    // configured for a different label size than we declared. Measure the
    // last fully-visible tick to find the real printable width/height.
    // ──────────────────────────────────────────────────────────────────────
    public static byte[] buildCalibration(int widthDots, int heightDots) {
        StringBuilder z = new StringBuilder();
        z.append("^XA\n")
         .append("^PW").append(widthDots).append("\n")
         .append("^LL").append(heightDots).append("\n")
         .append("^LH0,0\n")
         .append("^CI28\n");

        // Outer border (1-dot thick)
        z.append("^FO0,0^GB").append(widthDots - 1).append(",").append(heightDots - 1).append(",1^FS\n");

        // Dot matrix every 20 dots (2×2 squares so they're visible)
        for (int x = 20; x < widthDots; x += 20) {
            for (int y = 20; y < heightDots; y += 20) {
                z.append("^FO").append(x).append(",").append(y).append("^GB2,2,2^FS\n");
            }
        }

        // Major tick marks + numeric labels on TOP edge every 50 dots
        for (int x = 0; x <= widthDots; x += 50) {
            // 10-dot tick down from top
            int tx = Math.min(x, widthDots - 1);
            z.append("^FO").append(tx).append(",0^GB1,10,1^FS\n");
            // label to the right of the tick
            if (x + 24 < widthDots) {
                z.append("^FO").append(tx + 2).append(",2^A0N,12,8^FD").append(x).append("^FS\n");
            }
        }
        // Major tick marks + labels on LEFT edge every 50 dots
        for (int y = 0; y <= heightDots; y += 50) {
            int ty = Math.min(y, heightDots - 1);
            z.append("^FO0,").append(ty).append("^GB10,1,1^FS\n");
            if (y + 14 < heightDots) {
                z.append("^FO2,").append(ty + 2).append("^A0N,12,8^FD").append(y).append("^FS\n");
            }
        }

        // Center crosshair
        int cx = widthDots / 2;
        int cy = heightDots / 2;
        z.append("^FO").append(cx - 15).append(",").append(cy).append("^GB30,1,1^FS\n");
        z.append("^FO").append(cx).append(",").append(cy - 15).append("^GB1,30,1^FS\n");

        // Dimension banner (dots + inches @ 203 dpi)
        String inchW = String.format("%.2f", widthDots / 203.0);
        String inchH = String.format("%.2f", heightDots / 203.0);
        String banner = widthDots + "x" + heightDots + " dots  " + inchW + "\"x" + inchH + "\" @203dpi";
        int bannerX = Math.max(15, cx - (banner.length() * 6)); // approx char width 6 @ A0 12/8
        z.append("^FO").append(bannerX).append(",").append(Math.min(cy + 18, heightDots - 18))
         .append("^A0N,16,10^FD").append(banner).append("^FS\n");

        // Corner markers — L-shapes so clipping is obvious
        int armLen = 20;
        // top-left
        z.append("^FO0,0^GB").append(armLen).append(",2,2^FS\n");
        z.append("^FO0,0^GB2,").append(armLen).append(",2^FS\n");
        // top-right
        z.append("^FO").append(widthDots - armLen).append(",0^GB").append(armLen).append(",2,2^FS\n");
        z.append("^FO").append(widthDots - 2).append(",0^GB2,").append(armLen).append(",2^FS\n");
        // bottom-left
        z.append("^FO0,").append(heightDots - 2).append("^GB").append(armLen).append(",2,2^FS\n");
        z.append("^FO0,").append(heightDots - armLen).append("^GB2,").append(armLen).append(",2^FS\n");
        // bottom-right
        z.append("^FO").append(widthDots - armLen).append(",").append(heightDots - 2)
         .append("^GB").append(armLen).append(",2,2^FS\n");
        z.append("^FO").append(widthDots - 2).append(",").append(heightDots - armLen)
         .append("^GB2,").append(armLen).append(",2^FS\n");

        z.append("^XZ\n");
        return z.toString().getBytes(StandardCharsets.UTF_8);
    }
}
