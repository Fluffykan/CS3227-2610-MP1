package stockie.ui.javafx.command;

import java.util.Locale;

/** Parses the command name and argument text entered in the JavaFX command panel. */
public final class CommandParser {
    private CommandParser() {
    }

    /** Returns the command name and arguments extracted from the input. */
    public static ParsedCommand parse(String input) {
        String trimmedInput = input.trim();
        int separator = trimmedInput.indexOf(' ');
        String command = (separator < 0 ? trimmedInput : trimmedInput.substring(0, separator))
                .toLowerCase(Locale.ROOT);
        String arguments = separator < 0 ? "" : trimmedInput.substring(separator + 1).trim();
        return new ParsedCommand(command, arguments);
    }

    /** Stores the command name and its unparsed argument text. */
    public record ParsedCommand(String command, String arguments) {
    }
}
