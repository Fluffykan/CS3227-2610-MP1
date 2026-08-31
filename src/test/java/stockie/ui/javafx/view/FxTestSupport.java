package stockie.ui.javafx.view;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javafx.application.Platform;

/** Provides helpers for testing JavaFX controls on the JavaFX application thread. */
final class FxTestSupport {
    private static boolean toolkitStarted;

    private FxTestSupport() {
    }

    static synchronized void startToolkit() throws InterruptedException {
        if (toolkitStarted) {
            return;
        }
        CountDownLatch startupLatch = new CountDownLatch(1);
        try {
            Platform.startup(startupLatch::countDown);
        } catch (IllegalStateException exception) {
            startupLatch.countDown();
        }
        startupLatch.await();
        toolkitStarted = true;
    }

    static void runAndWait(Runnable action) throws InterruptedException {
        call(() -> {
            action.run();
            return null;
        });
    }

    static <T> T call(Supplier<T> action) throws InterruptedException {
        startToolkit();
        if (Platform.isFxApplicationThread()) {
            return action.get();
        }

        CountDownLatch actionLatch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                actionLatch.countDown();
            }
        });
        actionLatch.await();
        if (failure.get() != null) {
            throw new AssertionError("JavaFX action failed", failure.get());
        }
        return result.get();
    }
}
