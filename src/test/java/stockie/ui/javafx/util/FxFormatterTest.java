package stockie.ui.javafx.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FxFormatterTest {
    @Test
    void priceRoundsToTwoDecimalPlaces() {
        assertEquals("12.35", FxFormatter.price(new BigDecimal("12.345")));
        assertEquals("4.00", FxFormatter.price(new BigDecimal("4")));
    }

    @Test
    void dateFormatsUsingDayMonthYearPattern() {
        assertEquals("29-02-2028", FxFormatter.date(LocalDate.of(2028, 2, 29)));
        assertEquals("-", FxFormatter.date(null));
    }

    @Test
    void optionalTextUsesPlaceholderForMissingValue() {
        assertEquals("UPC-1", FxFormatter.optionalText("UPC-1"));
        assertEquals("-", FxFormatter.optionalText(null));
    }
}
