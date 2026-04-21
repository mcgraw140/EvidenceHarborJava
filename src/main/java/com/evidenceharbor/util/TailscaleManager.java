package com.evidenceharbor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for detecting, launching, and controlling Tailscale on Windows.
 */
public class TailscaleManager {

    // Common install locations on Windows
    private static final String[] TAILSCALE_CLI_PATHS = {
        "C:\\Program Files\\Tailscale\\tailscale.exe",
        "C:\\Program Files (x86)\\Tailscale\\tailscale.exe"
    };

    // Installer bundled with the app
    private static final String BUNDLED_INSTALLER_PATH =
        resolveInstallerPath();

    private static String resolveInstallerPath() {
        // Try next to the jar / working directory first, then project folder
        String[] candidates = {
            "Tailscale\\tailscale-setup-1.96.3.exe",
            "tailscale-setup-1.96.3.exe"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return new File(c).getAbsolutePath();
        }
        return null;
    }

    // ──────────────────────────────────────────────────
    // Detection
    // ──────────────────────────────────────────────────

    /** Returns path to tailscale.exe if installed, or null. */
    public static String findTailscaleCli() {
        for (String p : TAILSCALE_CLI_PATHS) {
            if (new File(p).exists()) return p;
        }
        return null;
    }

    public static boolean isInstalled() {
        return findTailscaleCli() != null;
    }

    public static String getBundledInstallerPath() {
        return BUNDLED_INSTALLER_PATH;
    }

    // ──────────────────────────────────────────────────
    // Status
    // ──────────────────────────────────────────────────

    public static class TailscaleStatus {
        public final boolean running;
        public final String  selfIp;
        public final String  rawOutput;

        public TailscaleStatus(boolean running, String selfIp, String rawOutput) {
            this.running   = running;
            this.selfIp    = selfIp;
            this.rawOutput = rawOutput;
        }
    }

    /** Runs `tailscale status` and parses the result. */
    public static TailscaleStatus getStatus() throws Exception {
        String cli = findTailscaleCli();
        if (cli == null) return new TailscaleStatus(false, null, "Tailscale not installed.");

        // `tailscale status --self=true` gives a line starting with the self IP
        String raw = runCommand(cli, "status");
        if (raw == null || raw.isBlank()) {
            return new TailscaleStatus(false, null, "No output from tailscale status.");
        }

        // If Tailscale is stopped it prints something like "Tailscale is stopped."
        if (raw.toLowerCase().contains("stopped") || raw.toLowerCase().contains("not running")) {
            return new TailscaleStatus(false, null, raw);
        }

        // Parse self IP — it appears as the first IP-like token on any line
        String selfIp = parseSelfIp(raw);
        return new TailscaleStatus(true, selfIp, raw);
    }

    private static String parseSelfIp(String statusOutput) {
        // `tailscale ip -4` is cleaner — try that first
        try {
            String cli = findTailscaleCli();
            if (cli != null) {
                String ip = runCommand(cli, "ip", "-4");
                if (ip != null && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
                    return ip.trim().split("\\s+")[0];
                }
            }
        } catch (Exception ignored) {}

        // Fallback: scan status output for 100.x.x.x (Tailscale CGNAT range)
        for (String line : statusOutput.split("\\n")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(100\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})")
                .matcher(line);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    // ──────────────────────────────────────────────────
    // Actions
    // ──────────────────────────────────────────────────

    /** Connect with an optional auth key. Pass null/blank to just bring up without re-auth. */
    public static String connect(String authKey) throws Exception {
        String cli = findTailscaleCli();
        if (cli == null) throw new Exception("Tailscale not installed.");
        if (authKey != null && !authKey.isBlank()) {
            return runCommand(cli, "up", "--auth-key=" + authKey.trim());
        } else {
            return runCommand(cli, "up");
        }
    }

    /** Disconnect from Tailscale (keeps login). */
    public static String disconnect() throws Exception {
        String cli = findTailscaleCli();
        if (cli == null) throw new Exception("Tailscale not installed.");
        return runCommand(cli, "down");
    }

    /** Log out of Tailscale entirely. */
    public static String logout() throws Exception {
        String cli = findTailscaleCli();
        if (cli == null) throw new Exception("Tailscale not installed.");
        return runCommand(cli, "logout");
    }

    /**
     * Launches the bundled Tailscale installer silently.
     * Returns the process so the caller can wait or monitor it.
     */
    public static Process launchInstaller() throws Exception {
        String installer = BUNDLED_INSTALLER_PATH;
        if (installer == null || !new File(installer).exists()) {
            throw new Exception("Bundled Tailscale installer not found. Expected: Tailscale\\tailscale-setup-1.96.3.exe");
        }
        // /S = silent install (NSIS standard flag used by Tailscale)
        return new ProcessBuilder(installer, "/S")
            .redirectErrorStream(true)
            .start();
    }

    // ──────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────

    private static String runCommand(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        proc.waitFor(10, TimeUnit.SECONDS);
        return sb.toString().trim();
    }
}
