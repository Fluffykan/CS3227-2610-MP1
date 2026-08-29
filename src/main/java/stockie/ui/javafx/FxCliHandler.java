package stockie.ui.javafx;

import java.util.function.Function;

import stockie.application.controller.StockieController;
import stockie.entities.InventoryItem;
import stockie.ui.javafx.CommandHandlers.FxHistoryCommandHandler;
import stockie.ui.javafx.CommandHandlers.FxInventoryCommandHandler;
import stockie.ui.javafx.CommandHandlers.FxInventoryQueryHandler;
import stockie.ui.javafx.command.CommandConstants;
import stockie.ui.javafx.command.CommandMetadata;
import stockie.ui.javafx.command.CommandParser;
import stockie.ui.javafx.command.FxCommandResult;
import stockie.ui.javafx.util.InventoryRow;

/** Dispatches commands entered in the JavaFX command panel. */
public final class FxCliHandler {
    private final FxInventoryCommandHandler inventoryCommands;
    private final FxInventoryQueryHandler queryCommands;
    private final FxHistoryCommandHandler historyCommands;
    private final Runnable closeApplication;

    /** Creates a handler connected to the controller and application-close callback. */
    public FxCliHandler(StockieController controller, Runnable closeApplication,
            Function<InventoryItem, InventoryRow> itemMapper) {
        this.inventoryCommands = new FxInventoryCommandHandler(controller);
        this.queryCommands = new FxInventoryQueryHandler(controller, itemMapper);
        this.historyCommands = new FxHistoryCommandHandler(controller);
        this.closeApplication = closeApplication;
    }

    /** Executes one command-line input and returns its message and UI effects. */
    public FxCommandResult execute(String input) {
        CommandParser.ParsedCommand parsedCommand = CommandParser.parse(input);
        String arguments = parsedCommand.arguments();
        CommandMetadata command = CommandMetadata.fromName(parsedCommand.command());
        if (command == null) {
            return FxCommandResult.message(CommandConstants.UNKNOWN_COMMAND_MESSAGE);
        }
        return switch (command) {
        case HELP -> help();
        case BYE -> close();
        case UNDO -> historyCommands.undo();
        case REDO -> historyCommands.redo();
        case LIST -> queryCommands.list(arguments);
        case FIND -> queryCommands.find(arguments);
        case ADD -> inventoryCommands.add(arguments);
        case SELL -> inventoryCommands.sell(arguments);
        case RECALL -> inventoryCommands.recall(arguments);
        case REMOVE -> inventoryCommands.remove(arguments);
        case UPDATE_SKU -> inventoryCommands.updateSku(arguments);
        };
    }

    /** Builds help text from the command metadata so it cannot drift from dispatch behavior. */
    private FxCommandResult help() {
        StringBuilder help = new StringBuilder();
        for (CommandMetadata command : CommandMetadata.values()) {
            help.append(command.commandName()).append(" - ").append(command.description()).append('\n');
        }
        help.append(CommandConstants.HELP_OPTIONS_MESSAGE);
        return FxCommandResult.message(help.toString());
    }

    /** Closes the application and returns the session termination message. */
    private FxCommandResult close() {
        closeApplication.run();
        return FxCommandResult.message(CommandConstants.GOODBYE_MESSAGE);
    }
}
