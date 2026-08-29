package stockie.ui.javafx.CommandHandlers;

import stockie.application.controller.StockieController;
import stockie.application.result.CommandResult;
import stockie.ui.javafx.command.FxCommandResult;

/** Handles undo and redo commands. */
public final class FxHistoryCommandHandler {
    private final StockieController controller;

    public FxHistoryCommandHandler(StockieController controller) {
        this.controller = controller;
    }

    public FxCommandResult undo() {
        return execute(controller.undo(), "Undo");
    }

    public FxCommandResult redo() {
        return execute(controller.redo(), "Redo");
    }

    private FxCommandResult execute(CommandResult result, String action) {
        if (result.errorMessage() != null) {
            return FxCommandResult.message(result.errorMessage().trim() + "\n");
        }
        return FxCommandResult.refresh(action + " applied.\n");
    }
}
