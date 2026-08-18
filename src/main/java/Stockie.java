import java.util.Scanner;

/**
 * Starts the Stockie chatbot application.
 */
public class Stockie {
    /** Separates the chatbot's messages in the console. */
    private static final String DIVIDER = "____________________________________________________________";

    /**
     * Greets the user, echoes their input, and exits when they enter {@code bye}.
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
            if (input.equalsIgnoreCase("bye")) {
                System.out.println("Bye. Hope to see you again soon!");
                printDivider();
                break;
            }

            printDivider();
            System.out.println(" " + input);
            printDivider();
        }
        scanner.close();
    }

    /**
     * Prints a divider to separate the chatbot's messages in the console.
     */
    private static void printDivider() {
        System.out.println(DIVIDER);
    }
}
