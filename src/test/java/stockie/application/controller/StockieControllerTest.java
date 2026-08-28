package stockie.application.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.FindQueryResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.RecallBatchResult;
import stockie.application.result.SellItemResult;
import stockie.application.result.UpdateSkuResult;
import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;
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
    void addSaveFailureLeavesInventoryUnchanged() {
        InventoryService failingInventory = new InventoryService();
        StockieController failingController = controllerWithFailingRepository(failingInventory);

        AddBatchResult result = failingController.addBatch(request("Milk", "MILK", "INV-1", 3));

        assertEquals(" unable to save inventory; addition cancelled", result.message());
        assertEquals(0, failingInventory.size());
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
    void addBatchMismatchedCategoryReturnsError() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        AddBatchResult result = controller.addBatch(perishableRequest("Milk", "MILK", "INV-2", 2,
                LocalDate.now()));

        assertNull(result.item());
        assertEquals(" item category does not match existing item: Milk", result.message());
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
    void findMissingNameAndSkuReturnExpectedMessages() {
        assertEquals(" no item found with item: Milk", controller.findByName("Milk").message());
        assertEquals(" no item found with sku: MILK", controller.findBySku("MILK").message());
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
    void sellNonPositiveQuantityReturnsValidationError() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        SellItemResult zeroResult = controller.sellItemByName("Milk", 0);
        SellItemResult negativeResult = controller.sellItemByName("Milk", -1);

        assertEquals(" quantity must be positive", zeroResult.message());
        assertEquals(" quantity must be positive", negativeResult.message());
        assertEquals(3, inventory.get("milk").getTotalQuantity());
    }

    @Test
    void recallBySkuRemovesRequestedBatch() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        RecallBatchResult result = controller.recallBatchBySku("milk", "inv-1");

        assertNull(result.message());
        assertEquals(0, result.item().getTotalQuantity());
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
    void removeItemByNameRemovesItemAndSkuIndex() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        RemoveItemResult result = controller.removeItemByName("mIlK");

        assertNotNull(result.item());
        assertNull(controller.findByName("Milk").item());
        assertNull(controller.findBySku("MILK").item());
    }

    @Test
    void updateSkuByCurrentSkuUpdatesSkuIndex() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        UpdateSkuResult result = controller.updateSkuByCurrentSku("milk", "NEW-MILK");

        assertNull(result.message());
        assertEquals("NEW-MILK", result.item().getSku());
        assertNotNull(controller.findBySku("new-milk").item());
        assertNull(controller.findBySku("milk").item());
    }

    @Test
    void updateSkuToSameSkuIgnoringCaseReturnsError() {
        controller.addBatch(request("Milk", "MILK", "INV-1", 3));

        UpdateSkuResult result = controller.updateSkuByName("Milk", "milk");

        assertNull(result.item());
        assertEquals(" new sku is the same as the current sku", result.message());
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

    @Test
    void operationsForMissingItems_returnControlledErrors() {
        assertEquals(" item not found: Milk", controller.sellItemByName("Milk", 1).message());
        assertEquals(" batch not found: Milk / INV-1", controller.recallBatchByName("Milk", "INV-1").message());
        assertEquals(" item not found: Milk", controller.removeItemByName("Milk").message());
        assertEquals(" item not found: Milk", controller.updateSkuByName("Milk", "NEW").message());
    }

    @Test
    void skuBasedOperationsForMissingItemsReturnControlledErrors() {
        assertEquals(" item not found: MILK", controller.sellItemBySku("MILK", 1).message());
        assertEquals(" batch not found: MILK / INV-1", controller.recallBatchBySku("MILK", "INV-1").message());
        assertEquals(" item not found: MILK", controller.removeItemBySku("MILK").message());
        assertEquals(" item not found: MILK", controller.updateSkuByCurrentSku("MILK", "NEW").message());
    }

    @Test
    void redoWithoutHistoryReturnsExpectedMessage() {
        CommandResult result = controller.redo();

        assertNull(result.command());
        assertEquals(" nothing to redo", result.message());
    }

    @Test
    void listEmptyInventoryReturnsExpectedMessages() {
        assertEquals(" No items in list", controller.listItems(false).message());
        assertEquals(" No depleted items in list", controller.listItems(true).message());
        assertEquals(" No batches expiring in 0 days", controller.listExpiringBatches(0).message());
        assertEquals(" No expired batches in list", controller.listExpiredBatches().message());
    }

    @Test
    void recallSaveFailureLeavesBatchUnchanged() {
        InventoryService failingInventory = inventoryWithItem();
        StockieController failingController = controllerWithFailingRepository(failingInventory);

        RecallBatchResult result = failingController.recallBatchByName("Milk", "INV-1");

        assertEquals(" unable to save inventory; recall cancelled", result.message());
        assertEquals(3, failingInventory.get("milk").getTotalQuantity());
    }

    @Test
    void removeSaveFailureLeavesItemUnchanged() {
        InventoryService failingInventory = inventoryWithItem();
        StockieController failingController = controllerWithFailingRepository(failingInventory);

        RemoveItemResult result = failingController.removeItemByName("Milk");

        assertEquals(" unable to save inventory; item removal cancelled", result.message());
        assertNotNull(failingInventory.get("milk"));
    }

    @Test
    void updateSkuSaveFailureLeavesSkuUnchanged() {
        InventoryService failingInventory = inventoryWithItem();
        StockieController failingController = controllerWithFailingRepository(failingInventory);

        UpdateSkuResult result = failingController.updateSkuByName("Milk", "NEW-MILK");

        assertEquals(" unable to save inventory; sku update cancelled", result.message());
        assertEquals("MILK", failingInventory.get("milk").getSku());
    }

    @Test
    void sellSaveFailureLeavesStockUnchanged() {
        InventoryService failingInventory = inventoryWithItem();
        StockieController failingController = controllerWithFailingRepository(failingInventory);

        SellItemResult result = failingController.sellItemByName("Milk", 1);

        assertEquals(" unable to save inventory; sale cancelled", result.message());
        assertEquals(3, failingInventory.get("milk").getTotalQuantity());
    }

    private static AddBatchRequest request(String name, String sku, String invoice, int quantity) {
        return new AddBatchRequest(name, sku, invoice, quantity, BigDecimal.ONE, null, "123");
    }

    private static AddBatchRequest perishableRequest(String name, String sku, String invoice, int quantity,
            LocalDate expiryDate) {
        return new AddBatchRequest(name, sku, invoice, quantity, BigDecimal.ONE, expiryDate, "123");
    }

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.ONE, "123"));
        return inventory;
    }

    private static StockieController controllerWithFailingRepository(InventoryService inventory) {
        InventoryRepository repository = new InventoryRepository() {
            @Override
            public HashMap<String, InventoryItem> load() {
                return new HashMap<>();
            }

            @Override
            public void save(HashMap<String, InventoryItem> snapshot) throws IOException {
                throw new IOException("save failed");
            }
        };
        return new StockieController(inventory, new CommandManager(inventory, repository), repository);
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
