package stockie.ui.console;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import stockie.command.InventoryCommand;
import stockie.application.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.FindQueryResult;
import stockie.application.result.ListQueryResult;
import stockie.application.result.RecallBatchResult;
import stockie.model.Batch;
import stockie.model.InventoryItem;
import stockie.model.PerishableBatch;

/** Runs Stockie's command-line interface and renders its application results. */
public final class ConsoleUi {
    /** Separates chatbot messages in the console. */
    private static final String DIVIDER = "____________________________________________________________";
    /** Formats expiry dates as day-month-year. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    /** Provides UI-independent inventory operations. */
    private final StockieController controller;
    /** Required named fields accepted by the add command. */
    private static final List<String> ADD_REQUIRED_FIELDS =
            List.of("item", "sku", "invoice", "quantity", "price");
    /** All named fields accepted by the add command, including optional fields. */
    private static final List<String> ADD_SUPPORTED_FIELDS =
            List.of("item", "sku", "invoice", "quantity", "price", "expiry", "upc");
    /** Fields accepted by the recall command; one identifier and an invoice are required. */
    private static final List<String> RECALL_SUPPORTED_FIELDS = List.of("item", "sku", "invoice");
    /** Fields accepted by the find command; exactly one must be supplied. */
    private static final List<String> FIND_SUPPORTED_FIELDS = List.of("item", "sku");

    public ConsoleUi(StockieController controller) {
        this.controller = controller;
    }

