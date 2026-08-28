package stockie.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import stockie.application.result.SoldBatch;

class InventoryItemTest {
    private static final BigDecimal UNIT_PRICE = new BigDecimal("2.50");

    @Test
    void addBatchPerishableItemCreatesPerishableBatchAndUpdatesTotals() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);

        item.addBatch("inv-1", "INV-1", 4, UNIT_PRICE,
                LocalDate.of(2026, 9, 1), "123");

        assertInstanceOf(PerishableBatch.class, item.getBatches().get("inv-1"));
        assertEquals(4, item.getTotalQuantity());
        assertEquals(new BigDecimal("10.00"), item.getTotalCost());
    }

    @Test
    void sellPerishableItemSellsEarliestExpiryAndSupportsPartialBatch() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);
        item.addBatch("later", "LATER", 3, UNIT_PRICE,
                LocalDate.of(2026, 10, 1), "123");
        item.addBatch("earlier", "EARLIER", 5, UNIT_PRICE,
                LocalDate.of(2026, 9, 1), "456");

        List<SoldBatch> sold = item.sell(6);

        assertEquals(List.of(
                new SoldBatch("EARLIER", 5, UNIT_PRICE),
                new SoldBatch("LATER", 1, UNIT_PRICE)), sold);
        assertEquals(2, item.getTotalQuantity());
        assertEquals(new BigDecimal("5.00"), item.getTotalCost());
        assertEquals(2, item.getBatches().get("later").getQuantity());
    }

    @Test
    void sellNonPerishableItemOrdersByInvoiceNumber() {
        InventoryItem item = new InventoryItem("Rice", "RICE-1", ItemCategory.NON_PERISHABLE);
        item.addBatch("z", "Z-INV", 2, UNIT_PRICE, null, "123");
        item.addBatch("a", "A-INV", 2, UNIT_PRICE, null, "456");

        List<SoldBatch> sold = item.sell(1);

        assertEquals(List.of(new SoldBatch("A-INV", 1, UNIT_PRICE)), sold);
        assertEquals(3, item.getTotalQuantity());
    }

    @Test
    void sellExactAvailableQuantityRemovesAllInventory() {
        InventoryItem item = new InventoryItem("Rice", "RICE-1", ItemCategory.NON_PERISHABLE);
        item.addBatch("a", "A-INV", 3, UNIT_PRICE, null, "123");

        List<SoldBatch> sold = item.sell(3);

        assertEquals(List.of(new SoldBatch("A-INV", 3, UNIT_PRICE)), sold);
        assertEquals(0, item.getTotalQuantity());
        assertEquals(0, item.getBatches().size());
    }

    @Test
    void recallBatchExistingBatchRemovesBatchAndUpdatesTotals() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);
        item.addBatch("inv-1", "INV-1", 4, UNIT_PRICE,
                LocalDate.of(2026, 9, 1), "123");

        Batch recalled = item.recallBatch("inv-1");

        assertEquals("INV-1", recalled.getInvoiceNumber());
        assertEquals(0, item.getTotalQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getTotalCost()));
        assertFalse(item.hasBatch("inv-1"));
    }

    @Test
    void deepCopyMutatingCopyDoesNotChangeOriginal() {
        InventoryItem original = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);
        original.addBatch("inv-1", "INV-1", 4, UNIT_PRICE,
                LocalDate.of(2026, 9, 1), "123");

        InventoryItem copy = original.deepCopy();
        copy.sell(2);

        assertEquals(4, original.getTotalQuantity());
        assertEquals(2, copy.getTotalQuantity());
    }

    @Test
    void addBatchPerishableItemWithoutExpiryKeepsMissingExpiryDate() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);

        item.addBatch("inv-1", "INV-1", 1, UNIT_PRICE, null, "123");

        assertNull(((PerishableBatch) item.getBatches().get("inv-1")).getExpiryDate());
    }

    @Test
    void addDuplicateInvoiceRejectsBatchWithoutChangingTotals() {
        InventoryItem item = itemWithBatch(3);

        assertThrows(IllegalArgumentException.class,
                () -> item.addBatch("inv-1", "INV-1", 2, UNIT_PRICE, null, "456"));
        assertEquals(3, item.getTotalQuantity());
        assertEquals(1, item.getBatches().size());
    }

    @Test
    void addBatchZeroQuantityRejectsBatch() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);

        assertThrows(IllegalArgumentException.class, () -> item.addBatch("inv-1", "INV-1", 0,
                UNIT_PRICE, LocalDate.of(2026, 9, 1), "123"));

        assertEquals(0, item.getTotalQuantity());
    }

    @Test
    void addBatchNegativeQuantityRejectsBatch() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);

        assertThrows(IllegalArgumentException.class, () -> item.addBatch("inv-1", "INV-1", -1,
                UNIT_PRICE, LocalDate.of(2026, 9, 1), "123"));
    }

    @Test
    void addBatchNegativePriceAddsBatchAndUpdatesCost() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);
        BigDecimal negativePrice = new BigDecimal("-2.50");

        item.addBatch("inv-1", "INV-1", 1, negativePrice,
                LocalDate.of(2026, 9, 1), "123");

        assertEquals(negativePrice, item.getTotalCost());
    }

    @Test
    void addBatchZeroPriceAddsFreeGiveawayBatch() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);

        item.addBatch("inv-1", "INV-1", 3, BigDecimal.ZERO,
                LocalDate.of(2026, 9, 1), "123");

        assertEquals(3, item.getTotalQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getTotalCost()));
    }

    @Test
    void addBatchEmptySkuRejectsItem() {
        assertThrows(IllegalArgumentException.class,
                () -> new InventoryItem("Milk", "", ItemCategory.NON_PERISHABLE));
    }

    @Test
    void addBatchEmptyNameRejectsItem() {
        assertThrows(IllegalArgumentException.class,
                () -> new InventoryItem("", "MILK-1", ItemCategory.NON_PERISHABLE));
    }

    @Test
    void sellZeroQuantityRejectsQuantity() {
        InventoryItem item = itemWithBatch(3);

        assertThrows(IllegalArgumentException.class, () -> item.sell(0));
        assertEquals(3, item.getTotalQuantity());
    }

    @Test
    void sellNegativeQuantityRejectsQuantity() {
        InventoryItem item = itemWithBatch(3);

        assertThrows(IllegalArgumentException.class, () -> item.sell(-1));
        assertEquals(3, item.getTotalQuantity());
    }

    @Test
    void sellMoreThanAvailableQuantityRejectsQuantity() {
        InventoryItem item = itemWithBatch(3);

        assertThrows(IllegalArgumentException.class, () -> item.sell(4));
        assertEquals(3, item.getTotalQuantity());
    }

    @Test
    void sellEmptyInventoryRejectsPositiveQuantity() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.NON_PERISHABLE);

        assertThrows(IllegalArgumentException.class, () -> item.sell(1));
    }

    @Test
    void recallMissingInvoiceThrowsException() {
        InventoryItem item = itemWithBatch(3);

        assertThrows(IllegalArgumentException.class, () -> item.recallBatch("missing"));
        assertEquals(3, item.getTotalQuantity());
    }

    @Test
    void sellPerishableBatchesWithSameExpiryUsesInvoiceOrder() {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.PERISHABLE);
        LocalDate expiry = LocalDate.of(2026, 9, 1);
        item.addBatch("z", "Z-INV", 1, UNIT_PRICE, expiry, "123");
        item.addBatch("a", "A-INV", 1, UNIT_PRICE, expiry, "456");

        assertEquals("A-INV", item.sell(1).get(0).invoiceNumber());
    }

    private static InventoryItem itemWithBatch(int quantity) {
        InventoryItem item = new InventoryItem("Milk", "MILK-1", ItemCategory.NON_PERISHABLE);
        item.addBatch("inv-1", "INV-1", quantity, UNIT_PRICE, null, "123");
        return item;
    }
}
