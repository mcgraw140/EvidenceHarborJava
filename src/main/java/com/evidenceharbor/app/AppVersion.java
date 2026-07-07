package com.evidenceharbor.app;

/**
 * Reports the running application's version. Reads from the JAR manifest
 * (Implementation-Version), which is populated by maven-shade-plugin at build time.
 * Falls back to a dev sentinel when running from an IDE / uncompiled classes.
 */
public final class AppVersion {

    private static final String DEV = "0.0.0-dev";

    private AppVersion() {}

    /** Current running version, or {@code "0.0.0-dev"} when unknown. */
    public static String current() {
        String v = AppVersion.class.getPackage().getImplementationVersion();
        if (v == null || v.isBlank()) return DEV;
        return v.trim();
    }

    /** True if the app was launched from an installed/packaged jar with a real version. */
    public static boolean isPackaged() {
        return !DEV.equals(current());
    }

    /**
     * Compare two dotted version strings (e.g. "1.2.3" vs "1.10.0"). Non-numeric
     * suffixes (e.g. "-SNAPSHOT") are ignored for comparison purposes.
     *
     * @return negative if {@code a < b}, zero if equal, positive if {@code a > b}.
     */
    public static int compare(String a, String b) {
        int[] pa = parts(a);
        int[] pb = parts(b);
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int va = i < pa.length ? pa[i] : 0;
            int vb = i < pb.length ? pb[i] : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int[] parts(String v) {
        if (v == null) return new int[0];
        String core = v.trim();
        if (core.startsWith("v") || core.startsWith("V")) core = core.substring(1);
        int dash = core.indexOf('-');
        if (dash >= 0) core = core.substring(0, dash);
        String[] segs = core.split("\\.");
        int[] out = new int[segs.length];
        for (int i = 0; i < segs.length; i++) {
            try { out[i] = Integer.parseInt(segs[i].replaceAll("\\D", "")); }
            catch (NumberFormatException ex) { out[i] = 0; }
        }
        return out;
    }
}
