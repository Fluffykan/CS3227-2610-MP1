package stockie.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import stockie.application.result.ExpiringItem;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;
import stockie.entities.PerishableBatch;

class InventoryQueryServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    @Test
    void findByNameAndFindBySkuAreCaseInsensitive() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 1, BigDecimal.ONE, "1"));
        InventoryQueryService queries = new InventoryQueryService(inventory);

        assertEquals("Milk", queries.findByName("MILK").getDisplayName());
        assertEquals("Milk", queries.findBySku("milk").getDisplayName());
    }

    @Test
    void findByNameAndFindBySkuReturnNullForMissingValues() {
        InventoryQueryService queries = new InventoryQueryService(new InventoryService());

        assertNull(queries.findByName("missing"));
        assertNull(queries.findBySku("missing"));
    }

    @Test
    void listDepletedOnlyReturnsOnlyItemsWithNoRemainingStock() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("full", "Full", "FULL", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("FULL-1", 2, BigDecimal.ONE, "1"));
        inventory.addBatch("depleted", "Depleted", "DEPLETED", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("DEP-1", 1, BigDecimal.ONE, "2"));
        inventory.sell("depleted", 1);
        InventoryQueryService queries = new InventoryQueryService(inventory);

        List<InventoryItem> depleted = queries.list(true);

        assertEquals(List.of("DEPLETED"), depleted.stream().map(InventoryItem::getSku).toList());
    }

    @Test
    void listExpiringInInclusiveWindowReturnsMatchingPerishableBatchesOnly() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("BEFORE", 1, BigDecimal.ONE, TODAY.minusDays(1), "1"));
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("START", 1, BigDecimal.ONE, TODAY, "2"));
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("END", 1, BigDecimal.ONE, TODAY.plusDays(3), "3"));
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("AFTER", 1, BigDecimal.ONE, TODAY.plusDays(4), "4"));
        InventoryQueryService queries = new InventoryQueryService(inventory);

        List<String> invoices = queries.listExpiringIn(TODAY, 3).get(0).batches().stream()
                .map(PerishableBatch::getInvoiceNumber)
                .toList();

        assertEquals(List.of("START", "END"), invoices);
    }

    @Test
    void listExpiredExcludesBatchesExpiringToday() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("EXPIRED", 1, BigDecimal.ONE, TODAY.minusDays(1), "1"));
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("TODAY", 1, BigDecimal.ONE, TODAY, "2"));
        InventoryQueryService queries = new InventoryQueryService(inventory);

        List<String> invoices = queries.listExpired(TODAY).get(0).batches().stream()
                .map(PerishableBatch::getInvoiceNumber)
                .toList();

        assertEquals(List.of("EXPIRED"), invoices);
    }

    @Test
    void listExpiringInZeroDaysReturnsBatchesExpiringToday() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("today", "Today", "TODAY", ItemCategory.PERISHABLE,
                new PerishableBatch("TODAY-1", 1, BigDecimal.ONE, TODAY, "1"));
        inventory.addBatch("tomorrow", "Tomorrow", "TOMORROW", ItemCategory.PERISHABLE,
                new PerishableBatch("TOMORROW-1", 1, BigDecimal.ONE, TODAY.plusDays(1), "2"));

        List<ExpiringItem> results = new InventoryQueryService(inventory).listExpiringIn(TODAY, 0);

        assertEquals(List.of("Today"), results.stream().map(result -> result.item().getDisplayName()).toList());
    }

    @Test
    void listExpiringInIncludesBothWindowBoundaries() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("START", 1, BigDecimal.ONE, TODAY, "1"));
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("END", 1, BigDecimal.ONE, TODAY.plusDays(2), "2"));

        List<String> invoices = new InventoryQueryService(inventory).listExpiringIn(TODAY, 2).get(0).batches()
                .stream().map(PerishableBatch::getInvoiceNumber).toList();

        assertEquals(List.of("START", "END"), invoices);
    }

    @Test
    void listExpiringInExcludesBatchOneDayOutsideWindow() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("OUTSIDE", 1, BigDecimal.ONE, TODAY.plusDays(3), "1"));

        assertEquals(List.of(), new InventoryQueryService(inventory).listExpiringIn(TODAY, 2));
    }

    @Test
    void listExpiringInNegativeDaysReturnsNoResults() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("TODAY", 1, BigDecimal.ONE, TODAY, "1"));

        assertEquals(List.of(), new InventoryQueryService(inventory).listExpiringIn(TODAY, -1));
    }

    @Test
    void listExpiringInNullExpiryDateSkipsBatch() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.PERISHABLE,
                new PerishableBatch("MISSING-DATE", 1, BigDecimal.ONE, null, "1"));

        assertEquals(List.of(), new InventoryQueryService(inventory).listExpiringIn(TODAY, 0));
    }

    @Test
    void listOrdersItemsBySkuThenName() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("z-item", "Zed", "SAME", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("Z-1", 1, BigDecimal.ONE, "1"));
        inventory.addBatch("a-item", "Alpha", "SAME", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("A-1", 1, BigDecimal.ONE, "2"));
        inventory.addBatch("b-item", "Beta", "BETA", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("B-1", 1, BigDecimal.ONE, "3"));

        List<String> names = new InventoryQueryService(inventory).list(false).stream()
                .map(InventoryItem::getDisplayName).toList();

        assertEquals(List.of("Beta", "Alpha", "Zed"), names);
    }

    @Test
    void listExpiringBatchesOrdersByExpiryThenSkuThenName() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("zed", "Zed", "Z-SKU", ItemCategory.PERISHABLE,
                new PerishableBatch("Z-1", 1, BigDecimal.ONE, TODAY.plusDays(1), "1"));
        inventory.addBatch("alpha", "Alpha", "Z-SKU", ItemCategory.PERISHABLE,
                new PerishableBatch("A-1", 1, BigDecimal.ONE, TODAY.plusDays(1), "2"));
        inventory.addBatch("beta", "Beta", "A-SKU", ItemCategory.PERISHABLE,
                new PerishableBatch("B-1", 1, BigDecimal.ONE, TODAY.plusDays(1), "3"));
        inventory.addBatch("earliest", "Earliest", "EARLIEST", ItemCategory.PERISHABLE,
                new PerishableBatch("E-1", 1, BigDecimal.ONE, TODAY, "4"));

        List<String> names = new InventoryQueryService(inventory).listExpiringIn(TODAY, 2).stream()
                .map(result -> result.item().getDisplayName()).toList();

        assertEquals(List.of("Earliest", "Beta", "Alpha", "Zed"), names);
    }
}
