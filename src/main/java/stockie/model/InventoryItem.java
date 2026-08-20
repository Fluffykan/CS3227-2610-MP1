package stockie.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import stockie.application.result.SoldBatch;
import stockie.util.TextNormalizer;

/** Mutable aggregate for one item and its invoice-keyed batches. */
public final class InventoryItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String displayName;
    private String sku;
    private final ItemCategory category;
    private final HashMap<String, Batch> batches = new HashMap<>();
    private int totalQuantity;
    private BigDecimal totalCost = BigDecimal.ZERO;

    public InventoryItem(String displayName, String sku, ItemCategory category) {
        this.displayName = displayName;
        this.sku = sku;
        this.category = category;
    }

    public boolean hasBatch(String invoiceKey) { return batches.containsKey(invoiceKey); }

    /** Creates the correct batch subtype and updates aggregate totals. */
    public void addBatch(String invoiceKey, String invoiceNumber, int quantity,
            BigDecimal unitPrice, java.time.LocalDate expiryDate, String upc) {
        Batch batch = category == ItemCategory.PERISHABLE
                ? new PerishableBatch(invoiceNumber, quantity, unitPrice, expiryDate, upc)
                : new NonPerishableBatch(invoiceNumber, quantity, unitPrice, upc);
        batches.put(invoiceKey, batch);
        totalQuantity += batch.getQuantity();
        totalCost = totalCost.add(batch.getTotalCost());
    }

    /** Recalls the batch identified by its normalized invoice number. */
    public Batch recallBatch(String invoiceKey) {
        Batch batch = batches.remove(invoiceKey);
        totalQuantity -= batch.getQuantity();
        totalCost = totalCost.subtract(batch.getTotalCost());
        return batch;
    }

    /**
     * Removes the requested quantity according to the item's category and returns each consumed part.
     * Perishables are consumed by earliest expiry; non-perishables by invoice number.
     */
    public List<SoldBatch> sell(int quantity) {
        Comparator<Map.Entry<String, Batch>> saleOrder = category == ItemCategory.PERISHABLE
                ? Comparator.comparing((Map.Entry<String, Batch> entry) ->
                        ((PerishableBatch) entry.getValue()).getExpiryDate())
                        .thenComparing(entry -> TextNormalizer.normalize(
                                entry.getValue().getInvoiceNumber()))
                : Comparator.comparing(entry -> TextNormalizer.normalize(
                        entry.getValue().getInvoiceNumber()));
        List<Map.Entry<String, Batch>> orderedBatches = batches.entrySet().stream()
                .sorted(saleOrder)
                .toList();
        List<SoldBatch> soldBatches = new ArrayList<>();
        int remainingToSell = quantity;
        for (Map.Entry<String, Batch> entry : orderedBatches) {
            if (remainingToSell == 0) break;
            Batch batch = entry.getValue();
            int soldQuantity = Math.min(remainingToSell, batch.getQuantity());
            soldBatches.add(new SoldBatch(batch.getInvoiceNumber(), soldQuantity, batch.getUnitPrice()));
            totalQuantity -= soldQuantity;
            totalCost = totalCost.subtract(batch.getUnitPrice().multiply(
                    BigDecimal.valueOf(soldQuantity)));
            remainingToSell -= soldQuantity;
            if (soldQuantity == batch.getQuantity()) {
                batches.remove(entry.getKey());
            } else {
                batches.put(entry.getKey(), copyWithQuantity(batch,
                        batch.getQuantity() - soldQuantity));
            }
        }
        return List.copyOf(soldBatches);
    }

    /** Creates a replacement for a partially consumed immutable batch. */
    private static Batch copyWithQuantity(Batch batch, int quantity) {
        return batch instanceof PerishableBatch
                ? new PerishableBatch(batch.getInvoiceNumber(), quantity, batch.getUnitPrice(),
                        ((PerishableBatch) batch).getExpiryDate(), batch.getUpc())
                : new NonPerishableBatch(batch.getInvoiceNumber(), quantity, batch.getUnitPrice(),
                        batch.getUpc());
    }

    public String getDisplayName() { return displayName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public ItemCategory getCategory() { return category; }
    public Map<String, Batch> getBatches() { return batches; }
    public int getTotalQuantity() { return totalQuantity; }
    public BigDecimal getTotalCost() { return totalCost; }

    /** Creates a defensive copy including every batch and aggregate total. */
    public InventoryItem deepCopy() {
        InventoryItem copy = new InventoryItem(displayName, sku, category);
        for (Map.Entry<String, Batch> entry : batches.entrySet()) {
            Batch batch = entry.getValue();
            Batch batchCopy = batch instanceof PerishableBatch
                    ? new PerishableBatch(batch.getInvoiceNumber(), batch.getQuantity(),
                            batch.getUnitPrice(), ((PerishableBatch) batch).getExpiryDate(), batch.getUpc())
                    : new NonPerishableBatch(batch.getInvoiceNumber(), batch.getQuantity(),
                            batch.getUnitPrice(), batch.getUpc());
            copy.batches.put(entry.getKey(), batchCopy);
        }
        copy.totalQuantity = totalQuantity;
        copy.totalCost = totalCost;
        return copy;
    }
}
