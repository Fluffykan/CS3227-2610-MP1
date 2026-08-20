package stockie.application.result;

import stockie.model.InventoryItem;

/** Result returned by a find query before the UI renders it. */
public record FindQueryResult(InventoryItem item, String message) { }
