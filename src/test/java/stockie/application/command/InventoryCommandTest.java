package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import stockie.application.service.InventoryService;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;

class InventoryCommandTest {
    private static final BigDecimal UNIT_PRICE = BigDecimal.TEN;

    @Test
    void addBatchCommandExecuteAndUndoRestoresPreviousState() {
        InventoryService inventory = new InventoryService();
        AddBatchCommand command = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, batch("INV-1", 3));

        command.execute();
        assertEquals(3, inventory.get("milk").getTotalQuantity());
        command.undo();

        assertNull(inventory.get("milk"));
        assertEquals("added", command.getRedoAction());
    }

    @Test
    void addBatchCommandExecuteTwiceAddsBatchOnlyOnce() {
        InventoryService inventory = new InventoryService();
        AddBatchCommand command = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, batch("INV-1", 3));

        command.execute();
        command.execute();

        assertEquals(3, inventory.get("milk").getTotalQuantity());
    }

    @Test
    void sellItemCommandExecuteUndoAndRedoRepeatsSaleCorrectly() {
        InventoryService inventory = inventoryWithItem();
        SellItemCommand command = new SellItemCommand(inventory, "milk", 2);

        command.execute();
        assertEquals(1, inventory.get("milk").getTotalQuantity());
        assertEquals(2, command.getSoldBatches().get(0).quantity());
        command.undo();
        assertEquals(3, inventory.get("milk").getTotalQuantity());
        command.execute();
        assertEquals(1, inventory.get("milk").getTotalQuantity());
    }

    @Test
    void recallBatchCommandExecuteAndUndoRestoresBatch() {
        InventoryService inventory = inventoryWithItem();
        RecallBatchCommand command = new RecallBatchCommand(inventory, "milk", "inv-1");

        command.execute();
        assertEquals(0, inventory.get("milk").getTotalQuantity());
        command.undo();

        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals("INV-1", command.getAffectedBatch().getInvoiceNumber());
    }

    @Test
    void removeItemCommandExecuteAndUndoRestoresCompleteItem() {
        InventoryService inventory = inventoryWithItem();
        RemoveItemCommand command = new RemoveItemCommand(inventory, "milk");

        command.execute();
        assertNull(inventory.get("milk"));
        command.undo();

        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals("MILK", command.getAffectedItem().getSku());
    }

    @Test
    void updateSkuCommandExecuteAndUndoRestoresSku() {
        InventoryService inventory = inventoryWithItem();
        UpdateSkuCommand command = new UpdateSkuCommand(inventory, "milk", "NEW-MILK");

        command.execute();
        assertEquals("NEW-MILK", inventory.get("milk").getSku());
        assertEquals("MILK", command.getOldSku());
        command.undo();

        assertEquals("MILK", inventory.get("milk").getSku());
    }

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 3));
        return inventory;
    }

    private static NonPerishableBatch batch(String invoice, int quantity) {
        return new NonPerishableBatch(invoice, quantity, UNIT_PRICE, "123");
    }
}
