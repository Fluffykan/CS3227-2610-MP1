package stockie.ui.javafx.command;

/** Shared command-line option names and messages used by the JavaFX command handlers. */
public final class CommandConstants {
    public static final String ITEM = "item";
    public static final String SKU = "sku";
    public static final String CURRENT_SKU = "current-sku";
    public static final String INVOICE = "invoice";
    public static final String QUANTITY = "quantity";
    public static final String PRICE = "price";
    public static final String EXPIRY = "expiry";
    public static final String UPC = "upc";

    public static final String UNKNOWN_COMMAND_MESSAGE =
            "Unknown command. Type help for available commands.\n";
    public static final String HELP_OPTIONS_MESSAGE =
            "Use --item, --sku, --quantity, --invoice, --price, --expiry, and --upc.\n";
    public static final String GOODBYE_MESSAGE = "Goodbye.\n";

    private CommandConstants() {
    }
}
