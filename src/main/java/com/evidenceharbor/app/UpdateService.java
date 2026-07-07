package com.evidenceharbor.app;

import com.evidenceharbor.util.Dialogs;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks GitHub Releases for a newer build, downloads it, and swaps the
 * running JAR via a self-deleting Windows helper script.
 *
 * <p>Release convention on GitHub:
 * <ul>
 *   <li>Tag name is the version, e.g. {@code v1.2.3} or {@code 1.2.3}.</li>
 *   <li>Attach the built shaded JAR as a release asset, e.g. {@code evidence-harbor-1.2.3.jar}.</li>
 * </ul>
 *
 * <p>The updater only runs when the app was launched from a real packaged JAR
 * (see {@link AppVersion#isPackaged()}). In dev / IDE runs it is a no-op.
 */
public final class UpdateService {

    private static final String OWNER = "mcgraw140";
    private static final String REPO  = "EvidenceHarborJava";
    private static final String API_URL =
            "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";

    /** User-agent required by the GitHub API. */
    private static final String UA = "EvidenceHarbor-Updater";

    private UpdateService() {}

    /**
     * Non-blocking update check. Silent if already up to date, offline, or
     * running in dev mode. If an update is available, prompts the user on the
     * JavaFX thread with release notes and Install / Later buttons.
     */
    public static void checkAsync() {
        Task<Optional<Release>> task = new Task<>() {
            @Override
            protected Optional<Release> call() {
                if (!AppVersion.isPackaged()) return Optional.empty();
                try {
                    return fetchLatest();
                } catch (Exception ex) {
                    // Network offline / rate-limited / GitHub down — swallow silently.
                    return Optional.empty();
                }
            }
        };
        task.setOnSucceeded(e -> {
            Optional<Release> r = task.getValue();
            if (r.isEmpty()) return;
            Release rel = r.get();
            if (AppVersion.compare(AppVersion.current(), rel.version) >= 0) return;
            promptAndInstall(rel);
        });
        Thread t = new Thread(task, "EvidenceHarbor-UpdateCheck");
        t.setDaemon(true);
        t.start();
    }

    /** Manual "Check for updates" trigger — always reports the result. */
    public static void checkInteractive() {
        Task<Optional<Release>> task = new Task<>() {
            @Override
            protected Optional<Release> call() throws Exception {
                return fetchLatest();
            }
        };
        task.setOnSucceeded(e -> {
            Optional<Release> r = task.getValue();
            if (r.isEmpty()) {
                Dialogs.info("Up to date",
                        "No releases were found for this app on GitHub.");
                return;
            }
            Release rel = r.get();
            if (AppVersion.compare(AppVersion.current(), rel.version) >= 0) {
                Dialogs.info("Up to date",
                        "You're running the latest version (" + AppVersion.current() + ").");
                return;
            }
            promptAndInstall(rel);
        });
        task.setOnFailed(e -> Dialogs.error("Update check failed",
                task.getException() == null ? "Unknown error" : task.getException().getMessage()));
        Thread t = new Thread(task, "EvidenceHarbor-UpdateCheck-Interactive");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    private static Optional<Release> fetchLatest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", UA)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return Optional.empty();
        return parseRelease(res.body());
    }

    /** Minimal, tolerant JSON extraction — avoids a hard dependency on a JSON library. */
    static Optional<Release> parseRelease(String json) {
        if (json == null || json.isBlank()) return Optional.empty();
        String tag = extract(json, "\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        if (tag == null) return Optional.empty();
        String name = extract(json, "\"name\"\\s*:\\s*\"([^\"]*)\"");
        String body = extract(json, "\"body\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        String htmlUrl = extract(json, "\"html_url\"\\s*:\\s*\"([^\"]+)\"");
        boolean prerelease = "true".equalsIgnoreCase(extract(json, "\"prerelease\"\\s*:\\s*(true|false)"));
        if (prerelease) return Optional.empty();

        String jarUrl = null;
        Pattern assets = Pattern.compile(
                "\"name\"\\s*:\\s*\"([^\"]+\\.jar)\"[^\\}]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"",
                Pattern.DOTALL);
        Matcher m = assets.matcher(json);
        if (m.find()) jarUrl = m.group(2);
        if (jarUrl == null) return Optional.empty();

        Release r = new Release();
        r.version   = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        r.name      = (name == null || name.isBlank()) ? ("Version " + r.version) : unescape(name);
        r.notes     = body == null ? "" : unescape(body);
        r.htmlUrl   = htmlUrl == null ? "" : htmlUrl;
        r.jarUrl    = jarUrl;
        return Optional.of(r);
    }

    private static String extract(String json, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String unescape(String s) {
        return s.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static void promptAndInstall(Release rel) {
        Alert a = Dialogs.alert(AlertType.CONFIRMATION,
                "Update available",
                "Evidence Harbor " + rel.version + " is available",
                "You're currently running " + AppVersion.current()
                        + ". Would you like to install " + rel.version + " now?",
                ButtonType.YES, ButtonType.NO);

        // Attach the release notes in a scrollable area.
        if (rel.notes != null && !rel.notes.isBlank()) {
            TextArea notes = new TextArea(rel.notes);
            notes.setEditable(false);
            notes.setWrapText(true);
            notes.setPrefRowCount(10);
            notes.setPrefColumnCount(60);
            VBox box = new VBox(6, notes);
            box.setPrefWidth(560);
            a.getDialogPane().setExpandableContent(box);
            a.getDialogPane().setExpanded(true);
        }

        Optional<ButtonType> res = a.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        performInstall(rel);
    }

    private static void performInstall(Release rel) {
        Path runningJar;
        try {
            runningJar = locateRunningJar();
        } catch (Exception ex) {
            Dialogs.error("Update failed",
                    "Couldn't locate the running application JAR:\n" + ex.getMessage());
            return;
        }
        if (runningJar == null) {
            Dialogs.error("Update failed",
                    "This looks like a development run — updates only work on installed builds.");
            return;
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("eh-update-");
        } catch (IOException ex) {
            Dialogs.error("Update failed", "Couldn't create a temp folder: " + ex.getMessage());
            return;
        }
        Path downloadTo = tempDir.resolve("evidence-harbor-" + rel.version + ".jar");

        // Simple progress dialog.
        Alert progress = Dialogs.alert(AlertType.INFORMATION, "Downloading update",
                "Downloading Evidence Harbor " + rel.version,
                "Please wait — the app will restart automatically.");
        progress.getButtonTypes().setAll(ButtonType.CANCEL);
        ProgressBar bar = new ProgressBar(-1);
        bar.setPrefWidth(400);
        progress.getDialogPane().setContent(bar);

        Task<Void> dl = new Task<>() {
            @Override
            protected Void call() throws Exception {
                downloadWithProgress(rel.jarUrl, downloadTo, this);
                return null;
            }
        };
        bar.progressProperty().bind(dl.progressProperty());

        dl.setOnSucceeded(e -> {
            progress.close();
            try {
                launchUpdaterAndExit(runningJar, downloadTo);
            } catch (Exception ex) {
                Dialogs.error("Update failed",
                        "Couldn't launch the installer script:\n" + ex.getMessage());
            }
        });
        dl.setOnFailed(e -> {
            progress.close();
            Dialogs.error("Update failed",
                    "Download failed: "
                            + (dl.getException() == null ? "unknown error" : dl.getException().getMessage()));
        });
        dl.setOnCancelled(e -> progress.close());

        Thread t = new Thread(dl, "EvidenceHarbor-UpdateDownload");
        t.setDaemon(true);
        t.start();

        progress.showAndWait();
        if (dl.isRunning()) dl.cancel();
    }

    private static void downloadWithProgress(String url, Path target, Task<?> task) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/octet-stream")
                .GET()
                .build();
        HttpResponse<java.io.InputStream> res =
                client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + res.statusCode() + " while downloading " + url);
        }
        long total = res.headers().firstValueAsLong("content-length").orElse(-1L);
        try (var in = res.body();
             var out = Files.newOutputStream(target)) {
            byte[] buf = new byte[64 * 1024];
            long read = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                if (task.isCancelled()) throw new IOException("Cancelled");
                out.write(buf, 0, n);
                read += n;
                if (total > 0) {
                    final long r = read;
                    Platform.runLater(() -> { /* progress updated by binding below */ });
                    updateProgressReflective(task, r, total);
                }
            }
        }
    }

    /** javafx.concurrent.Task#updateProgress(long,long) is protected; expose via reflection. */
    private static void updateProgressReflective(Task<?> task, long done, long total) {
        try {
            var m = Task.class.getDeclaredMethod("updateProgress", long.class, long.class);
            m.setAccessible(true);
            m.invoke(task, done, total);
        } catch (Exception ignored) {}
    }

    private static Path locateRunningJar() throws Exception {
        var src = UpdateService.class.getProtectionDomain().getCodeSource();
        if (src == null) return null;
        var loc = src.getLocation();
        if (loc == null) return null;
        Path p = Paths.get(loc.toURI());
        // Only accept if it looks like a jar (dev runs return a classes/ dir).
        String name = p.getFileName() == null ? "" : p.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".jar")) return null;
        return p;
    }

    private static void launchUpdaterAndExit(Path runningJar, Path newJar) throws IOException {
        // Prefer the jpackage-produced launcher .exe two folders up
        // (<install>\app\<jar>  →  <install>\Evidence Harbor.exe).
        Path relaunchTarget = findRelaunchTarget(runningJar);

        Path bat = Files.createTempFile("eh-update-", ".bat");
        String script = buildUpdaterScript(runningJar, newJar, relaunchTarget, bat);
        Files.writeString(bat, script);

        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c",
                "start", "\"\"", "/min", bat.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        pb.start();

        // Give the shell a moment to spawn, then exit so the file lock is released.
        Platform.exit();
        // Belt-and-suspenders — some subsystems keep JavaFX alive.
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            System.exit(0);
        }, "EvidenceHarbor-Exit").start();
    }

    private static Path findRelaunchTarget(Path runningJar) {
        try {
            // <install>\app\<jar> → look for <install>\Evidence Harbor.exe
            Path installDir = runningJar.getParent() == null ? null : runningJar.getParent().getParent();
            if (installDir != null) {
                Path exe = installDir.resolve("Evidence Harbor.exe");
                if (Files.isRegularFile(exe)) return exe;
                // Fallback: any *.exe next to the app folder
                try (var s = Files.list(installDir)) {
                    var any = s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe"))
                              .findFirst();
                    if (any.isPresent()) return any.get();
                }
            }
        } catch (IOException ignored) {}
        return runningJar; // last resort — will be re-launched via javaw
    }

    private static String buildUpdaterScript(Path runningJar, Path newJar, Path relaunchTarget, Path self) {
        String q = "\"";
        String relaunchCmd = relaunchTarget.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe")
                ? "start \"\" " + q + relaunchTarget + q
                : "start \"\" javaw -jar " + q + runningJar + q;

        return String.join("\r\n",
                "@echo off",
                "setlocal",
                "set " + q + "SRC=" + newJar + q,
                "set " + q + "DST=" + runningJar + q,
                "set /a TRIES=0",
                ":loop",
                "if %TRIES% GEQ 60 goto giveup",
                "copy /y " + q + "%SRC%" + q + " " + q + "%DST%" + q + " >nul 2>&1",
                "if not errorlevel 1 goto ok",
                "set /a TRIES+=1",
                "timeout /t 1 /nobreak >nul",
                "goto loop",
                ":ok",
                "del " + q + "%SRC%" + q + " >nul 2>&1",
                relaunchCmd,
                "goto done",
                ":giveup",
                "echo Update failed - could not replace " + q + "%DST%" + q + ".",
                "echo The new build is at " + q + "%SRC%" + q + ".",
                "pause",
                ":done",
                "del " + q + self.toAbsolutePath() + q + " >nul 2>&1",
                "endlocal",
                ""
        );
    }

    // ─────────────────────────────────────────────────────────────────────

    /** Minimal DTO describing the latest GitHub release. */
    public static final class Release {
        public String version;
        public String name;
        public String notes;
        public String htmlUrl;
        public String jarUrl;
    }
}
