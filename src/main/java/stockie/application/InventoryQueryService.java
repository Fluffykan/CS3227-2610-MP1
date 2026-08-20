package stockie.application;

import stockie.model.InventoryItem;
import stockie.util.TextNormalizer;

import java.util.Comparator;
import java.util.List;

/** Provides read-only inventory queries for the controller. */
public final class InventoryQueryService {
    private final InventoryService inventory;

    public InventoryQueryService(InventoryService inventory) {
        this.inventory = inventory;
    }

    public InventoryItem findByName(String name) {
        return inventory.get(TextNormalizer.normalize(name));
    }

    public InventoryItem findBySku(String sku) {
        return inventory.getBySku(TextNormalizer.normalize(sku));
    }

    /** Returns items in SKU order, optionally retaining only depleted items. */
    public List<InventoryItem> list(boolean depleted) {
        return inventory.values().stream()
                .filter(item -> !depleted || item.getTotalQuantity() == 0)
                .sorted(Comparator.comparing((InventoryItem item) -> TextNormalizer.normalize(item.getSku()))
                        .thenComparing(item -> TextNormalizer.normalize(item.getDisplayName())))
                .toList();
    }
}

