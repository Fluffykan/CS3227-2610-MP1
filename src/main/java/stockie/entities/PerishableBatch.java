package stockie.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Immutable batch whose expiry date is mandatory. */
public final class PerishableBatch extends Batch {
    private final LocalDate expiryDate;

    /** Creates a perishable batch with an expiry date. */
    public PerishableBatch(String invoiceNumber, int quantity, BigDecimal unitPrice,
            LocalDate expiryDate, String upc) {
        super(invoiceNumber, quantity, unitPrice, upc);
        this.expiryDate = expiryDate;
    }

    public LocalDate getExpiryDate() { return expiryDate; }
}

