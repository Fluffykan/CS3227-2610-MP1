package stockie.ui.javafx.util;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Presentation data for one batch in the item details table. */
public record BatchRow(String invoice, int quantity, BigDecimal unitPrice,
                       LocalDate expiry, String upc) {
    public BigDecimal totalCost() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
