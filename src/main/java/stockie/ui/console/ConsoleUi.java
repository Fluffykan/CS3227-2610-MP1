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
import stockie.application.command.InventoryCommand;
import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.ExpiringItem;
import stockie.application.result.ExpiringBatchQueryResult;
import stockie.application.result.FindQueryResult;
import stockie.application.result.ListQueryResult;
import stockie.application.result.RecallBatchResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.SellItemResult;
import stockie.application.result.SoldBatch;
import stockie.application.result.UpdateSkuResult;
import stockie.application.command.UpdateSkuCommand;
import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.PerishableBatch;

/** Runs Stockie's command-line interface and renders its application results. */
public final class ConsoleUi {
    /** Separates chatbot messages in the console. */
    private static final String DIVIDER = "____________________________________________________________";
    /** Formats expiry dates as day-month-year. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    /** Provides UI-independent inventory operations. */
    private final StockieController controller;
    /** Required field groups for the add command; each group requires one supplied field. */
    private static final List<List<String>> ADD_REQUIRED_FIELDS = List.of(
            List.of("item"), List.of("sku"), List.of("invoice"), List.of("quantity"),
            List.of("price"));
    /** All named fields accepted by the add command, including optional fields. */
    private static final List<String> ADD_SUPPORTED_FIELDS =
            List.of("item", "sku", "invoice", "quantity", "price", "expiry", "upc");
    /** Fields accepted by the recall command; one identifier and an invoice are required. */
    private static final List<String> RECALL_SUPPORTED_FIELDS = List.of("item", "sku", "invoice");
    /** Required field groups for the recall command. */
    private static final List<List<String>> RECALL_REQUIRED_FIELDS =
            List.of(List.of("item", "sku"), List.of("invoice"));
    /** Fields accepted by the item removal command; exactly one identifier is required. */
    private static final List<String> REMOVE_SUPPORTED_FIELDS = List.of("item", "sku");
    /** Required field groups for the item removal command. */
    private static final List<List<String>> REMOVE_REQUIRED_FIELDS = List.of(List.of("item", "sku"));
    /** Fields accepted by the sell command; one identifier and a quantity are required. */
    private static final List<String> SELL_SUPPORTED_FIELDS = List.of("item", "sku", "quantity");
    /** Required field groups for the sell command. */
    private static final List<List<String>> SELL_REQUIRED_FIELDS =
            List.of(List.of("item", "sku"), List.of("quantity"));
    /** Fields accepted by the SKU update command. */
    private static final List<String> UPDATE_SKU_SUPPORTED_FIELDS =
            List.of("item", "current-sku", "sku");
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
        case "remove": removeItem(arguments, scanner); break;
        case "sell": sellItem(arguments); break;
        case "update-sku": updateSku(arguments); break;
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
        System.out.println(" remove (--item <name> | --sku <sku>)");
        System.out.println(" sell (--item <name> | --sku <sku>) --quantity <quantity>");
        System.out.println(" update-sku (--item <name> | --current-sku <old sku>) --sku <new sku>");
        System.out.println(" list [depleted | expired | expiring-in <days>]");
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

