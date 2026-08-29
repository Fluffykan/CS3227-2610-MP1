package stockie.ui.javafx.command;

import stockie.ui.javafx.util.InventoryRow;

/** Carries command output and the JavaFX effects that the caller should apply. */
public record FxCommandResult(String message, boolean refreshRequired, InventoryRow selectedRow) {
    /** Creates a result with no additional UI effect. */
    public static FxCommandResult message(String message) {
        return new FxCommandResult(message, false, null);
    }

    /** Creates a result that requires the current inventory view to be refreshed. */
    public static FxCommandResult refresh(String message) {
        return new FxCommandResult(message, true, null);
    }

    /** Creates a result that selects a specific inventory row. */
    public static FxCommandResult select(String message, InventoryRow row) {
        return new FxCommandResult(message, false, row);
    }
}
