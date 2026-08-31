package stockie.ui.javafx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import stockie.application.controller.StockieController;
import stockie.application.result.ExpiringBatchQueryResult;
import stockie.application.result.ExpiringItem;
import stockie.application.result.ListQueryResult;
import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.PerishableBatch;
import stockie.ui.javafx.util.BatchRow;
import stockie.ui.javafx.util.DashboardMetrics;
import stockie.ui.javafx.util.InventoryRow;
import stockie.ui.javafx.view.ViewMode;

/** Coordinates dashboard queries and translates application results into view data. */
public final class StockieFxPresenter {
    private final StockieController controller;
    private final Consumer<List<InventoryRow>> rowsConsumer;
    private final Consumer<DashboardMetrics> metricsConsumer;
    private final Consumer<String> statusConsumer;
    private final Runnable clearDetails;

    /** Creates a presenter connected to controller queries and view callbacks. */
    public StockieFxPresenter(StockieController controller,
            Consumer<List<InventoryRow>> rowsConsumer,
            Consumer<DashboardMetrics> metricsConsumer,
            Consumer<String> statusConsumer,
            Runnable clearDetails) {
        this.controller = controller;
        this.rowsConsumer = rowsConsumer;
        this.metricsConsumer = metricsConsumer;
        this.statusConsumer = statusConsumer;
        this.clearDetails = clearDetails;
    }

    /** Refreshes dashboard metrics and rows for the selected filter. */
    public void refresh(ViewMode viewMode, int expiringInDays) {
        refreshMetrics(expiringInDays);
        switch (viewMode) {
        case ALL -> applyListResult(controller.listItems(false), "Showing all items");
        case DEPLETED -> applyListResult(controller.listItems(true), "Showing depleted items");
        case EXPIRED -> applyExpiringResult(controller.listExpiredBatches(), "Showing expired batches");
        case EXPIRING -> applyExpiringResult(controller.listExpiringBatches(expiringInDays),
                "Showing batches expiring in " + expiringInDays + " days");
        default -> throw new IllegalArgumentException("Unknown view mode: " + viewMode);
        }
    }

    private void refreshMetrics(int expiringInDays) {
        List<InventoryItem> items = controller.listItems(false).items();
        int totalQuantity = items.stream().mapToInt(InventoryItem::getTotalQuantity).sum();
        BigDecimal totalCost = items.stream().map(InventoryItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int expiringQuantity = controller.listExpiringBatches(expiringInDays).items().stream()
                .flatMap(item -> item.batches().stream())
                .mapToInt(PerishableBatch::getQuantity)
                .sum();
        metricsConsumer.accept(new DashboardMetrics(items.size(), totalQuantity, totalCost, expiringQuantity));
    }

    private void applyListResult(ListQueryResult result, String successStatus) {
        if (result.message() != null) {
            clearDetails.run();
            rowsConsumer.accept(List.<InventoryRow>of());
            statusConsumer.accept(result.message().trim());
            return;
        }
        rowsConsumer.accept(result.items().stream().map(this::toInventoryRow).toList());
        statusConsumer.accept(successStatus);
    }

    private void applyExpiringResult(ExpiringBatchQueryResult result, String successStatus) {
        if (result.message() != null) {
            clearDetails.run();
            rowsConsumer.accept(List.<InventoryRow>of());
            statusConsumer.accept(result.message().trim());
            return;
        }
        rowsConsumer.accept(result.items().stream().map(this::toInventoryRow).toList());
        statusConsumer.accept(successStatus);
    }

    private InventoryRow toInventoryRow(InventoryItem item) {
        List<Batch> orderedBatches = item.getBatches().values().stream()
                .sorted(Comparator.comparing(Batch::getInvoiceNumber, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new InventoryRow(item.getDisplayName(), item.getSku(),
                item.getCategory().name().toLowerCase(Locale.ROOT), item.getTotalQuantity(),
                item.getTotalCost(), orderedBatches.stream().map(this::toBatchRow).toList());
    }

    private InventoryRow toInventoryRow(ExpiringItem expiringItem) {
        InventoryItem item = expiringItem.item();
        List<BatchRow> rows = expiringItem.batches().stream().map(this::toBatchRow).toList();
        int quantity = rows.stream().mapToInt(BatchRow::quantity).sum();
        BigDecimal cost = rows.stream().map(BatchRow::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InventoryRow(item.getDisplayName(), item.getSku(),
                item.getCategory().name().toLowerCase(Locale.ROOT), quantity, cost, rows);
    }

    private BatchRow toBatchRow(Batch batch) {
        LocalDate expiry = batch instanceof PerishableBatch perishable
                ? perishable.getExpiryDate() : null;
        return new BatchRow(batch.getInvoiceNumber(), batch.getQuantity(), batch.getUnitPrice(),
                expiry, batch.getUpc());
    }
}
