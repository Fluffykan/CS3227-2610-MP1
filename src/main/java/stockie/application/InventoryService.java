package stockie.application;

import stockie.model.Batch;
import stockie.model.InventoryItem;
import stockie.model.ItemCategory;
import stockie.model.PerishableBatch;
import stockie.storage.InventoryRepository;
import stockie.util.TextNormalizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Owns inventory mutations and creates defensive copies for command history. */
public final class InventoryService {
    private HashMap<String, InventoryItem> items = new HashMap<>();
    /** Secondary index from normalized SKU to the canonical item object. */
    private HashMap<String, InventoryItem> itemsBySku = new HashMap<>();

    public void load(InventoryRepository repository) throws IOException, ClassNotFoundException {
        items = deepCopy(repository.load());
        rebuildSkuIndex();
    }

    public InventoryItem get(String itemKey) { return items.get(itemKey); }
    public InventoryItem getBySku(String skuKey) { return itemsBySku.get(skuKey); }
    public java.util.Collection<InventoryItem> values() { return items.values(); }
    public int size() { return items.size(); }

    public void addBatch(String itemKey, String itemName, String sku,
            ItemCategory category, Batch batch) {
        InventoryItem item = items.get(itemKey);
        if (item == null) {
            item = new InventoryItem(itemName, sku, category);
            items.put(itemKey, item);
            itemsBySku.put(TextNormalizer.normalize(sku), item);
        }
        item.addBatch(TextNormalizer.normalize(batch.getInvoiceNumber()), batch.getInvoiceNumber(),
                batch.getQuantity(), batch.getUnitPrice(),
                batch instanceof PerishableBatch ? ((PerishableBatch) batch).getExpiryDate() : null,
                batch.getUpc());
    }

    /** Recalls a batch from the item identified by its normalized name. */
    public void recallBatch(String itemKey, String invoiceKey) {
        InventoryItem item = items.get(itemKey);
        item.recallBatch(invoiceKey);
    }

    public InventoryItem copyItem(String itemKey) {
        InventoryItem item = items.get(itemKey);
        return item == null ? null : item.deepCopy();
    }

    public void restoreItem(String itemKey, InventoryItem item) {
        InventoryItem current = items.remove(itemKey);
        if (current != null) {
            itemsBySku.remove(TextNormalizer.normalize(current.getSku()));
        }
        if (item == null) {
            return;
        }
        InventoryItem restored = item.deepCopy();
        items.put(itemKey, restored);
        itemsBySku.put(TextNormalizer.normalize(restored.getSku()), restored);
    }

    public HashMap<String, InventoryItem> snapshot() { return deepCopy(items); }

    private static HashMap<String, InventoryItem> deepCopy(Map<String, InventoryItem> source) {
        HashMap<String, InventoryItem> copy = new HashMap<>();
        for (Map.Entry<String, InventoryItem> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return copy;
    }

    /** Rebuilds the transient SKU index after loading persisted inventory data. */
    private void rebuildSkuIndex() {
        itemsBySku.clear();
        for (InventoryItem item : items.values()) {
            itemsBySku.put(TextNormalizer.normalize(item.getSku()), item);
        }
    }
}

