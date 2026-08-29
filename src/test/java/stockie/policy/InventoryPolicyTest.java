package stockie.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.storage.InventoryRepository;

class InventoryPolicyTest {
    @Test
    void maxItems_isPositiveAndMatchesConfiguredLimit() {
        assertEquals(200_000, InventoryPolicy.MAX_ITEMS);
    }

    @Test
    void controllerEnforcesMaximumForNewItemsButAllowsExistingItems()
            throws IOException, ClassNotFoundException {
        HashMap<String, InventoryItem> loadedItems = new HashMap<>();
        for (int index = 0; index < InventoryPolicy.MAX_ITEMS; index++) {
            loadedItems.put("item " + index,
                    new InventoryItem("Item " + index, "SKU-" + index, ItemCategory.NON_PERISHABLE));
        }

        InventoryService inventory = new InventoryService();
        InventoryRepository repository = repositoryContaining(loadedItems);
        inventory.load(repository);
        StockieController controller = new StockieController(inventory,
                new CommandManager(inventory, repository), repository);

        var newItemResult = controller.addBatch(new AddBatchRequest(
                "New item", "NEW-SKU", "INV-1", 1, BigDecimal.ONE, null, null));

        assertNull(newItemResult.item());
        assertEquals(" cannot track more than " + InventoryPolicy.MAX_ITEMS, newItemResult.message());
        assertEquals(InventoryPolicy.MAX_ITEMS, inventory.size());

        var result = controller.addBatch(new AddBatchRequest(
                "Item 0", "SKU-0", "INV-1", 1, BigDecimal.ONE, null, null));

        assertNotNull(result.item());
        assertNull(result.message());
        assertEquals(InventoryPolicy.MAX_ITEMS, inventory.size());
        assertEquals(1, inventory.get("item 0").getTotalQuantity());
    }

    private static InventoryRepository repositoryContaining(HashMap<String, InventoryItem> items) {
        return new InventoryRepository() {
            @Override
            public HashMap<String, InventoryItem> load() {
                return items;
            }

            @Override
            public void save(HashMap<String, InventoryItem> snapshot) {
            }
        };
    }
}
