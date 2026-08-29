package stockie.ui.javafx.command;

import stockie.entities.InventoryItem;

/** Formats user-facing responses produced by JavaFX command handlers. */
public final class CommandResponseFormatter {
    private CommandResponseFormatter() {
    }

    /** Formats a successful add response. */
    public static String addedBatch(String itemName) {
        return "Added batch for " + itemName + "\n";
    }

    /** Formats a successful sell response. */
    public static String soldItems(int quantity, String itemName) {
        return "Sold " + quantity + " of " + itemName + "\n";
    }

    /** Formats an inventory item for list and find responses. */
    public static String inventoryItem(InventoryItem item) {
        return item.getDisplayName() + " | SKU " + item.getSku()
                + " | Qty " + item.getTotalQuantity() + "\n";
    }

    /** Returns the response used when no inventory items match a query. */
    public static String noMatchingItems() {
        return "No matching items.\n";
    }

    /** Returns the response used when no batches match a query. */
    public static String noMatchingBatches() {
        return "No matching batches.\n";
    }

    /** Returns the response used after recalling a batch. */
    public static String batchRecalled() {
        return "Batch recalled.";
    }

    /** Returns the response used after removing an item. */
    public static String itemRemoved() {
        return "Item removed.";
    }

    /** Returns the response used after updating an item's SKU. */
    public static String skuUpdated() {
        return "SKU updated.";
    }
}
