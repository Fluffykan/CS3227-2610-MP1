package stockie.application.result;

import stockie.model.InventoryItem;

/** Reports a recalled batch's item's updated state or an explanatory failure message. */
public record RecallBatchResult(InventoryItem item, String message) { }
