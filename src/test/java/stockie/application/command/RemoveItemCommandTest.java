package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import stockie.application.service.InventoryService;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;

class RemoveItemCommandTest {
    @Test
    void executeAndUndo_restoresCompleteItem() {
        InventoryService inventory = inventoryWithItem();
        RemoveItemCommand command = new RemoveItemCommand(inventory, "milk");

        command.execute();
        assertNull(inventory.get("milk"));
        command.undo();

        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals("MILK", command.getAffectedItem().getSku());
    }

    @Test
    void executeTwice_removesItemOnlyOnce() {
        InventoryService inventory = inventoryWithItem();
        RemoveItemCommand command = new RemoveItemCommand(inventory, "milk");

        command.execute();
        command.execute();

        assertNull(inventory.get("milk"));
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