    /**
     * Parses named arguments using command-specific required field groups and supported fields.
     * Each required field group must contain at least one supplied field.
     */
    static Map<String, String> parseNamedArguments(String arguments,
            List<List<String>> requiredFieldGroups, List<String> supportedFields) {
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
        for (List<String> requiredFieldGroup : requiredFieldGroups) {
            boolean hasRequiredField = false;
            for (String field : requiredFieldGroup) {
                if (values.containsKey(field)) {
                    hasRequiredField = true;
                    break;
                }
            }
            if (!hasRequiredField) {
                if (missing.length() > 0) missing.append(", ");
                boolean firstFieldInGroup = true;
                for (String field : requiredFieldGroup) {
                    if (!firstFieldInGroup) missing.append(" or ");
                    missing.append("--").append(field);
                    firstFieldInGroup = false;
                }
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
        Map<String, String> fields = parseNamedArguments(arguments,
                RECALL_REQUIRED_FIELDS, RECALL_SUPPORTED_FIELDS);
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

    /** Confirms and removes every batch belonging to one item. */
    private void removeItem(String arguments, Scanner scanner) {
        Map<String, String> fields = parseNamedArguments(arguments,
                REMOVE_REQUIRED_FIELDS, REMOVE_SUPPORTED_FIELDS);
        if (fields == null) return;
        if (fields.containsKey("item") == fields.containsKey("sku")) {
            System.out.println(" usage: remove (--item <name> | --sku <sku>)");
            return;
        }
        InventoryItem item = fields.containsKey("item")
                ? controller.findByName(fields.get("item")).item()
                : controller.findBySku(fields.get("sku")).item();
        if (item == null) {
            String identifier = fields.containsKey("item") ? fields.get("item") : fields.get("sku");
            System.out.println(" item not found: " + identifier);
            return;
        }
        System.out.println(" warning: this will remove " + item.getDisplayName()
                + " and all of its batches. Continue? (yes/no)");
        if (!scanner.hasNextLine() || !scanner.nextLine().trim().equalsIgnoreCase("yes")) {
            System.out.println(" item removal cancelled");
            return;
        }
        RemoveItemResult result = fields.containsKey("item")
                ? controller.removeItemByName(fields.get("item"))
                : controller.removeItemBySku(fields.get("sku"));
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        System.out.println(" removed item: " + result.item().getDisplayName());
    }

    /** Sells stock selected by item name or SKU. */
    private void sellItem(String arguments) {
        Map<String, String> fields = parseNamedArguments(arguments,
                SELL_REQUIRED_FIELDS, SELL_SUPPORTED_FIELDS);
        if (fields == null) return;
        if (fields.containsKey("item") == fields.containsKey("sku")) {
            System.out.println(" usage: sell (--item <name> | --sku <sku>) --quantity <quantity>");
            return;
        }
        Integer quantity = parsePositiveQuantity(fields.get("quantity"));
        if (quantity == null) return;
        SellItemResult result = fields.containsKey("item")
                ? controller.sellItemByName(fields.get("item"), quantity)
                : controller.sellItemBySku(fields.get("sku"), quantity);
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        System.out.println(" sold: " + quantity + " of " + result.item().getDisplayName());
        for (SoldBatch batch : result.soldBatches()) {
            System.out.println(" invoice " + batch.invoiceNumber() + ": quantity " + batch.quantity());
        }
        printTotalsOnly(result.item());
    }

    /** Updates an item's SKU after selecting it by name or its current SKU. */
    private void updateSku(String arguments) {
        Map<String, String> fields = parseNamedArguments(arguments, List.of(List.of("sku")),
                UPDATE_SKU_SUPPORTED_FIELDS);
        if (fields == null) return;
        if (fields.containsKey("item") == fields.containsKey("current-sku")) {
            System.out.println(" usage: update-sku (--item <name> | --current-sku <old sku>)"
                    + " --sku <new sku>");
            return;
        }
        UpdateSkuResult result = fields.containsKey("item")
                ? controller.updateSkuByName(fields.get("item"), fields.get("sku"))
                : controller.updateSkuByCurrentSku(fields.get("current-sku"), fields.get("sku"));
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        System.out.println(" updated sku: " + result.item().getDisplayName());
        System.out.println(" old sku: " + result.oldSku());
        System.out.println(" new sku: " + result.item().getSku());
    }

    /** Parses a list request before delegating its UI-independent part to the controller. */
    private void list(String arguments) {
        if (arguments.isEmpty()) {
            displayList(controller.listItems(false));
        } else if (arguments.equalsIgnoreCase("depleted")) {
            displayList(controller.listItems(true));
        } else if (arguments.equalsIgnoreCase("expired")) {
            displayExpiringBatches(controller.listExpiredBatches());
        } else {
            String[] listArguments = arguments.split("\\s+");
            if (listArguments.length == 2 && listArguments[0].equalsIgnoreCase("expiring-in")) {
                Integer days = parseNonNegativeDays(listArguments[1]);
                if (days != null) displayExpiringBatches(controller.listExpiringBatches(days));
                return;
            }
            System.out.println(" usage: list [depleted | expired | expiring-in <days>]");
        }
    }

    /** Renders grouped perishable-batch query results. */
    private static void displayExpiringBatches(ExpiringBatchQueryResult result) {
        if (result.message() != null) {
            System.out.println(result.message());
            return;
        }
        int itemNumber = 1;
        for (ExpiringItem resultItem : result.items()) {
            InventoryItem item = resultItem.item();
            System.out.println(" " + itemNumber + ". " + item.getDisplayName());
            System.out.println("    sku: " + item.getSku());
            for (PerishableBatch batch : resultItem.batches()) {
                System.out.println("    invoice: " + batch.getInvoiceNumber());
                System.out.println("    quantity: " + batch.getQuantity());
                System.out.println("    unit price: " + formatPrice(batch.getUnitPrice()));
                if (batch.getUpc() != null) {
                    System.out.println("    upc: " + batch.getUpc());
                }
                System.out.println("    expiry date: " + DATE_FORMAT.format(batch.getExpiryDate()) + "\n");
            }
            itemNumber++;
        }
    }

    /** Parses the non-negative number of days accepted by the expiry query. */
    private static Integer parseNonNegativeDays(String text) {
        try {
            int days = Integer.parseInt(text);
            if (days >= 0) return days;
        } catch (NumberFormatException ignored) { }
        System.out.println(" days must be a non-negative whole number");
        return null;
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
        System.out.println();
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
        if (command instanceof UpdateSkuCommand) {
            UpdateSkuCommand skuCommand = (UpdateSkuCommand) command;
            System.out.println(" " + action + ": " + skuCommand.getItemName());
            System.out.println(" old sku: " + skuCommand.getOldSku());
            System.out.println(" new sku: " + skuCommand.getNewSku());
            return;
        }
        if (command.getAffectedItem() != null) {
            System.out.println(" " + action + " item:");
            System.out.println(" item: " + command.getItemName());
            printItemDetails(command.getAffectedItem());
            return;
        }
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
