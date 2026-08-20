package stockie.application.result;

import java.util.List;
import stockie.model.InventoryItem;
import stockie.model.PerishableBatch;

/** Pairs an item with its expiry-window batches, ordered by ascending expiry date. */
public record ExpiringItem(InventoryItem item, List<PerishableBatch> batches) { }
