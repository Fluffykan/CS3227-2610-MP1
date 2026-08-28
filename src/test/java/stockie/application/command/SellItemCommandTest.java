package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
