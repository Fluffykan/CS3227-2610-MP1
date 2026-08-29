package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void gettersBeforeExecutionReadCurrentItemMetadata() {
        InventoryService inventory = inventoryWithItem();
        RecallBatchCommand command = new RecallBatchCommand(inventory, "milk", "inv-1");

        assertEquals("milk", command.getItemKey());
        assertEquals("Milk", command.getItemName());
        assertEquals("MILK", command.getSku());
        assertEquals(ItemCategory.NON_PERISHABLE, command.getCategory());
        assertEquals("INV-1", command.getAffectedBatch().getInvoiceNumber());
        assertEquals("added", command.getUndoAction());
        assertEquals("recalled", command.getRedoAction());
    }

    @Test
    void failedExecutionLeavesBatchUnchanged() {
        InventoryService inventory = inventoryWithItem();
        RecallBatchCommand command = new RecallBatchCommand(inventory, "milk", "missing-invoice");

        assertThrows(IllegalArgumentException.class, command::execute);

        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals(1, inventory.get("milk").getBatches().size());
    }

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        return inventory;
    }
}
