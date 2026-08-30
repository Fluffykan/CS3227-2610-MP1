package stockie.ui.javafx.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;

class CommandResponseFormatterTest {
    @Test
    void formatsSuccessfulMutationResponses() {
        assertEquals("Added batch for Milk\n", CommandResponseFormatter.addedBatch("Milk"));
        assertEquals("Sold 2 of Milk\n", CommandResponseFormatter.soldItems(2, "Milk"));
        assertEquals("Batch recalled.", CommandResponseFormatter.batchRecalled());
        assertEquals("Item removed.", CommandResponseFormatter.itemRemoved());
        assertEquals("SKU updated.", CommandResponseFormatter.skuUpdated());
    }

    @Test
    void formatsInventoryAndEmptyQueryResponses() {
        InventoryItem item = new InventoryItem("Milk", "MILK", ItemCategory.NON_PERISHABLE);

        assertEquals("Milk | SKU MILK | Qty 0\n", CommandResponseFormatter.inventoryItem(item));
        assertEquals("No matching items.\n", CommandResponseFormatter.noMatchingItems());
        assertEquals("No matching batches.\n", CommandResponseFormatter.noMatchingBatches());
    }
}
