package stockie.model;

import java.math.BigDecimal;

/** Immutable batch that has no expiry date. */
public final class NonPerishableBatch extends Batch {
    public NonPerishableBatch(String invoiceNumber, int quantity, BigDecimal unitPrice, String upc) {
        super(invoiceNumber, quantity, unitPrice, upc);
    }
}

