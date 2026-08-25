package stockie.application.result;

import stockie.entities.InventoryItem;

import java.util.List;

/** Result returned by a list query before the UI renders it. */
public record ListQueryResult(List<InventoryItem> items, String message) { }
