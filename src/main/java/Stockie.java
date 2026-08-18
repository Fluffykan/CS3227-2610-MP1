import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

/**
 * Starts the Stockie chatbot application.
 */
public class Stockie {
    /** Maximum number of distinct items that can be tracked. */
    private static final int MAX_ITEMS = 100;

    /** Separates the chatbot's messages in the console. */
    private static final String DIVIDER = "____________________________________________________________";

    /** Maps normalized item names to the original text used when displaying them. */
    private static final HashMap<String, String> items = new HashMap<>();

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
            String input = scanner.nextLine();
            String trimmedInput = input.trim();
            if (trimmedInput.equalsIgnoreCase("bye")) {
                System.out.println("Bye. Hope to see you again soon!");
                printDivider();
                break;
            }

            printDivider();
            processCommand(trimmedInput);
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

        String[] commandParts = input.split("\\s+", 2);
        String command = commandParts[0].toLowerCase(Locale.ROOT);
        String itemName = commandParts.length > 1 ? commandParts[1].trim() : "";

        switch (command) {
        case "add":
            addItem(itemName);
            break;
        case "remove":
            removeItem(itemName);
            break;
        case "list":
            listItems();
            break;
        default:
            System.out.println(" " + input);
            break;
        }
    }

    /**
     * Adds an item if it is non-empty, unique, and within the item limit.
     *
     * @param itemName item text supplied by the user
     */
    private static void addItem(String itemName) {
        if (itemName.isEmpty()) {
            System.out.println(" item name cannot be empty");
            return;
        }

        String normalizedName = normalizeItemName(itemName);
        if (items.containsKey(normalizedName)) {
            System.out.println(" item already exists: " + items.get(normalizedName));
        } else if (items.size() >= MAX_ITEMS) {
            System.out.println(" cannot track more than " + MAX_ITEMS + " items");
        } else {
            items.put(normalizedName, itemName);
            System.out.println(" added: " + itemName);
        }
    }

    /**
     * Removes an item by its name.
     *
     * @param itemName item text supplied by the user
     */
    private static void removeItem(String itemName) {
        if (itemName.isEmpty()) {
            System.out.println(" item name cannot be empty");
            return;
        }

        String removedItem = items.remove(normalizeItemName(itemName));
        if (removedItem == null) {
            System.out.println(" item not found: " + itemName);
        } else {
            System.out.println(" removed: " + removedItem);
        }
    }

    /** Prints all tracked items with one-based numbering. */
    private static void listItems() {
        if (items.isEmpty()) {
            System.out.println(" No items in list");
            return;
        }

        int itemNumber = 1;
        for (String item : items.values()) {
            System.out.println(" " + itemNumber + ". " + item);
            itemNumber++;
        }
    }

    /**
     * Normalizes an item name for case-insensitive lookup.
     *
     * @param itemName item text to normalize
     * @return normalized item name
     */
    private static String normalizeItemName(String itemName) {
        return itemName.toLowerCase(Locale.ROOT);
    }

    /**
     * Prints a divider to separate the chatbot's messages in the console.
     */
    private static void printDivider() {
        System.out.println(DIVIDER);
    }
}
