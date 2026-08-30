package stockie.application.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import stockie.application.exception.ItemNotFoundException;
import stockie.application.result.SoldBatch;
import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.entities.PerishableBatch;
import stockie.storage.InventoryRepository;
import stockie.util.TextNormalizer;

/** Owns inventory mutations and creates defensive copies for command history. */
public final class InventoryService {
    private HashMap<String, InventoryItem> items = new HashMap<>();
    /** Secondary index from normalized SKU to the canonical item object. */
    private HashMap<String, InventoryItem> itemsBySku = new HashMap<>();

    /** Loads, validates, and indexes persisted inventory, skipping corrupted entries. */
    public List<String> load(InventoryRepository repository) throws IOException, ClassNotFoundException {
        HashMap<String, InventoryItem> loadedItems = repository.load();
        HashMap<String, InventoryItem> validItems = new HashMap<>();
        HashMap<String, InventoryItem> validSkuIndex = new HashMap<>();
        List<String> skippedItems = new ArrayList<>();

        if (loadedItems == null) {
            throw new IllegalStateException("Repository returned null inventory");
        }
        for (Map.Entry<String, InventoryItem> entry : loadedItems.entrySet()) {
            try {
                InventoryItem item = entry.getValue();
                validateLoadedItem(entry.getKey(), item);
                item = item.deepCopy();
                String normalizedSku = TextNormalizer.normalize(item.getSku());
                if (validSkuIndex.containsKey(normalizedSku)) {
                    skippedItems.add(entry.getKey());
                    continue;
                }
                validItems.put(entry.getKey(), item);
                validSkuIndex.put(normalizedSku, item);
            } catch (IllegalArgumentException exception) {
                skippedItems.add(entry.getKey());
            }
        }
        items = validItems;
        itemsBySku = validSkuIndex;
        return List.copyOf(skippedItems);
    }

    public InventoryItem get(String itemKey) { return items.get(itemKey); }
    /** Returns the item identified by a case-insensitive SKU, or {@code null} if absent. */
    public InventoryItem getBySku(String skuKey) {
        return itemsBySku.get(TextNormalizer.normalize(skuKey));
    }
    public java.util.Collection<InventoryItem> values() { return items.values(); }
    public int size() { return items.size(); }

    /** Adds a batch while maintaining item category and SKU invariants. */
    public void addBatch(String itemKey, String itemName, String sku,
            ItemCategory category, Batch batch) {
        validateBatchForAddition(batch);
        InventoryItem item = items.get(itemKey);
        if (item == null) {
            item = new InventoryItem(itemName, sku, category);
            items.put(itemKey, item);
            itemsBySku.put(TextNormalizer.normalize(sku), item);
        } else if (item.getCategory() != category) {
            throw new IllegalArgumentException("Item category does not match existing item");
        } else if (!TextNormalizer.normalize(item.getSku()).equals(TextNormalizer.normalize(sku))) {
            throw new IllegalArgumentException("SKU does not match existing item");
        }
        item.addBatch(TextNormalizer.normalize(batch.getInvoiceNumber()), batch.getInvoiceNumber(),
                batch.getQuantity(), batch.getUnitPrice(),
                batch instanceof PerishableBatch ? ((PerishableBatch) batch).getExpiryDate() : null,
                batch.getUpc());
    }

    /** Recalls a batch from the item identified by its normalized name. */
    public void recallBatch(String itemKey, String invoiceKey) {
        InventoryItem item = items.get(itemKey);
        if (item == null) {
            throw new ItemNotFoundException(itemKey);
        }
        item.recallBatch(invoiceKey);
    }

    /** Sells quantities from the specified item according to its category's batch ordering. */
    public List<SoldBatch> sell(String itemKey, int quantity) {
        InventoryItem item = items.get(itemKey);
        if (item == null) {
            throw new ItemNotFoundException(itemKey);
        }
        return item.sell(quantity);
    }

    /** Removes an entire item and its batches using its normalized item name. */
    public void removeItem(String itemKey) {
        InventoryItem item = items.remove(itemKey);
        if (item != null) {
            itemsBySku.remove(TextNormalizer.normalize(item.getSku()));
        }
    }

    /** Changes an item's SKU while keeping the secondary SKU index synchronized. */
    public void updateSku(String itemKey, String newSku) {
        if (newSku == null || newSku.isEmpty()) {
            throw new IllegalArgumentException("SKU must not be empty");
        }
        InventoryItem item = items.get(itemKey);
        if (item == null) {
            throw new ItemNotFoundException(itemKey);
        }
        InventoryItem itemWithNewSku = itemsBySku.get(TextNormalizer.normalize(newSku));
        if (itemWithNewSku != null && itemWithNewSku != item) {
            throw new IllegalArgumentException("SKU already exists: " + newSku);
        }
        itemsBySku.remove(TextNormalizer.normalize(item.getSku()));
        item.setSku(newSku);
        itemsBySku.put(TextNormalizer.normalize(newSku), item);
    }

    /** Returns a defensive copy of an item for command history. */
    public InventoryItem copyItem(String itemKey) {
        InventoryItem item = items.get(itemKey);
        return item == null ? null : item.deepCopy();
    }

    /** Restores an item snapshot and rebuilds its SKU index entry. */
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

    private static void validateLoadedItem(String itemKey, InventoryItem item) {
        if (itemKey == null || item == null || item.getDisplayName() == null
                || item.getDisplayName().isEmpty() || item.getSku() == null || item.getSku().isEmpty()
                || item.getCategory() == null) {
            throw new IllegalArgumentException("Invalid inventory item");
        }
        if (item.getBatches() == null) {
            throw new IllegalArgumentException("Invalid inventory batches");
        }
        for (Map.Entry<String, Batch> batchEntry : item.getBatches().entrySet()) {
            Batch batch = batchEntry.getValue();
            if (batchEntry.getKey() == null || batch == null || batch.getInvoiceNumber() == null
                    || batch.getInvoiceNumber().isEmpty()
                    || !TextNormalizer.normalize(batch.getInvoiceNumber()).equals(batchEntry.getKey())
                    || batch.getQuantity() <= 0 || batch.getUnitPrice() == null
                    || item.getCategory() == ItemCategory.PERISHABLE != (batch instanceof PerishableBatch)) {
                throw new IllegalArgumentException("Invalid inventory batch");
            }
        }
    }

    private static void validateBatchForAddition(Batch batch) {
        if (batch == null || batch.getQuantity() <= 0) {
            throw new IllegalArgumentException("Batch quantity must be positive");
        }
        if (batch.getInvoiceNumber() == null || batch.getInvoiceNumber().isEmpty()) {
            throw new IllegalArgumentException("Batch invoice must not be empty");
        }
        if (batch.getUnitPrice() == null) {
            throw new IllegalArgumentException("Batch price must not be null");
        }
    }
}

