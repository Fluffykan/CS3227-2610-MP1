package stockie.application.result;

import java.util.List;

import stockie.entities.InventoryItem;
import stockie.entities.PerishableBatch;

/** Pairs an item with its expiry-window batches, ordered by ascending expiry date. */
public record ExpiringItem(InventoryItem item, List<PerishableBatch> batches) { }
