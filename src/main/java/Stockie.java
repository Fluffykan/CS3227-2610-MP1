import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;

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
    private static final InventoryService inventory = new InventoryService();
    /** Stores the inventory between application sessions. */
    private static final InventoryRepository repository = new FileInventoryRepository(Path.of(
            System.getProperty("stockie.data.file", "stockie-inventory.dat")));
    /** Coordinates command execution and the undo/redo history. */
    private static final CommandManager commandManager = new CommandManager(inventory, repository);
    /** Item categories determine which immutable batch subtype an inventory accepts. */
    private enum ItemCategory { PERISHABLE, NON_PERISHABLE }
    /** Required named fields accepted by the add command. */
    private static final List<String> ADD_REQUIRED_FIELDS =
            List.of("item", "sku", "invoice", "quantity", "price");
    /** All named fields accepted by the add command, including optional fields. */
    private static final List<String> ADD_SUPPORTED_FIELDS =
            List.of("item", "sku", "invoice", "quantity", "price", "expiry", "upc");
    /** Required and supported named fields accepted by the remove command. */
    private static final List<String> REMOVE_FIELDS = List.of("item", "invoice");


    /** Greets the user, processes commands, and exits on {@code bye}. */
    public static void main(String[] args) {
        try {
            inventory.load(repository);
        } catch (IOException | ClassNotFoundException exception) {
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
    private static void processCommand(String input, Scanner scanner) {
        if (input.isEmpty()) return;
        int separator = input.indexOf(' ');
        String command = (separator < 0 ? input : input.substring(0, separator)).toLowerCase(Locale.ROOT);
        String arguments = separator < 0 ? "" : input.substring(separator + 1).trim();
        switch (command) {
        case "add": addBatch(arguments, scanner); break;
        case "remove": removeBatch(arguments); break;
        case "list": listItems(arguments); break;
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
        System.out.println(" remove --item <name> --invoice <invoice>");
        System.out.println(" list");
        System.out.println(" undo");
        System.out.println(" redo");
        System.out.println(" help");
        System.out.println(" bye");
    }

    /** Adds a batch using order-independent {@code --field value} arguments. */
    private static void addBatch(String arguments, Scanner scanner) {
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

        ItemCategory category = hasExpiry ? ItemCategory.PERISHABLE : ItemCategory.NON_PERISHABLE;
        String itemKey = normalize(itemName);
        String invoiceKey = normalize(invoiceNumber);
        InventoryItem item = inventory.get(itemKey);
        if (item == null) {
            if (inventory.size() >= MAX_ITEMS) {
                System.out.println(" cannot track more than " + MAX_ITEMS + " items");
                return;
            }
        } else if (item.getCategory() != category) {
            System.out.println(" item category does not match existing item: " + itemName);
            return;
        } else if (!item.getSku().equals(sku)) {
            System.out.println(" sku does not match existing item: " + itemName);
            return;
        }

        if (item != null && item.hasBatch(invoiceKey)) {
            System.out.println(" invoice already exists: " + invoiceNumber);
            return;
        }
        Batch batch = category == ItemCategory.PERISHABLE
                ? new PerishableBatch(invoiceNumber, quantity, unitPrice, expiryDate, upc)
                : new NonPerishableBatch(invoiceNumber, quantity, unitPrice, upc);
        try {
            commandManager.execute(new AddBatchCommand(itemName, itemKey, sku, category, batch));
            printTotals(" added: " + itemName, inventory.get(itemKey));
        } catch (IOException exception) {
            System.out.println(" unable to save inventory; addition cancelled");
        }
    }

    /** Parses named arguments using command-specific required and supported fields. */
    private static Map<String, String> parseNamedArguments(String arguments,
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

    /** Removes an entire batch using {@code --item} and {@code --invoice} arguments. */
    private static void removeBatch(String arguments) {
        Map<String, String> fields = parseNamedArguments(arguments, REMOVE_FIELDS, REMOVE_FIELDS);
        if (fields == null) return;
        String itemName = fields.get("item");
        String invoiceNumber = fields.get("invoice");
        String itemKey = normalize(itemName);
        String invoiceKey = normalize(invoiceNumber);
        InventoryItem item = inventory.get(itemKey);
        if (item == null || !item.hasBatch(invoiceKey)) {
            System.out.println(" batch not found: " + itemName + " / " + invoiceNumber);
            return;
        }
        try {
            commandManager.execute(new RemoveBatchCommand(itemName, itemKey, invoiceKey));
            InventoryItem updatedItem = inventory.get(itemKey);
            System.out.println(" removed: " + invoiceNumber);
            if (updatedItem == null) {
                System.out.println(" total quantity: 0");
                System.out.println(" inventory cost: 0.00");
            } else {
                printTotalsOnly(updatedItem);
            }
        } catch (IOException exception) {
            System.out.println(" unable to save inventory; removal cancelled");
        }
    }

    /** Undoes the most recent successful change. */
    private static void undo() {
        try {
            InventoryCommand command = commandManager.undo();
            printCommandDetails(command.getUndoAction(), command);
        } catch (IllegalStateException exception) {
            System.out.println(" nothing to undo");
        } catch (IOException exception) {
            System.out.println(" unable to save inventory; undo cancelled");
        }
    }

    /** Redoes the most recently undone change. */
    private static void redo() {
        try {
            InventoryCommand command = commandManager.redo();
            printCommandDetails(command.getRedoAction(), command);
        } catch (IllegalStateException exception) {
            System.out.println(" nothing to redo");
        } catch (IOException exception) {
            System.out.println(" unable to save inventory; redo cancelled");
        }
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
        printTotalsOnly(item);
    }

    /** Prints only the aggregate totals for an item. */
    private static void printTotalsOnly(InventoryItem item) {
        System.out.println(" total quantity: " + item.getTotalQuantity());
        System.out.println(" inventory cost: " + formatPrice(item.getTotalCost()));
    }

    /** Prints the affected batch followed by aggregate totals after undo or redo. */
    private static void printCommandDetails(String action, InventoryCommand command) {
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
    private static void printCommandTotals(InventoryCommand command) {
        InventoryItem item = inventory.get(command.getItemKey());
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

    /** Abstraction for loading and saving complete inventory snapshots. */
    private interface InventoryRepository {
        HashMap<String, InventoryItem> load() throws IOException, ClassNotFoundException;
        void save(HashMap<String, InventoryItem> snapshot) throws IOException;
    }

    /** Persists snapshots using an atomic temporary-file replacement. */
    private static final class FileInventoryRepository implements InventoryRepository {
        private final Path path;

        private FileInventoryRepository(Path path) {
            this.path = path;
        }

        @Override
        public HashMap<String, InventoryItem> load() throws IOException, ClassNotFoundException {
            if (!Files.exists(path) || Files.size(path) == 0) {
                return new HashMap<>();
            }
            try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
                @SuppressWarnings("unchecked")
                HashMap<String, InventoryItem> snapshot = (HashMap<String, InventoryItem>) input.readObject();
                return snapshot;
            }
        }

        @Override
        public void save(HashMap<String, InventoryItem> snapshot) throws IOException {
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(snapshot);
                output.flush();
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        }
    }

    /** Owns inventory mutations and creates defensive copies for command history. */
    private static final class InventoryService {
        private HashMap<String, InventoryItem> items = new HashMap<>();

        private void load(InventoryRepository repository) throws IOException, ClassNotFoundException {
            items = deepCopy(repository.load());
        }

        private InventoryItem get(String itemKey) { return items.get(itemKey); }
        private boolean isEmpty() { return items.isEmpty(); }
        private java.util.Collection<InventoryItem> values() { return items.values(); }
        private int size() { return items.size(); }

        private void addBatch(String itemKey, String itemName, String sku,
                ItemCategory category, Batch batch) {
            InventoryItem item = items.get(itemKey);
            if (item == null) {
                item = new InventoryItem(itemName, sku, category);
                items.put(itemKey, item);
            }
            item.addBatch(normalize(batch.getInvoiceNumber()), batch.getInvoiceNumber(),
                    batch.getQuantity(), batch.getUnitPrice(),
                    batch instanceof PerishableBatch ? ((PerishableBatch) batch).getExpiryDate() : null,
                    batch.getUpc());
        }

        private void removeBatch(String itemKey, String invoiceKey) {
            InventoryItem item = items.get(itemKey);
            item.removeBatch(invoiceKey);
            if (item.isEmpty()) {
                items.remove(itemKey);
            }
        }

        private InventoryItem copyItem(String itemKey) {
            InventoryItem item = items.get(itemKey);
            return item == null ? null : item.deepCopy();
        }

        private void restoreItem(String itemKey, InventoryItem item) {
            if (item == null) {
                items.remove(itemKey);
            } else {
                items.put(itemKey, item.deepCopy());
            }
        }

        private HashMap<String, InventoryItem> snapshot() { return deepCopy(items); }

        private static HashMap<String, InventoryItem> deepCopy(Map<String, InventoryItem> source) {
            HashMap<String, InventoryItem> copy = new HashMap<>();
            for (Map.Entry<String, InventoryItem> entry : source.entrySet()) {
                copy.put(entry.getKey(), entry.getValue().deepCopy());
            }
            return copy;
        }
    }

    /** Executes commands and coordinates persistence with undo and redo stacks. */
    private static final class CommandManager {
        private final InventoryService inventory;
        private final InventoryRepository repository;
        private final Deque<InventoryCommand> undoStack = new ArrayDeque<>();
        private final Deque<InventoryCommand> redoStack = new ArrayDeque<>();

        private CommandManager(InventoryService inventory, InventoryRepository repository) {
            this.inventory = inventory;
            this.repository = repository;
        }

        private void execute(InventoryCommand command) throws IOException {
            command.execute();
            try {
                repository.save(inventory.snapshot());
            } catch (IOException exception) {
                command.undo();
                throw exception;
            }
            undoStack.push(command);
            redoStack.clear();
        }

        private InventoryCommand undo() throws IOException {
            if (undoStack.isEmpty()) throw new IllegalStateException();
            InventoryCommand command = undoStack.pop();
            command.undo();
            try {
                repository.save(inventory.snapshot());
            } catch (IOException exception) {
                command.execute();
                undoStack.push(command);
                throw exception;
            }
            redoStack.push(command);
            return command;
        }

        private InventoryCommand redo() throws IOException {
            if (redoStack.isEmpty()) throw new IllegalStateException();
            InventoryCommand command = redoStack.pop();
            command.execute();
            try {
                repository.save(inventory.snapshot());
            } catch (IOException exception) {
                command.undo();
                redoStack.push(command);
                throw exception;
            }
            undoStack.push(command);
            return command;
        }
    }

    /** Represents one reversible inventory mutation. */
    private interface InventoryCommand {
        void execute();
        void undo();
        String getUndoAction();
        String getRedoAction();
        String getItemKey();
        String getItemName();
        String getSku();
        ItemCategory getCategory();
        Batch getAffectedBatch();
    }

    /** Adds a batch and snapshots the previous item for undo. */
    private static final class AddBatchCommand implements InventoryCommand {
        private final String itemName;
        private final String itemKey;
        private final String sku;
        private final ItemCategory category;
        private final Batch batch;
        private InventoryItem previousItem;
        private boolean initialized;
        private boolean executed;

        private AddBatchCommand(String itemName, String itemKey, String sku,
                ItemCategory category, Batch batch) {
            this.itemName = itemName;
            this.itemKey = itemKey;
            this.sku = sku;
            this.category = category;
            this.batch = batch;
        }

        @Override
        public void execute() {
            if (executed) return;
            if (!initialized) {
                previousItem = inventory.copyItem(itemKey);
                initialized = true;
            }
            inventory.addBatch(itemKey, itemName, sku, category, batch);
            executed = true;
        }

        @Override
        public void undo() {
            if (!executed) return;
            inventory.restoreItem(itemKey, previousItem);
            executed = false;
        }

        @Override
        public String getUndoAction() { return "removed"; }

        @Override
        public String getRedoAction() { return "added"; }

        @Override
        public String getItemKey() { return itemKey; }

        @Override
        public String getItemName() { return itemName; }

        @Override
        public String getSku() { return sku; }

        @Override
        public ItemCategory getCategory() { return category; }

        @Override
        public Batch getAffectedBatch() { return batch; }
    }

    /** Removes a batch and snapshots the complete item for undo. */
    private static final class RemoveBatchCommand implements InventoryCommand {
        private final String itemKey;
        private final String invoiceKey;
        private InventoryItem previousItem;
        private String invoiceNumber;
        private boolean initialized;
        private boolean executed;

        private RemoveBatchCommand(String itemName, String itemKey, String invoiceKey) {
            this.itemKey = itemKey;
            this.invoiceKey = invoiceKey;
        }

        @Override
        public void execute() {
            if (executed) return;
            if (!initialized) {
                previousItem = inventory.copyItem(itemKey);
                invoiceNumber = previousItem.getBatches().get(invoiceKey).getInvoiceNumber();
                initialized = true;
            }
            inventory.removeBatch(itemKey, invoiceKey);
            executed = true;
        }

        @Override
        public void undo() {
            if (!executed) return;
            inventory.restoreItem(itemKey, previousItem);
            executed = false;
        }

        @Override
        public String getUndoAction() { return "added"; }

        @Override
        public String getRedoAction() { return "removed"; }

        @Override
        public String getItemKey() { return itemKey; }

        @Override
        public String getItemName() { return previousItem.getDisplayName(); }

        @Override
        public String getSku() { return previousItem.getSku(); }

        @Override
        public ItemCategory getCategory() { return previousItem.getCategory(); }

        @Override
        public Batch getAffectedBatch() { return previousItem.getBatches().get(invoiceKey); }
    }

    /** Common immutable data for all batches. */
    private abstract static class Batch implements Serializable {
        private static final long serialVersionUID = 1L;
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
    private static final class InventoryItem implements Serializable {
        private static final long serialVersionUID = 1L;
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

        /** Creates a defensive copy including every batch and aggregate total. */
        private InventoryItem deepCopy() {
            InventoryItem copy = new InventoryItem(displayName, sku, category);
            for (Map.Entry<String, Batch> entry : batches.entrySet()) {
                Batch batch = entry.getValue();
                Batch batchCopy = batch instanceof PerishableBatch
                        ? new PerishableBatch(batch.getInvoiceNumber(), batch.getQuantity(),
                                batch.getUnitPrice(), ((PerishableBatch) batch).getExpiryDate(), batch.getUpc())
                        : new NonPerishableBatch(batch.getInvoiceNumber(), batch.getQuantity(),
                                batch.getUnitPrice(), batch.getUpc());
                copy.batches.put(entry.getKey(), batchCopy);
            }
            copy.totalQuantity = totalQuantity;
            copy.totalCost = totalCost;
            return copy;
        }
    }
}
