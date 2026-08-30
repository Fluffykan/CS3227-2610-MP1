package stockie.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import stockie.application.exception.ItemNotFoundException;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;
import stockie.storage.InventoryRepository;

class InventoryServiceTest {
    @Test
    void addBatchNewItemCreatesItemAndIndexesSku() {
        InventoryService service = new InventoryService();

        service.addBatch("milk", "Milk", " MILK ", ItemCategory.NON_PERISHABLE,
                batch("INV-1", 3));

        assertNotNull(service.get("milk"));
        assertEquals(3, service.getBySku(" milk ").getTotalQuantity());
    }

    @Test
    void addBatchNewItemWithZeroQuantityRejectsBatchWithoutCreatingIndexes() {
        InventoryService service = new InventoryService();

        assertThrows(IllegalArgumentException.class, () -> service.addBatch("milk", "Milk", "MILK",
                ItemCategory.NON_PERISHABLE, batch("INV-1", 0)));

        assertEquals(0, service.size());
        assertNull(service.get("milk"));
        assertNull(service.getBySku("milk"));
    }

    @Test
    void addBatchNewItemWithNegativeQuantityRejectsBatchWithoutCreatingIndexes() {
        InventoryService service = new InventoryService();

        assertThrows(IllegalArgumentException.class, () -> service.addBatch("milk", "Milk", "MILK",
                ItemCategory.NON_PERISHABLE, batch("INV-1", -1)));

        assertEquals(0, service.size());
        assertNull(service.get("milk"));
        assertNull(service.getBySku("milk"));
    }

    @Test
    void addBatchWithNullBatchRejectsWithoutCreatingIndexes() {
        InventoryService service = new InventoryService();

        assertThrows(IllegalArgumentException.class, () -> service.addBatch("milk", "Milk", "MILK",
                ItemCategory.NON_PERISHABLE, null));

        assertEquals(0, service.size());
        assertNull(service.get("milk"));
        assertNull(service.getBySku("milk"));
    }

