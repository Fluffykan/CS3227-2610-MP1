package stockie.application.result;

import stockie.application.command.InventoryCommand;

/** Reports a history command or a reason it could not be performed. */
public record CommandResult(InventoryCommand command, String message) {
    /** Returns the failure reason, if the history operation was not completed. */
    public String errorMessage() {
        return message;
    }
}
