package stockie.application.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.FindQueryResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.SellItemResult;
import stockie.application.result.UpdateSkuResult;
import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.storage.InventoryRepository;

class StockieControllerTest {
    private InMemoryRepository repository;
    private InventoryService inventory;
    private StockieController controller;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        inventory = new InventoryService();
        controller = new StockieController(inventory, new CommandManager(inventory, repository), repository);
    }

    @Test
    void addBatchNewItemCreatesItemAndPersistsSnapshot() {
        AddBatchResult result = controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        assertNotNull(result.item());
        assertNull(result.message());
        assertEquals(3, result.item().getTotalQuantity());
        assertNotNull(repository.savedSnapshot);
    }

    @Test
    void addBatchDuplicateInvoiceReturnsErrorWithoutAddingBatch() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        AddBatchResult result = controller.addBatch(request("Milk", "MILK", "INV-1", 2));

        assertNull(result.item());
        assertEquals(" invoice already exists: INV-1", result.message());
        assertEquals(3, inventory.get("milk").getTotalQuantity());
    }

    @Test
    void addBatchMismatchedSkuReturnsError() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        AddBatchResult result = controller.addBatch(request("Milk", "OTHER", "INV-2", 2));

        assertNull(result.item());
        assertEquals(" sku does not match existing item: Milk", result.message());
    }

    @Test
    void findByNameAndFindBySkuAreCaseInsensitive() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        FindQueryResult byName = controller.findByName("mIlK");
        FindQueryResult bySku = controller.findBySku("milk");

        assertNotNull(byName.item());
        assertNotNull(bySku.item());
        assertEquals("Milk", byName.item().getDisplayName());
    }

    @Test
    void sellItemByNameExactAvailableQuantityDepletesItem() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        SellItemResult result = controller.sellItemByName("Milk", 3);

        assertNotNull(result.item());
        assertNull(result.message());
        assertEquals(3, result.soldBatches().get(0).quantity());
        assertEquals(0, result.item().getTotalQuantity());
    }

    @Test
    void sellItemInsufficientStockReturnsErrorWithoutChangingInventory() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        SellItemResult result = controller.sellItemByName("Milk", 4);

        assertNull(result.item());
        assertEquals(" insufficient stock: Milk", result.message());
        assertEquals(3, inventory.get("milk").getTotalQuantity());
    }

    @Test
    void updateSkuValidSkuUpdatesSkuIndex() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        UpdateSkuResult result = controller.updateSkuByName("Milk", "NEW-MILK");

        assertNotNull(result.item());
        assertEquals("MILK", result.oldSku());
        assertEquals("NEW-MILK", result.item().getSku());
        assertNotNull(controller.findBySku("new-milk").item());
        assertNull(controller.findBySku("milk").item());
    }

    @Test
    void updateSkuDuplicateSkuReturnsError() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));
        controller.addBatch(request("Bread", "BREAD", "INV-2", 2));

        UpdateSkuResult result = controller.updateSkuByName("Milk", "BREAD");

        assertNull(result.item());
        assertEquals(" sku already exists: BREAD", result.message());
    }

    @Test
    void removeItemBySkuRemovesItemAndSkuIndex() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        RemoveItemResult result = controller.removeItemBySku("milk");

        assertNotNull(result.item());
        assertNull(controller.findBySku("MILK").item());
        assertNull(controller.findByName("Milk").item());
    }

    @Test
    void undoAndRedoAfterAddRestoreAndReapplyChange() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        CommandResult undo = controller.undo();
        assertNotNull(undo.command());
        assertNull(controller.findByName("Milk").item());

        CommandResult redo = controller.redo();
        assertNotNull(redo.command());
        assertNotNull(controller.findByName("Milk").item());
    }

    @Test
    void undoWithoutHistoryReturnsExpectedMessage() {
        CommandResult result = controller.undo();

        assertNull(result.command());
        assertEquals(" nothing to undo", result.message());
    }

    private static AddBatchRequest request(String name, String sku, String invoice, int quantity) {
        return new AddBatchRequest(name, sku, invoice, quantity, BigDecimal.ONE, null, "123");
    }

    private static final class InMemoryRepository implements InventoryRepository {
        private HashMap<String, InventoryItem> savedSnapshot;

        @Override
        public HashMap<String, InventoryItem> load() {
            return savedSnapshot == null ? new HashMap<>() : savedSnapshot;
        }

        @Override
        public void save(HashMap<String, InventoryItem> snapshot) throws IOException {
            savedSnapshot = snapshot;
        }
    }
}
