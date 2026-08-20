package stockie.application.request;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Carries validated data needed to add one inventory batch. */
public record AddBatchRequest(String itemName, String sku, String invoiceNumber, int quantity,
        BigDecimal unitPrice, LocalDate expiryDate, String upc) { }
