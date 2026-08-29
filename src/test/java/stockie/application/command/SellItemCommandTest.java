package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import stockie.application.service.InventoryService;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;

class SellItemCommandTest {
    @Test
    void executeUndoAndRedo_repeatsSaleCorrectly() {
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
    void gettersBeforeExecutionReadCurrentItemMetadata() {
        InventoryService inventory = inventoryWithItem();
        SellItemCommand command = new SellItemCommand(inventory, "milk", 2);

        assertEquals("milk", command.getItemKey());
        assertEquals("Milk", command.getItemName());
        assertEquals("MILK", command.getSku());
        assertEquals(ItemCategory.NON_PERISHABLE, command.getCategory());
        assertNull(command.getAffectedBatch());
        assertNull(command.getSoldBatches());
        assertEquals("restored sale for", command.getUndoAction());
        assertEquals("sold", command.getRedoAction());
        assertEquals(3, command.getAffectedItem().getTotalQuantity());
    }

    @Test
    void failedExecutionDoesNotPartiallySellStock() {
        InventoryService inventory = inventoryWithItem();
        SellItemCommand command = new SellItemCommand(inventory, "milk", 4);

        assertThrows(IllegalArgumentException.class, command::execute);

        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals(3, inventory.get("milk").getBatches().get("inv-1").getQuantity());
    }

    @Test
    void executeAcrossMultipleBatchesStoresEverySoldBatch() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-2", 3, BigDecimal.ONE, "456"));
        SellItemCommand command = new SellItemCommand(inventory, "milk", 4);

        command.execute();

        assertEquals(2, command.getSoldBatches().size());
        assertEquals(2, command.getSoldBatches().get(0).quantity());
        assertEquals(2, command.getSoldBatches().get(1).quantity());
        assertEquals(1, inventory.get("milk").getTotalQuantity());
    }

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        return inventory;
    }
}
