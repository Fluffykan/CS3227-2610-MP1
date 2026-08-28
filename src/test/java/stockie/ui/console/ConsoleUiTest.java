package stockie.ui.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ConsoleUiTest {
    @Test
    void parseNamedArguments_acceptsOrderIndependentMultiWordValues() {
        Map<String, String> values = ConsoleUi.parseNamedArguments(
                "--sku MILK --item Whole Milk --quantity 2",
                List.of(List.of("item"), List.of("sku"), List.of("quantity")),
                List.of("item", "sku", "quantity"));

        assertEquals("Whole Milk", values.get("item"));
        assertEquals("MILK", values.get("sku"));
        assertEquals("2", values.get("quantity"));
    }

    @Test
    void parseNamedArguments_rejectsMissingDuplicateAndUnknownFields() {
        assertNull(ConsoleUi.parseNamedArguments("--item Milk", List.of(List.of("sku")),
                List.of("item", "sku")));
        assertNull(ConsoleUi.parseNamedArguments("--item Milk --item Bread", List.of(),
                List.of("item")));
        assertNull(ConsoleUi.parseNamedArguments("--unknown value", List.of(), List.of("item")));
    }
}