    @Test
    void addBatchWithNullInvoiceRejectsWithoutCreatingIndexes() {
        InventoryService service = new InventoryService();

        assertThrows(IllegalArgumentException.class, () -> service.addBatch("milk", "Milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch(null, 1, BigDecimal.ONE, "UPC")));

        assertEquals(0, service.size());
        assertNull(service.get("milk"));
        assertNull(service.getBySku("milk"));
    }

    @Test
    void addBatchWithNullPriceRejectsWithoutCreatingIndexes() {
        InventoryService service = new InventoryService();

        assertThrows(IllegalArgumentException.class, () -> service.addBatch("milk", "Milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 1, null, "UPC")));

        assertEquals(0, service.size());
        assertNull(service.get("milk"));
        assertNull(service.getBySku("milk"));
    }

    @Test
    void addBatchExistingItemAddsBatchAndUpdatesTotals() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 2));

        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-2", 4));

        assertEquals(2, service.get("milk").getBatches().size());
        assertEquals(6, service.get("milk").getTotalQuantity());
    }

    @Test
    void addBatchExistingItemWithMismatchedCategoryRejectsBatch() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 2));

        assertThrows(IllegalArgumentException.class, () -> service.addBatch("milk", "Milk", "MILK",
                ItemCategory.PERISHABLE, batch("INV-2", 1)));
        assertEquals(2, service.get("milk").getTotalQuantity());
    }

    @Test
    void addBatchExistingItemWithMismatchedSkuRejectsBatch() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 2));

        assertThrows(IllegalArgumentException.class, () -> service.addBatch("milk", "Milk", "OTHER",
                ItemCategory.NON_PERISHABLE, batch("INV-2", 1)));
        assertEquals(2, service.get("milk").getTotalQuantity());
    }

    @Test
    void addBatchNormalizesInvoiceNumberForStorage() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                batch(" Inv-1 ", 1));

        assertEquals(" Inv-1 ", service.get("milk").getBatches().get(" inv-1 ").getInvoiceNumber());
    }

    @Test
    void sellExactAvailableQuantityRemovesAllStock() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 3));

        assertEquals(3, service.sell("milk", 3).get(0).quantity());
        assertEquals(0, service.get("milk").getTotalQuantity());
    }

    @Test
    void sellPartialQuantityUpdatesRemainingStockAndCost() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 5, BigDecimal.TEN, "UPC"));

        service.sell("milk", 2);

        assertEquals(3, service.get("milk").getTotalQuantity());
        assertEquals(new BigDecimal("30"), service.get("milk").getTotalCost());
    }

    @Test
    void sellInvalidQuantityRejectsMutation() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 3));

        assertThrows(IllegalArgumentException.class, () -> service.sell("milk", 0));
        assertThrows(IllegalArgumentException.class, () -> service.sell("milk", -1));
        assertThrows(IllegalArgumentException.class, () -> service.sell("milk", 4));
        assertEquals(3, service.get("milk").getTotalQuantity());
    }

    @Test
    void recallBatchExistingBatchRemovesBatchAndUpdatesTotals() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 2));

        service.recallBatch("milk", "inv-1");

        assertEquals(0, service.get("milk").getTotalQuantity());
        assertNull(service.get("milk").getBatches().get("inv-1"));
    }

    @Test
    void recallMissingBatchThrowsControlledException() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 2));

        assertThrows(IllegalArgumentException.class, () -> service.recallBatch("milk", "missing"));
        assertEquals(2, service.get("milk").getTotalQuantity());
    }

    @Test
    void updateSkuUpdatesNewIndexAndRemovesOldIndex() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 1));

        service.updateSku("milk", "NEW-MILK");

        assertSameItem(service.get("milk"), service.getBySku("new-milk"));
        assertNull(service.getBySku("milk"));
    }

    @Test
    void copyItemReturnsIndependentCopy() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 1));

        InventoryItem copy = service.copyItem("milk");
        copy.sell(1);

        assertEquals(1, service.get("milk").getTotalQuantity());
    }

    @Test
    void loadCorruptedEntriesSkipsInvalidEntriesAndReturnsSkippedKeys() throws Exception {
        HashMap<String, InventoryItem> data = new HashMap<>();
        data.put("valid", item("Milk", "MILK"));
        data.put("null-item", null);
        data.put("missing-category", item("Missing Category", "MISSING", null));
        InventoryService service = new InventoryService();

        List<String> skipped = service.load(repository(data));

        assertEquals(2, skipped.size());
        assertEquals(1, service.size());
        assertNotNull(service.getBySku("milk"));
    }

    @Test
    void loadInvalidNestedBatchesSkipsTheirItems() throws Exception {
        InventoryItem nullBatchItem = item("Null Batch", "NULL-BATCH");
        nullBatchItem.getBatches().put("inv-1", null);
        InventoryItem invalidQuantityItem = item("Invalid Quantity", "INVALID-QUANTITY");
        invalidQuantityItem.getBatches().put("inv-1", new NonPerishableBatch("INV-1", 0, BigDecimal.ONE, null));
        InventoryItem invalidPriceItem = item("Invalid Price", "INVALID-PRICE");
        invalidPriceItem.getBatches().put("inv-1", new NonPerishableBatch("INV-1", 1, null, null));
        HashMap<String, InventoryItem> data = new HashMap<>();
        data.put("valid", item("Milk", "MILK"));
        data.put("null-batch", nullBatchItem);
        data.put("invalid-quantity", invalidQuantityItem);
        data.put("invalid-price", invalidPriceItem);

        InventoryService service = new InventoryService();

        List<String> skipped = service.load(repository(data));

        assertEquals(List.of("invalid-price", "invalid-quantity", "null-batch"), skipped.stream().sorted().toList());
        assertEquals(1, service.size());
        assertNotNull(service.getBySku("milk"));
    }

    @Test
    void loadDuplicateNormalizedSkuSkipsDuplicateEntry() throws Exception {
        HashMap<String, InventoryItem> data = new HashMap<>();
        data.put("first", item("Milk", "MILK"));
        data.put("second", item("Bread", "MILK"));
        InventoryService service = new InventoryService();

        List<String> skipped = service.load(repository(data));

        assertEquals(1, skipped.size());
        assertEquals(1, service.size());
    }

    @Test
    void loadRepositoryThrowsIoExceptionPropagatesException() {
        InventoryService service = new InventoryService();

        assertThrows(IOException.class, () -> service.load(failingRepository(new IOException())));
    }

    @Test
    void loadRepositoryThrowsClassNotFoundExceptionPropagatesException() {
        InventoryService service = new InventoryService();

        assertThrows(ClassNotFoundException.class,
                () -> service.load(failingRepository(new ClassNotFoundException())));
    }

    @Test
    void sellMissingItemThrowsItemNotFoundException() {
        assertThrows(ItemNotFoundException.class, () -> new InventoryService().sell("missing", 1));
    }

    @Test
    void recallBatchMissingItemThrowsItemNotFoundException() {
        assertThrows(ItemNotFoundException.class,
                () -> new InventoryService().recallBatch("missing", "INV-1"));
    }

    @Test
    void updateSkuMissingItemThrowsItemNotFoundException() {
        assertThrows(ItemNotFoundException.class,
                () -> new InventoryService().updateSku("missing", "SKU"));
    }

    @Test
    void updateSkuEmptyValueRejectsUpdate() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 1));

        assertThrows(IllegalArgumentException.class, () -> service.updateSku("milk", ""));
        assertEquals("MILK", service.get("milk").getSku());
    }

    @Test
    void updateSkuDuplicateNormalizedValueRejectsUpdate() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 1));
        service.addBatch("bread", "Bread", "BREAD", ItemCategory.NON_PERISHABLE, batch("INV-2", 1));

        assertThrows(IllegalArgumentException.class, () -> service.updateSku("milk", "bread"));
        assertEquals("MILK", service.get("milk").getSku());
    }

    @Test
    void removeMissingItemLeavesInventoryAndSkuIndexUnchanged() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 1));

        service.removeItem("missing");

        assertEquals(1, service.size());
        assertNotNull(service.getBySku("milk"));
    }

    @Test
    void restoreNullItemRemovesExistingItemAndSkuIndex() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 1));

        service.restoreItem("milk", null);

        assertNull(service.get("milk"));
        assertNull(service.getBySku("milk"));
    }

    @Test
    void restoreItemRebuildsSkuIndexWithIndependentCopy() {
        InventoryService service = new InventoryService();
        service.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE, batch("INV-1", 1));
        InventoryItem snapshot = service.copyItem("milk");

        service.updateSku("milk", "NEW-MILK");
        service.restoreItem("milk", snapshot);

        assertEquals("MILK", service.getBySku("milk").getSku());
        assertNull(service.getBySku("new-milk"));
        assertEquals(1, service.get("milk").getTotalQuantity());
    }

    @Test
    void loadNullRepositoryDataThrowsControlledException() {
        InventoryRepository repository = new InventoryRepository() {
            @Override
            public HashMap<String, InventoryItem> load() {
                return null;
            }

            @Override
            public void save(HashMap<String, InventoryItem> snapshot) { }
        };

        assertThrows(IllegalStateException.class, () -> new InventoryService().load(repository));
    }

    private static InventoryItem item(String name, String sku) {
        return new InventoryItem(name, sku, ItemCategory.NON_PERISHABLE);
    }

    private static InventoryItem item(String name, String sku, ItemCategory category) {
        return new InventoryItem(name, sku, category);
    }

    private static NonPerishableBatch batch(String invoice, int quantity) {
        return new NonPerishableBatch(invoice, quantity, BigDecimal.ONE, "UPC");
    }

    private static InventoryRepository repository(HashMap<String, InventoryItem> data) {
        return new InventoryRepository() {
            @Override
            public HashMap<String, InventoryItem> load() {
                return data;
            }

            @Override
            public void save(HashMap<String, InventoryItem> snapshot) { }
        };
    }

    private static InventoryRepository failingRepository(Exception exception) {
        return new InventoryRepository() {
            @Override
            public HashMap<String, InventoryItem> load() throws IOException, ClassNotFoundException {
                if (exception instanceof IOException) {
                    throw (IOException) exception;
                }
                throw (ClassNotFoundException) exception;
            }

            @Override
            public void save(HashMap<String, InventoryItem> snapshot) { }
        };
    }

    private static void assertSameItem(InventoryItem expected, InventoryItem actual) {
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
        assertEquals(expected.getSku(), actual.getSku());
    }
}
