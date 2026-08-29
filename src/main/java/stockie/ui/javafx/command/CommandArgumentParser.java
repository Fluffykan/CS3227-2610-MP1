package stockie.ui.javafx.command;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stores validated, named arguments for a single CLI command. */
public final class CommandArgumentParser {
    private final Map<String, String> values;

    private CommandArgumentParser(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    /** Parses arguments and returns {@code null} when the syntax is invalid. */
    public static CommandArgumentParser parse(String arguments, List<String> supportedOptions) {
        String[] tokens = arguments.trim().isEmpty() ? new String[0] : arguments.trim().split("\\s+");
        Map<String, String> values = new HashMap<>();
        String key = null;
        StringBuilder value = new StringBuilder();
        for (String token : tokens) {
            if (token.startsWith("--")) {
                if (key != null && !store(values, key, value.toString())) {
                    return null;
                }
                key = token.substring(2).toLowerCase(Locale.ROOT);
                if (!supportedOptions.contains(key)) {
                    return null;
                }
                value.setLength(0);
            } else if (key == null) {
                return null;
            } else {
                if (value.length() > 0) {
                    value.append(' ');
                }
                value.append(token);
            }
        }
        if (key != null && !store(values, key, value.toString())) {
            return null;
        }
        return new CommandArgumentParser(values);
    }

    /** Returns whether this command contains the named option. */
    public boolean has(String option) {
        return values.containsKey(option);
    }

    /** Returns the value associated with the named option. */
    public String get(String option) {
        return values.get(option);
    }

    /** Returns the number of options supplied to this command. */
    public int size() {
        return values.size();
    }

    private static boolean store(Map<String, String> values, String key, String value) {
        return !value.trim().isEmpty() && values.putIfAbsent(key, value.trim()) == null;
    }
}
