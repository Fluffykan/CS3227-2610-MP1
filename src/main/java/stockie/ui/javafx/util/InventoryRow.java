package stockie.ui.javafx.util;

import java.math.BigDecimal;
import java.util.List;

/** Presentation data for one row in the inventory table. */
public record InventoryRow(String itemName, String sku, String category, int totalQuantity,
                           BigDecimal inventoryCost, List<BatchRow> batches) {
}
