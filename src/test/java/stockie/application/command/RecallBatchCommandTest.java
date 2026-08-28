package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import stockie.application.service.InventoryService;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;

class RecallBatchCommandTest {
    @Test
    void executeAndUndo_restoresBatch() {
        InventoryService inventory = inventoryWithItem();
        RecallBatchCommand command = new RecallBatchCommand(inventory, "milk", "inv-1");

        command.execute();
        assertEquals(0, inventory.get("milk").getTotalQuantity());
        command.undo();

        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals("INV-1", command.getAffectedBatch().getInvoiceNumber());
    }

    @Test
    void executeTwice_recallsBatchOnlyOnce() {
        InventoryService inventory = inventoryWithItem();
        RecallBatchCommand command = new RecallBatchCommand(inventory, "milk", "inv-1");

        command.execute();
        command.execute();

        assertEquals(0, inventory.get("milk").getTotalQuantity());
        assertEquals("Milk", command.getItemName());
        assertEquals("MILK", command.getSku());
    }

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        return inventory;
    }
}
