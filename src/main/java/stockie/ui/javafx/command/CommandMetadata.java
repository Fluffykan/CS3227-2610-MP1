package stockie.ui.javafx.command;

import java.util.Arrays;
import java.util.Locale;

/** Describes the commands supported by the JavaFX command panel. */
public enum CommandMetadata {
    HELP("help", Category.SYSTEM, "Show the available commands."),
    BYE("bye", Category.SYSTEM, "Close the application."),
    UNDO("undo", Category.HISTORY, "Undo the last inventory change."),
    REDO("redo", Category.HISTORY, "Redo the last undone inventory change."),
    LIST("list", Category.QUERY, "List inventory items."),
    FIND("find", Category.QUERY, "Find inventory items."),
    ADD("add", Category.INVENTORY, "Add a batch to inventory."),
    SELL("sell", Category.INVENTORY, "Sell inventory items."),
    RECALL("recall", Category.INVENTORY, "Recall a batch."),
    REMOVE("remove", Category.INVENTORY, "Remove a batch."),
    UPDATE_SKU("update-sku", Category.INVENTORY, "Update an item's SKU.");

    private final String name;
    private final Category category;
    private final String description;

    CommandMetadata(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    /** Returns the command name accepted by the CLI. */
    public String commandName() {
        return name;
    }

    /** Returns the command category used to identify its owning handler. */
    public Category category() {
        return category;
    }

    /** Returns a short description suitable for help output. */
    public String description() {
        return description;
    }

    /** Finds metadata for a command name, or returns {@code null} if unknown. */
    public static CommandMetadata fromName(String commandName) {
        String normalizedName = commandName.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(command -> command.name.equals(normalizedName))
                .findFirst()
                .orElse(null);
    }

    /** Groups commands by the part of the application that handles them. */
    public enum Category {
        SYSTEM,
        HISTORY,
        QUERY,
        INVENTORY
    }
}
