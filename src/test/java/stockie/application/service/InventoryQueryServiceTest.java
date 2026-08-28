package stockie.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;
import stockie.entities.PerishableBatch;

class InventoryQueryServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

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
}
