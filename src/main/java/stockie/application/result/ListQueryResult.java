package stockie.application.result;

import java.util.List;

import stockie.entities.InventoryItem;

/** Result returned by a list query before the UI renders it. */
public record ListQueryResult(List<InventoryItem> items, String message) { }
