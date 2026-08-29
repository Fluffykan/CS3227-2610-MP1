package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import stockie.application.service.InventoryService;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;

class UpdateSkuCommandTest {
    @Test
    void executeAndUndo_restoresSku() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        UpdateSkuCommand command = new UpdateSkuCommand(inventory, "milk", "NEW-MILK");

        command.execute();
        assertEquals("NEW-MILK", inventory.get("milk").getSku());
        assertEquals("MILK", command.getOldSku());
        command.undo();

        assertEquals("MILK", inventory.get("milk").getSku());
    }

    @Test
    void executeTwice_updatesSkuOnlyOnce() {
        InventoryService inventory = inventoryWithItem();
        UpdateSkuCommand command = new UpdateSkuCommand(inventory, "milk", "NEW-MILK");

        command.execute();
        command.execute();

        assertEquals("NEW-MILK", inventory.get("milk").getSku());
        assertEquals("MILK", command.getOldSku());
        assertEquals("NEW-MILK", command.getNewSku());
    }

    @Test
    void gettersBeforeExecutionReadCurrentMetadata() {
        InventoryService inventory = inventoryWithItem();
        UpdateSkuCommand command = new UpdateSkuCommand(inventory, "milk", "NEW-MILK");

        assertEquals("milk", command.getItemKey());
        assertEquals("Milk", command.getItemName());
        assertEquals("MILK", command.getSku());
        assertEquals(ItemCategory.NON_PERISHABLE, command.getCategory());
        assertNull(command.getOldSku());
        assertEquals("NEW-MILK", command.getNewSku());
        assertEquals("restored sku", command.getUndoAction());
        assertEquals("updated sku", command.getRedoAction());
    }

    @Test
    void failedExecutionDoesNotPartiallyUpdateSku() {
        InventoryService inventory = inventoryWithItem();
        inventory.addBatch("bread", "Bread", "BREAD", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-2", 1, BigDecimal.ONE, "456"));
        UpdateSkuCommand command = new UpdateSkuCommand(inventory, "milk", "bread");

        assertThrows(IllegalArgumentException.class, command::execute);

        assertEquals("MILK", inventory.get("milk").getSku());
        assertEquals("MILK", inventory.getBySku("MILK").getSku());
        assertEquals("BREAD", inventory.getBySku("bread").getSku());
    }

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        return inventory;
    }
}
