import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Starts the Stockie chatbot application.
 */
public class Stockie {
    /** Maximum number of distinct items that can be tracked. */
    private static final int MAX_ITEMS = 100;

    /** Separates the chatbot's messages in the console. */
    private static final String DIVIDER = "____________________________________________________________";

    /** Maps normalized item names to their batch inventory. */
    private static final HashMap<String, ItemInventory> inventory = new HashMap<>();

    /** Parses an add command with an item name followed by three fixed fields (invoice, quantity, unit price). */
    private static final Pattern ADD_ARGUMENTS = Pattern.compile("^(.+?)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)$");

    /** Parses a remove command with an item name followed by an invoice number. */
    private static final Pattern REMOVE_ARGUMENTS = Pattern.compile("^(.+?)\\s+(\\S+)$");

    /**
     * Greets the user, processes commands, and exits when they enter {@code bye}.
     *
     * @param args command-line arguments (not currently used)
     */
    public static void main(String[] args) {
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
            processCommand(input);
            printDivider();
        }
        scanner.close();
    }

    /**
     * Processes one user command and prints the corresponding response.
     *
     * @param input trimmed command text
     */
    private static void processCommand(String input) {
        if (input.isEmpty()) {
            return;
        }

        int separator = input.indexOf(' ');
        String command = (separator < 0 ? input : input.substring(0, separator)).toLowerCase(Locale.ROOT);
        String arguments = separator < 0 ? "" : input.substring(separator + 1).trim();
        switch (command) {
        case "add":
            addBatch(arguments);
            break;
        case "remove":
            removeBatch(arguments);
            break;
        case "list":
            listItems(arguments);
            break;
        default:
            System.out.println(" " + input);
            break;
        }
    }

    /**
     * Adds a batch using {@code add item invoice quantity unitPrice}. The item
     * name may contain spaces; the final three fields are treated as invoice,
     * quantity, and unit price respectively.
     *
     * @param arguments command arguments
     */
    private static void addBatch(String arguments) {
        Matcher matcher = ADD_ARGUMENTS.matcher(arguments);
        if (!matcher.matches()) {
            System.out.println(" usage: add <item> <invoice> <quantity> <unit price>");
            return;
        }

        String itemName = matcher.group(1).trim();
        String invoiceNumber = matcher.group(2);
        Integer quantity = parsePositiveQuantity(matcher.group(3));
        BigDecimal unitPrice = parseNonNegativePrice(matcher.group(4));
        if (quantity == null || unitPrice == null) {
            return;
        }

        String itemKey = normalize(itemName);
        String invoiceKey = normalize(invoiceNumber);
        ItemInventory item = inventory.get(itemKey);
        if (item == null) {
            if (inventory.size() >= MAX_ITEMS) {
                System.out.println(" cannot track more than " + MAX_ITEMS + " items");
                return;
            }
            item = new ItemInventory(itemName);
            inventory.put(itemKey, item);
        }

        if (item.hasBatch(invoiceKey)) {
            System.out.println(" invoice already exists: " + invoiceNumber);
            return;
        }

        Batch batch = new Batch(invoiceNumber, quantity, unitPrice);
        item.addBatch(invoiceKey, batch);
        printTotals(" added: " + itemName, item);
    }

    /**
     * Removes an entire batch using {@code remove item invoice}. The item name
     * may contain spaces; the final field is treated as the invoice number.
     *
     * @param arguments command arguments
     */
    private static void removeBatch(String arguments) {
        Matcher matcher = REMOVE_ARGUMENTS.matcher(arguments);
        if (!matcher.matches()) {
            System.out.println(" usage: remove <item> <invoice>");
            return;
        }

        String itemName = matcher.group(1).trim();
        String invoiceNumber = matcher.group(2);
        ItemInventory item = inventory.get(normalize(itemName));
        if (item == null || !item.hasBatch(normalize(invoiceNumber))) {
            System.out.println(" batch not found: " + itemName + " / " + invoiceNumber);
            return;
        }

        Batch removedBatch = item.removeBatch(normalize(invoiceNumber));
        printTotals(" removed: " + removedBatch.getInvoiceNumber(), item);
        if (item.isEmpty()) {
            inventory.remove(normalize(itemName));
        }
    }

    /** Lists all items, their totals, and their batches. */
    private static void listItems(String arguments) {
        if (!arguments.isEmpty()) {
            System.out.println(" usage: list");
            return;
        }
        if (inventory.isEmpty()) {
            System.out.println(" No items in list");
            return;
        }

        int itemNumber = 1;
        for (ItemInventory item : inventory.values()) {
            System.out.println(" " + itemNumber + ". " + item.getDisplayName());
            System.out.println("    total quantity: " + item.getTotalQuantity());
            System.out.println("    inventory cost: " + formatPrice(item.getTotalCost()));
            for (Batch batch : item.getBatches().values()) {
                System.out.println("    invoice " + batch.getInvoiceNumber()
                        + ": quantity " + batch.getQuantity()
                        + ", unit price " + formatPrice(batch.getUnitPrice()));
            }
            itemNumber++;
        }
    }

    /** Prints an acknowledgement followed by the item's new totals. */
    private static void printTotals(String acknowledgement, ItemInventory item) {
        System.out.println(acknowledgement);
        System.out.println(" total quantity: " + item.getTotalQuantity());
        System.out.println(" inventory cost: " + formatPrice(item.getTotalCost()));
    }

    /** Parses a strictly positive integer quantity. */
    private static Integer parsePositiveQuantity(String text) {
        try {
            int quantity = Integer.parseInt(text);
            if (quantity > 0) {
                return quantity;
            }
        } catch (NumberFormatException ignored) {
            // The common error response is printed below.
        }
        System.out.println(" quantity must be a positive whole number");
        return null;
    }

    /** Parses a non-negative decimal unit price. */
    private static BigDecimal parseNonNegativePrice(String text) {
        try {
            BigDecimal price = new BigDecimal(text);
            if (price.signum() >= 0) {
                return price;
            }
        } catch (NumberFormatException ignored) {
            // The common error response is printed below.
        }
        System.out.println(" unit price must be a non-negative number");
        return null;
    }

    /** Normalizes command data for case-insensitive lookup. */
    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /** Formats monetary values to two decimal places. */
    private static String formatPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Prints a divider to separate the chatbot's messages in the console. */
    private static void printDivider() {
        System.out.println(DIVIDER);
    }

    /** An immutable inventory batch suitable for future export. */
    private static final class Batch {
        private final String invoiceNumber;
        private final int quantity;
        private final BigDecimal unitPrice;

        private Batch(String invoiceNumber, int quantity, BigDecimal unitPrice) {
            this.invoiceNumber = invoiceNumber;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        private String getInvoiceNumber() {
            return invoiceNumber;
        }

        private int getQuantity() {
            return quantity;
        }

        private BigDecimal getUnitPrice() {
            return unitPrice;
        }

        private BigDecimal getTotalCost() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /** Mutable aggregate for one item and its invoice-keyed batches. */
    private static final class ItemInventory {
        private final String displayName;
        private final HashMap<String, Batch> batches = new HashMap<>();
        private int totalQuantity;
        private BigDecimal totalCost = BigDecimal.ZERO;

        private ItemInventory(String displayName) {
            this.displayName = displayName;
        }

        private boolean hasBatch(String invoiceKey) {
            return batches.containsKey(invoiceKey);
        }

        private void addBatch(String invoiceKey, Batch batch) {
            batches.put(invoiceKey, batch);
            totalQuantity += batch.getQuantity();
            totalCost = totalCost.add(batch.getTotalCost());
        }

        private Batch removeBatch(String invoiceKey) {
            Batch batch = batches.remove(invoiceKey);
            totalQuantity -= batch.getQuantity();
            totalCost = totalCost.subtract(batch.getTotalCost());
            return batch;
        }

        private boolean isEmpty() {
            return batches.isEmpty();
        }

        private String getDisplayName() {
            return displayName;
        }

        private Map<String, Batch> getBatches() {
            return batches;
        }

        private int getTotalQuantity() {
            return totalQuantity;
        }

        private BigDecimal getTotalCost() {
            return totalCost;
        }
    }
}
