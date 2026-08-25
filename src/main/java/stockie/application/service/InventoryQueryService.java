package stockie.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import stockie.application.result.ExpiringItem;
import stockie.entities.InventoryItem;
import stockie.entities.PerishableBatch;
import stockie.util.TextNormalizer;

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

    /** Returns perishable batches whose expiry dates fall within the inclusive query window. */
    public List<ExpiringItem> listExpiringIn(LocalDate today, int days) {
        LocalDate lastIncludedDate = today.plusDays(days);
        return listPerishableBatches(expiryDate -> !expiryDate.isBefore(today)
                && !expiryDate.isAfter(lastIncludedDate));
    }

    /** Returns perishable batches whose expiry dates are before the day of the query. */
    public List<ExpiringItem> listExpired(LocalDate today) {
        return listPerishableBatches(expiryDate -> expiryDate.isBefore(today));
    }

    /** Groups matching perishable batches by item and orders both groups and batches by expiry date. */
    private List<ExpiringItem> listPerishableBatches(Predicate<LocalDate> matches) {
        return inventory.values().stream()
                .map(item -> new ExpiringItem(item, item.getBatches().values().stream()
                        .filter(PerishableBatch.class::isInstance)
                        .map(PerishableBatch.class::cast)
                        .filter(batch -> matches.test(batch.getExpiryDate()))
                        .sorted(Comparator.comparing(PerishableBatch::getExpiryDate)
                                .thenComparing(batch -> TextNormalizer.normalize(
                                        batch.getInvoiceNumber())))
                        .toList()))
                .filter(result -> !result.batches().isEmpty())
                .sorted(Comparator.comparing((ExpiringItem result) ->
                        result.batches().get(0).getExpiryDate())
                        .thenComparing(result -> TextNormalizer.normalize(result.item().getSku()))
                        .thenComparing(result -> TextNormalizer.normalize(result.item().getDisplayName())))
                .toList();
    }
}

