package stockie.application.result;

import java.math.BigDecimal;

/** Describes the quantity taken from one invoice while completing a sale. */
public record SoldBatch(String invoiceNumber, int quantity, BigDecimal unitPrice) { }
