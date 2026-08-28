package stockie.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BatchTest {
    @Test
    void batchStoresFieldsAndCalculatesTotalCost() {
        Batch batch = new NonPerishableBatch("INV-1", 4, new BigDecimal("2.50"), "UPC-1");

        assertEquals("INV-1", batch.getInvoiceNumber());
        assertEquals(4, batch.getQuantity());
        assertEquals(new BigDecimal("2.50"), batch.getUnitPrice());
        assertEquals("UPC-1", batch.getUpc());
        assertEquals(new BigDecimal("10.00"), batch.getTotalCost());
    }

    @Test
    void perishableBatchStoresExpiryDate() {
        LocalDate expiry = LocalDate.of(2026, 8, 29);
        PerishableBatch batch = new PerishableBatch("INV-2", 2, BigDecimal.ZERO, expiry, null);

        assertEquals(expiry, batch.getExpiryDate());
        assertNull(batch.getUpc());
        assertEquals(BigDecimal.ZERO, batch.getTotalCost());
    }
}
