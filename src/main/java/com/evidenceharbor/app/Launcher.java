package com.evidenceharbor.app;

/**
 * Trivial launcher that does NOT extend {@code javafx.application.Application}.
 *
 * <p>When the {@code main-class} in a jar manifest extends {@code Application},
 * the JVM performs a JavaFX module check at startup and refuses to launch with
 * "JavaFX runtime components are missing" if JavaFX isn't on the module path —
 * which is the case for shaded/fat jars produced by maven-shade-plugin.
 *
 * <p>Routing the entry point through this class bypasses that check, letting
 * the shaded JavaFX classes on the classpath initialize normally.
 */
public final class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
