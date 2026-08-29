package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import stockie.application.service.InventoryService;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;

class AddBatchCommandTest {
    @Test
    void executeAndUndo_restoresPreviousState() {
        InventoryService inventory = new InventoryService();
        AddBatchCommand command = command(inventory);

        command.execute();
        command.undo();

        assertNull(inventory.get("milk"));
        assertEquals("added", command.getRedoAction());
    }

    @Test
    void executeTwice_addsBatchOnlyOnce() {
        InventoryService inventory = new InventoryService();
        AddBatchCommand command = command(inventory);

        command.execute();
        command.execute();

        assertEquals(3, inventory.get("milk").getTotalQuantity());
    }

    @Test
    void gettersAfterExecutionReturnCommandMetadata() {
        AddBatchCommand command = command(new InventoryService());

        command.execute();

        assertEquals("milk", command.getItemKey());
        assertEquals("Milk", command.getItemName());
        assertEquals("MILK", command.getSku());
        assertEquals(ItemCategory.NON_PERISHABLE, command.getCategory());
        assertEquals("INV-1", command.getAffectedBatch().getInvoiceNumber());
        assertEquals("removed", command.getUndoAction());
        assertEquals("added", command.getRedoAction());
    }

    @Test
    void gettersBeforeExecutionReturnCommandMetadata() {
        AddBatchCommand command = command(new InventoryService());

        assertEquals("milk", command.getItemKey());
        assertEquals("Milk", command.getItemName());
        assertEquals("MILK", command.getSku());
        assertEquals(ItemCategory.NON_PERISHABLE, command.getCategory());
        assertEquals("INV-1", command.getAffectedBatch().getInvoiceNumber());
        assertEquals("removed", command.getUndoAction());
        assertEquals("added", command.getRedoAction());
    }

    @Test
    void failedExecutionDoesNotPartiallyAddBatch() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        AddBatchCommand command = command(inventory);

        assertThrows(IllegalArgumentException.class, command::execute);

        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals(1, inventory.get("milk").getBatches().size());
    }

    @Test
    void undoBeforeExecutionLeavesInventoryUnchanged() {
        InventoryService inventory = new InventoryService();
        AddBatchCommand command = command(inventory);

        command.undo();

        assertNull(inventory.get("milk"));
    }

    private static AddBatchCommand command(InventoryService inventory) {
        return new AddBatchCommand(inventory, "Milk", "milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
    }
}
