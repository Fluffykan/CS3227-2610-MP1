package stockie.application.result;

import stockie.entities.InventoryItem;

/** Reports an item's snapshot after a successful removal or an explanatory failure message. */
public record RemoveItemResult(InventoryItem item, String message) { }
