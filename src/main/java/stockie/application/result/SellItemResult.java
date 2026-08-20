package stockie.application.result;

import java.util.List;
import stockie.model.InventoryItem;

/** Reports the batches used by a sale and the item's updated totals. */
public record SellItemResult(InventoryItem item, List<SoldBatch> soldBatches, String message) { }
