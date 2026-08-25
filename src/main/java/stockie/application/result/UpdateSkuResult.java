package stockie.application.result;

import stockie.entities.InventoryItem;

/** Reports an item's updated SKU and its previous SKU, or an explanatory failure message. */
public record UpdateSkuResult(InventoryItem item, String oldSku, String message) { }
