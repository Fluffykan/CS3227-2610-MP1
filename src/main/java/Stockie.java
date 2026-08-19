import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Starts the Stockie chatbot application. */
public class Stockie {
    /** Maximum number of distinct items that can be tracked. */
    private static final int MAX_ITEMS = 100;
    /** Separates chatbot messages in the console. */
    private static final String DIVIDER = "____________________________________________________________";
    /** Formats expiry dates as day-month-year. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    /** Maps normalized item names to their inventory. */
    private static final HashMap<String, InventoryItem> inventory = new HashMap<>();
    /** Parses a remove command whose final token is an invoice number. */
    private static final Pattern REMOVE_ARGUMENTS = Pattern.compile("^(.+)\\s+(\\S+)$");

    /** Item categories determine which immutable batch subtype an inventory accepts. */
    private enum ItemCategory { PERISHABLE, NON_PERISHABLE }
    /** Required fields for adding an item to inventory. */
    private static final String[] REQUIRED_FIELDS = {"item", "sku", "invoice", "quantity", "price"};
    /** Optional fields for adding an item to inventory */
    private static final String[] OPTIONAL_FIELDS = {"expiry", "upc"};

    /** Greets the user, processes commands, and exits on {@code bye}. */
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
            processCommand(input, scanner);
            printDivider();
        }
        scanner.close();
    }

    /** Routes a command using its first whitespace-delimited word. */
    private static void processCommand(String input, Scanner scanner) {
        if (input.isEmpty()) return;
        int separator = input.indexOf(' ');
        String command = (separator < 0 ? input : input.substring(0, separator)).toLowerCase(Locale.ROOT);
        String arguments = separator < 0 ? "" : input.substring(separator + 1).trim();
        switch (command) {
        case "add": addBatch(arguments, scanner); break;
        case "remove": removeBatch(arguments); break;
        case "list": listItems(arguments); break;
        default: System.out.println(" " + input); break;
        }
    }

    /** Adds a batch using order-independent {@code --field value} arguments. */
    private static void addBatch(String arguments, Scanner scanner) {
        Map<String, String> fields = parseNamedArguments(arguments);
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

        ItemCategory category = hasExpiry ? ItemCategory.PERISHABLE : ItemCategory.NON_PERISHABLE;
        String itemKey = normalize(itemName);
        String invoiceKey = normalize(invoiceNumber);
        InventoryItem item = inventory.get(itemKey);
        if (item == null) {
            if (inventory.size() >= MAX_ITEMS) {
                System.out.println(" cannot track more than " + MAX_ITEMS + " items");
                return;
            }
            item = new InventoryItem(itemName, sku, category);
            inventory.put(itemKey, item);
        } else if (item.getCategory() != category) {
            System.out.println(" item category does not match existing item: " + itemName);
            return;
        } else if (!item.getSku().equals(sku)) {
            System.out.println(" sku does not match existing item: " + itemName);
            return;
        }

        if (item.hasBatch(invoiceKey)) {
            System.out.println(" invoice already exists: " + invoiceNumber);
            return;
        }
        item.addBatch(invoiceKey, invoiceNumber, quantity, unitPrice, expiryDate, upc);
        printTotals(" added: " + itemName, item);
    }

    /** Parses named arguments and rejects unknown, duplicate, empty, or missing fields. */
    private static Map<String, String> parseNamedArguments(String arguments) {
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
                if (!isSupportedArgument(currentKey)) {
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
        for (String field : REQUIRED_FIELDS) {
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

    /** Returns whether a named field is supported by the add command. */
    private static boolean isSupportedArgument(String key) {
        return Arrays.stream(REQUIRED_FIELDS).anyMatch(key::equals) 
        || Arrays.stream(OPTIONAL_FIELDS).anyMatch(key::equals);
    }

    /** Removes an entire batch using {@code remove item invoice}. */
    private static void removeBatch(String arguments) {
        Matcher matcher = REMOVE_ARGUMENTS.matcher(arguments);
        if (!matcher.matches()) {
            System.out.println(" usage: remove <item> <invoice>");
            return;
        }
        String itemName = matcher.group(1).trim();
        String invoiceNumber = matcher.group(2);
        String itemKey = normalize(itemName);
        String invoiceKey = normalize(invoiceNumber);
        InventoryItem item = inventory.get(itemKey);
        if (item == null || !item.hasBatch(invoiceKey)) {
            System.out.println(" batch not found: " + itemName + " / " + invoiceNumber);
            return;
        }
        Batch removedBatch = item.removeBatch(invoiceKey);
        printTotals(" removed: " + removedBatch.getInvoiceNumber(), item);
        if (item.isEmpty()) inventory.remove(itemKey);
    }

    /** Lists items, totals, categories, and batch details. */
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
        for (InventoryItem item : inventory.values()) {
            System.out.println(" " + itemNumber + ". " + item.getDisplayName());
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
            itemNumber++;
        }
    }

    /** Prints an acknowledgement followed by updated totals. */
    private static void printTotals(String acknowledgement, InventoryItem item) {
        System.out.println(acknowledgement);
        System.out.println(" total quantity: " + item.getTotalQuantity());
        System.out.println(" inventory cost: " + formatPrice(item.getTotalCost()));
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

    /** Normalizes command data for case-insensitive lookup. */
    private static String normalize(String value) { return value.toLowerCase(Locale.ROOT); }

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

    /** Common immutable data for all batches. */
    private abstract static class Batch {
        private final String invoiceNumber;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final String upc;

        private Batch(String invoiceNumber, int quantity, BigDecimal unitPrice, String upc) {
            this.invoiceNumber = invoiceNumber;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.upc = upc;
        }

        private String getInvoiceNumber() { return invoiceNumber; }
        private int getQuantity() { return quantity; }
        private BigDecimal getUnitPrice() { return unitPrice; }
        private String getUpc() { return upc; }
        private BigDecimal getTotalCost() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }

    /** Immutable batch whose expiry date is mandatory. */
    private static final class PerishableBatch extends Batch {
        private final LocalDate expiryDate;

        private PerishableBatch(String invoiceNumber, int quantity, BigDecimal unitPrice,
                LocalDate expiryDate, String upc) {
            super(invoiceNumber, quantity, unitPrice, upc);
            this.expiryDate = expiryDate;
        }

        private LocalDate getExpiryDate() { return expiryDate; }
    }

    /** Immutable batch that has no expiry date. */
    private static final class NonPerishableBatch extends Batch {
        private NonPerishableBatch(String invoiceNumber, int quantity, BigDecimal unitPrice, String upc) {
            super(invoiceNumber, quantity, unitPrice, upc);
        }
    }

    /** Mutable aggregate for one item and its invoice-keyed batches. */
    private static final class InventoryItem {
        private final String displayName;
        private String sku;
        private final ItemCategory category;
        private final HashMap<String, Batch> batches = new HashMap<>();
        private int totalQuantity;
        private BigDecimal totalCost = BigDecimal.ZERO;

        private InventoryItem(String displayName, String sku, ItemCategory category) {
            this.displayName = displayName;
            this.sku = sku;
            this.category = category;
        }

        private boolean hasBatch(String invoiceKey) { return batches.containsKey(invoiceKey); }

        /** Creates the correct batch subtype and updates aggregate totals. */
        private void addBatch(String invoiceKey, String invoiceNumber, int quantity,
                BigDecimal unitPrice, LocalDate expiryDate, String upc) {
            Batch batch = category == ItemCategory.PERISHABLE
                    ? new PerishableBatch(invoiceNumber, quantity, unitPrice, expiryDate, upc)
                    : new NonPerishableBatch(invoiceNumber, quantity, unitPrice, upc);
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

        private boolean isEmpty() { return batches.isEmpty(); }
        private String getDisplayName() { return displayName; }
        private String getSku() { return sku; }
        private void setSku(String sku) { this.sku = sku; }
        private ItemCategory getCategory() { return category; }
        private Map<String, Batch> getBatches() { return batches; }
        private int getTotalQuantity() { return totalQuantity; }
        private BigDecimal getTotalCost() { return totalCost; }
    }
}
