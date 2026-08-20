package stockie.application.result;

import stockie.model.InventoryItem;

/** Reports an added item's updated state or an explanatory failure message. */
public record AddBatchResult(InventoryItem item, String message) { }
