package stockie.model;

import java.io.Serializable;
import java.math.BigDecimal;

/** Common immutable data for all inventory batches. */
public abstract class Batch implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String invoiceNumber;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final String upc;

    public Batch(String invoiceNumber, int quantity, BigDecimal unitPrice, String upc) {
        this.invoiceNumber = invoiceNumber;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.upc = upc;
    }

    public String getInvoiceNumber() { return invoiceNumber; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getUpc() { return upc; }
    public BigDecimal getTotalCost() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}

