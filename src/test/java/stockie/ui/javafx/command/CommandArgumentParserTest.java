package stockie.ui.javafx.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CommandArgumentParserTest {
    private static final List<String> OPTIONS = List.of("item", "sku", "quantity");

    @Test
    void parsesCaseInsensitiveOptionsAndPreservesMultiWordValues() {
        CommandArgumentParser parser = CommandArgumentParser.parse("--ITEM Whole Milk --SKU milk", OPTIONS);

        assertEquals("Whole Milk", parser.get("item"));
        assertEquals("milk", parser.get("sku"));
        assertEquals(2, parser.size());
        assertTrue(parser.has("item"));
    }

    @Test
    void rejectsMissingDuplicateAndUnknownArguments() {
        assertNull(CommandArgumentParser.parse("--item", OPTIONS));
        assertNull(CommandArgumentParser.parse("--item Milk --item Bread", OPTIONS));
        assertNull(CommandArgumentParser.parse("--unknown value", OPTIONS));
    }

    @Test
    void rejectsValuesWithoutOptionsAndEmptyInputIsValid() {
        assertNull(CommandArgumentParser.parse("Milk", OPTIONS));
        CommandArgumentParser parser = CommandArgumentParser.parse("   ", OPTIONS);

        assertEquals(0, parser.size());
    }
}
