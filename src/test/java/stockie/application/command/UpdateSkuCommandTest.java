package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        return inventory;
    }
}
