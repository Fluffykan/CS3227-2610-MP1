package stockie.ui.javafx;

import javafx.application.Application;

/**
 * Bootstrap launcher for running the JavaFX app from an executable JAR.
 *
 * <p>The executable JAR's {@code Main-Class} should point to this class (not directly
 * to {@link StockieFxApp}) so the JVM does not take the legacy JavaFX launch path that
 * expects JavaFX to be present in the JDK runtime image.</p>
 */
public final class StockieFxLauncher {

    private StockieFxLauncher() {
        // Utility class.
    }

    /** Starts the JavaFX application. */
    public static void main(String[] args) {
        Application.launch(StockieFxApp.class, args);
    }
}
