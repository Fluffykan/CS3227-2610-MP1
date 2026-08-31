package stockie.ui.javafx.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BatchRowTest {
    @Test
    void totalCostMultipliesUnitPriceByQuantity() {
        BatchRow row = new BatchRow("INV-1", 4, new BigDecimal("2.50"),
                LocalDate.of(2026, 2, 28), "UPC-1");

        assertEquals(new BigDecimal("10.00"), row.totalCost());
    }
}