    /** Greets the user, processes commands, and exits on {@code bye}. */
    public void start() {
        try {
            controller.load();
        } catch (Exception exception) {
            System.out.println(" Unable to load saved inventory; starting with an empty inventory.");
        }
        String banner = " ____  _             _    _      \n"
                + "/ ___|| |_ ___   ___| | _(_) ___ \n"
                + "\\___ \\| __/ _ \\ / __| |/ / |/ _ \\\n"
                + " ___) | || (_) | (__|   <| |  __/\n"
                + "|____/ \\__\\___/ \\___|_|\\_\\_|\\___|\n";
        printDivider();
        System.out.println(banner);
        System.out.println("Hello! I'm Stockie.");
        System.out.println("What can I do for you?");
        printDivider();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("bye")) {
                System.out.println("Bye. Hope to see you again soon!");
                printDivider();
                break;
            }
            printDivider();
            processCommand(input, scanner);
            printDivider();
        }
        scanner.close();
    }

    /** Routes a command using its first whitespace-delimited word. */
    private void processCommand(String input, Scanner scanner) {
        if (input.isEmpty()) return;
        int separator = input.indexOf(' ');
        String command = (separator < 0 ? input : input.substring(0, separator)).toLowerCase(Locale.ROOT);
        String arguments = separator < 0 ? "" : input.substring(separator + 1).trim();
        switch (command) {
        case "add": addBatch(arguments, scanner); break;
        case "recall": recallBatch(arguments); break;
        case "list": list(arguments); break;
        case "find": find(arguments); break;
        case "undo": undo(); break;
        case "redo": redo(); break;
        case "help": printHelp(arguments); break;
        default:
            System.out.println(" I'm not too sure what you mean, did you use the right command?");
            System.out.println(" Type \"help\" to find the list of commands available");
            break;
        }
    }

    /** Prints the commands supported by Stockie. */
    private static void printHelp(String arguments) {
        if (!arguments.isEmpty()) {
            System.out.println(" usage: help");
            return;
        }
        System.out.println(" Available commands:");
        System.out.println(" add --item <name> --sku <sku> --invoice <invoice>"
                + " --quantity <quantity> --price <price> [--expiry <dd-MM-yyyy>] [--upc <upc>]");
        System.out.println(" recall (--item <name> | --sku <sku>) --invoice <invoice>");
        System.out.println(" list [depleted]");
        System.out.println(" find --item <name> | --sku <sku>");
        System.out.println(" undo");
        System.out.println(" redo");
        System.out.println(" help");
        System.out.println(" bye");
    }

    /** Adds a batch using order-independent {@code --field value} arguments. */
    private void addBatch(String arguments, Scanner scanner) {
        Map<String, String> fields = parseNamedArguments(arguments,
                ADD_REQUIRED_FIELDS, ADD_SUPPORTED_FIELDS);
        if (fields == null) {
            return;
        }
        String itemName = fields.get("item");
        String sku = fields.get("sku");
        String invoiceNumber = fields.get("invoice");
        Integer quantity = parsePositiveQuantity(fields.get("quantity"));
        BigDecimal unitPrice = parseNonNegativePrice(fields.get("price"));
        String expiryText = fields.get("expiry");
        LocalDate expiryDate = parseExpiryDate(expiryText);
        boolean hasExpiry = expiryText != null;
        String upc = fields.get("upc");
        if (quantity == null || unitPrice == null || (hasExpiry && expiryDate == null)) return;

        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            System.out.println(" warning: this batch expired on "
                    + DATE_FORMAT.format(expiryDate) + ". Add it anyway? (yes/no)");
            if (!scanner.hasNextLine() || !scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                System.out.println(" addition cancelled");
                return;
            }
        }

        AddBatchResult result = controller.addBatch(new AddBatchRequest(itemName, sku, invoiceNumber,
                quantity, unitPrice, expiryDate, upc));
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        printTotals(" added: " + itemName, result.item());
    }

    /** Parses named arguments using command-specific required and supported fields. */
    static Map<String, String> parseNamedArguments(String arguments,
            List<String> requiredFields, List<String> supportedFields) {
        String[] tokens = arguments.trim().isEmpty() ? new String[0] : arguments.trim().split("\\s+");
        HashMap<String, String> values = new HashMap<>();
        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();

        for (String token : tokens) {
            if (token.startsWith("--")) {
                if (currentKey != null && !storeArgument(values, currentKey, currentValue.toString())) {
                    return null;
                }
                currentKey = token.substring(2).toLowerCase(Locale.ROOT);
                if (!isSupportedArgument(currentKey, supportedFields)) {
                    System.out.println(" unknown field: --" + currentKey);
                    return null;
                }
                currentValue.setLength(0);
            } else if (currentKey == null) {
                System.out.println(" values must follow a named field such as --item");
                return null;
            } else {
                if (currentValue.length() > 0) currentValue.append(' ');
                currentValue.append(token);
            }
        }
        if (currentKey != null && !storeArgument(values, currentKey, currentValue.toString())) {
            return null;
        }

        StringBuilder missing = new StringBuilder();
        for (String field : requiredFields) {
            if (!values.containsKey(field)) {
                if (missing.length() > 0) missing.append(", ");
                missing.append("--").append(field);
            }
        }
        if (missing.length() > 0) {
            System.out.println(" missing required fields: " + missing);
            return null;
        }
        return values;
    }

    /** Stores one parsed argument after checking that it is non-empty and unique. */
    private static boolean storeArgument(Map<String, String> values, String key, String value) {
        if (value.trim().isEmpty()) {
            System.out.println(" field --" + key + " requires a value");
            return false;
        }
        if (values.containsKey(key)) {
            System.out.println(" duplicate field: --" + key);
            return false;
        }
        values.put(key, value.trim());
        return true;
    }

    /** Returns whether a named field is included in the command's supported fields. */
    private static boolean isSupportedArgument(String key, List<String> supportedFields) {
        return supportedFields.contains(key);
    }

    /** Recalls an entire batch using an item name or SKU and an invoice number. */
    private void recallBatch(String arguments) {
        Map<String, String> fields = parseNamedArguments(arguments, List.of("invoice"), RECALL_SUPPORTED_FIELDS);
        if (fields == null) return;
        if (fields.containsKey("item") == fields.containsKey("sku")) {
            System.out.println(" usage: recall (--item <name> | --sku <sku>) --invoice <invoice>");
            return;
        }
        String invoiceNumber = fields.get("invoice");
        RecallBatchResult result = fields.containsKey("item")
                ? controller.recallBatchByName(fields.get("item"), invoiceNumber)
                : controller.recallBatchBySku(fields.get("sku"), invoiceNumber);
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        InventoryItem updatedItem = result.item();
        System.out.println(" recalled: " + invoiceNumber);
        printTotalsOnly(updatedItem);
        if (updatedItem.getTotalQuantity() == 0) {
            System.out.println(" out of stock: " + updatedItem.getDisplayName());
        }
    }

    /** Parses a list request before delegating its UI-independent part to the controller. */
    private void list(String arguments) {
        if (arguments.isEmpty()) {
            displayList(controller.listItems(false));
        } else if (arguments.equalsIgnoreCase("depleted")) {
            displayList(controller.listItems(true));
        } else {
            System.out.println(" usage: list [depleted]");
        }
    }

    /** Parses a find request before delegating the lookup to the controller. */
    private void find(String arguments) {
        Map<String, String> fields = parseNamedArguments(arguments, List.of(), FIND_SUPPORTED_FIELDS);
        if (fields == null) return;
        if (fields.size() != 1) {
            System.out.println(" usage: find --item <name> or find --sku <sku>");
            return;
        }
        displayFind(fields.containsKey("item") ? controller.findByName(fields.get("item"))
                : controller.findBySku(fields.get("sku")));
    }

    /** Renders the result of a list query without changing application state. */
    private static void displayList(ListQueryResult result) {
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        int itemNumber = 1;
        for (InventoryItem item : result.items()) {
            System.out.println(" " + itemNumber + ". " + item.getDisplayName());
            printItemDetails(item);
            itemNumber++;
        }
    }

    /** Renders the result of a find query without changing application state. */
    private static void displayFind(FindQueryResult result) {
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        printItemDetails(result.item());
    }

    /** Undoes the most recent successful change. */
    private void undo() {
        renderHistoryResult(controller.undo(), true);
    }

    /** Redoes the most recently undone change. */
    private void redo() {
        renderHistoryResult(controller.redo(), false);
    }

    /** Renders either a completed undo/redo operation or its failure message. */
    private void renderHistoryResult(CommandResult result, boolean undo) {
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        InventoryCommand command = result.command();
        printCommandDetails(undo ? command.getUndoAction() : command.getRedoAction(), command);
    }

    /** Prints the fields, totals, and batches belonging to one inventory item. */
    private static void printItemDetails(InventoryItem item) {
        System.out.println("    sku: " + item.getSku());
        System.out.println("    category: " + item.getCategory().name().toLowerCase(Locale.ROOT));
        System.out.println("    total quantity: " + item.getTotalQuantity());
        System.out.println("    inventory cost: " + formatPrice(item.getTotalCost()));
        for (Batch batch : item.getBatches().values()) {
            System.out.println("    invoice " + batch.getInvoiceNumber()
                    + ": quantity " + batch.getQuantity()
                    + ", unit price " + formatPrice(batch.getUnitPrice())
                    + formatUpc(batch)
                    + formatExpiry(batch));
        }
    }

    /** Prints an acknowledgement followed by updated totals. */
    private static void printTotals(String acknowledgement, InventoryItem item) {
        System.out.println(acknowledgement);
        printTotalsOnly(item);
    }

    /** Prints only the aggregate totals for an item. */
    private static void printTotalsOnly(InventoryItem item) {
        System.out.println(" total quantity: " + item.getTotalQuantity());
        System.out.println(" inventory cost: " + formatPrice(item.getTotalCost()));
    }

    /** Prints the affected batch followed by aggregate totals after undo or redo. */
    private void printCommandDetails(String action, InventoryCommand command) {
        Batch batch = command.getAffectedBatch();
        System.out.println(" " + action + " batch:");
        System.out.println(" item: " + command.getItemName());
        System.out.println(" sku: " + command.getSku());
        System.out.println(" category: " + command.getCategory().name().toLowerCase(Locale.ROOT));
        System.out.println(" invoice: " + batch.getInvoiceNumber());
        System.out.println(" quantity: " + batch.getQuantity());
        System.out.println(" unit price: " + formatPrice(batch.getUnitPrice()));
        if (batch.getUpc() != null) {
            System.out.println(" upc: " + batch.getUpc());
        }
        if (batch instanceof PerishableBatch) {
            System.out.println(" expiry date: " + DATE_FORMAT.format(((PerishableBatch) batch).getExpiryDate()));
        }
        System.out.println();
        printCommandTotals(command);
    }

    /** Prints aggregate totals after an undo or redo operation. */
    private void printCommandTotals(InventoryCommand command) {
        InventoryItem item = controller.findByName(command.getItemName()).item();
        if (item == null) {
            System.out.println(" total quantity: 0");
            System.out.println(" inventory cost: 0.00");
        } else {
            printTotalsOnly(item);
        }
    }

    /** Parses a strictly positive quantity. */
    private static Integer parsePositiveQuantity(String text) {
        if (text == null) {
            System.out.println(" quantity must be a positive whole number");
            return null;
        }
        try {
            int quantity = Integer.parseInt(text);
            if (quantity > 0) return quantity;
        } catch (NumberFormatException ignored) { }
        System.out.println(" quantity must be a positive whole number");
        return null;
    }

    /** Parses a non-negative unit price. */
    private static BigDecimal parseNonNegativePrice(String text) {
        if (text == null) {
            System.out.println(" unit price must be a non-negative number");
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(text);
            if (price.signum() >= 0) return price;
        } catch (NumberFormatException ignored) { }
        System.out.println(" unit price must be a non-negative number");
        return null;
    }

    /** Parses an optional expiry date in dd-MM-yyyy format. */
    private static LocalDate parseExpiryDate(String text) {
        if (text == null) return null;
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            System.out.println(" expiry date must use DD-MM-YYYY");
            return null;
        }
    }

    /** Formats monetary values to two decimal places. */
    private static String formatPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Formats the expiry field for a perishable batch. */
    private static String formatExpiry(Batch batch) {
        if (batch instanceof PerishableBatch) {
            return ", expiry date " + DATE_FORMAT.format(((PerishableBatch) batch).getExpiryDate());
        }
        return "";
    }

    /** Formats the optional UPC field for a batch. */
    private static String formatUpc(Batch batch) {
        return batch.getUpc() == null ? "" : ", upc " + batch.getUpc();
    }

    /** Prints a message divider. */
    private static void printDivider() { System.out.println(DIVIDER); }

}
