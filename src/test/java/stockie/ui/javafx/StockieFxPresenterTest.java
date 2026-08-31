package stockie.ui.javafx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.service.InventoryService;
import stockie.storage.InventoryRepository;
import stockie.ui.javafx.util.DashboardMetrics;
import stockie.ui.javafx.util.InventoryRow;
import stockie.ui.javafx.view.ViewMode;

class StockieFxPresenterTest {
    @Test
    void refreshAll_mapsItemsAndCalculatesDashboardMetrics() {
        InventoryService inventory = new InventoryService();
        InventoryRepository repository = new EmptyRepository();
        StockieController controller = new StockieController(inventory,
                new CommandManager(inventory, repository), repository);
        controller.addBatch(new AddBatchRequest("Milk", "MILK", "INV-2", 2,
                new BigDecimal("3.00"), LocalDate.now(), "UPC"));
        controller.addBatch(new AddBatchRequest("Bread", "BREAD", "INV-1", 4,
                new BigDecimal("2.00"), null, null));
        List<InventoryRow> rows = new ArrayList<>();
        List<DashboardMetrics> metrics = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        StockieFxPresenter presenter = new StockieFxPresenter(controller, rows::addAll, metrics::add,
                statuses::add, () -> { });

        presenter.refresh(ViewMode.ALL, 0);

        assertEquals(2, rows.size());
        assertEquals("INV-1", rows.stream().filter(row -> row.itemName().equals("Bread"))
                .findFirst().orElseThrow().batches().get(0).invoice());
        assertEquals(new DashboardMetrics(2, 6, new BigDecimal("14.00"), 2), metrics.get(0));
        assertEquals("Showing all items", statuses.get(0));
    }

    @Test
    void refreshDepleted_showsOnlyItemsWithNoRemainingStock() {
        StockieController controller = controllerWithSampleData();
        List<InventoryRow> rows = new ArrayList<>();
        List<DashboardMetrics> metrics = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        StockieFxPresenter presenter = new StockieFxPresenter(controller, rows::addAll, metrics::add,
                statuses::add, () -> { });

        presenter.refresh(ViewMode.DEPLETED, 7);

        assertEquals(List.of("Milk"), rows.stream().map(InventoryRow::itemName).toList());
        assertEquals(0, rows.get(0).totalQuantity());
        assertEquals(new DashboardMetrics(3, 6, new BigDecimal("10.00"), 4), metrics.get(0));
        assertEquals("Showing depleted items", statuses.get(0));
    }

    @Test
    void refreshExpired_mapsOnlyExpiredBatchesAndTheirCost() {
        StockieController controller = controllerWithSampleData();
        List<InventoryRow> rows = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        StockieFxPresenter presenter = new StockieFxPresenter(controller, rows::addAll,
                metrics -> { }, statuses::add, () -> { });

        presenter.refresh(ViewMode.EXPIRED, 7);

        assertEquals(List.of("Bread"), rows.stream().map(InventoryRow::itemName).toList());
        assertEquals("INV-B", rows.get(0).batches().get(0).invoice());
        assertEquals(2, rows.get(0).totalQuantity());
        assertEquals(new BigDecimal("4.00"), rows.get(0).inventoryCost());
        assertEquals("Showing expired batches", statuses.get(0));
    }

    @Test
    void refreshExpiring_mapsOnlyBatchesWithinRequestedWindow() {
        StockieController controller = controllerWithSampleData();
        List<InventoryRow> rows = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        StockieFxPresenter presenter = new StockieFxPresenter(controller, rows::addAll,
                metrics -> { }, statuses::add, () -> { });

        presenter.refresh(ViewMode.EXPIRING, 7);

        assertEquals(List.of("Apple"), rows.stream().map(InventoryRow::itemName).toList());
        assertEquals("INV-A", rows.get(0).batches().get(0).invoice());
        assertEquals(4, rows.get(0).totalQuantity());
        assertEquals(new BigDecimal("6.00"), rows.get(0).inventoryCost());
        assertEquals("Showing batches expiring in 7 days", statuses.get(0));
    }

    @Test
    void refreshExpiringMetrics_sumsQuantitiesAcrossMatchingBatches() {
        InventoryService inventory = new InventoryService();
        InventoryRepository repository = new EmptyRepository();
        StockieController controller = new StockieController(inventory,
                new CommandManager(inventory, repository), repository);
        LocalDate today = LocalDate.now();
        controller.addBatch(new AddBatchRequest("Product A", "A", "INV-A", 20,
                new BigDecimal("1.00"), today.plusDays(3), null));
        controller.addBatch(new AddBatchRequest("Product B", "B", "INV-B", 2,
                new BigDecimal("1.00"), today.plusDays(5), null));
        List<DashboardMetrics> metrics = new ArrayList<>();
        StockieFxPresenter presenter = new StockieFxPresenter(controller, rows -> { }, metrics::add,
                status -> { }, () -> { });

        presenter.refresh(ViewMode.ALL, 7);

        assertEquals(22, metrics.get(0).expiringSoon());
    }

    @Test
    void refreshEmptyViewClearsRowsAndDetails() {
        InventoryService inventory = new InventoryService();
        InventoryRepository repository = new EmptyRepository();
        StockieController controller = new StockieController(inventory,
                new CommandManager(inventory, repository), repository);
        List<InventoryRow> rows = new ArrayList<>();
        List<DashboardMetrics> metrics = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        List<Boolean> cleared = new ArrayList<>();
        StockieFxPresenter presenter = new StockieFxPresenter(controller, rows::addAll, metrics::add,
                statuses::add, () -> cleared.add(true));

        presenter.refresh(ViewMode.ALL, 7);

        assertEquals(List.of(), rows);
        assertEquals(List.of(new DashboardMetrics(0, 0, BigDecimal.ZERO, 0)), metrics);
        assertEquals(List.of("No items in list"), statuses);
        assertEquals(List.of(true), cleared);
    }

    private static StockieController controllerWithSampleData() {
        InventoryService inventory = new InventoryService();
        InventoryRepository repository = new EmptyRepository();
        StockieController controller = new StockieController(inventory,
                new CommandManager(inventory, repository), repository);
        LocalDate today = LocalDate.now();
        controller.addBatch(new AddBatchRequest("Milk", "MILK", "INV-M", 2,
                new BigDecimal("2.00"), null, null));
        controller.addBatch(new AddBatchRequest("Bread", "BREAD", "INV-B", 2,
                new BigDecimal("2.00"), today.minusDays(1), null));
        controller.addBatch(new AddBatchRequest("Apple", "APPLE", "INV-A", 4,
                new BigDecimal("1.50"), today.plusDays(3), null));
        controller.sellItemByName("Milk", 2);
        return controller;
    }

    private static final class EmptyRepository implements InventoryRepository {
        @Override
        public java.util.HashMap<String, stockie.entities.InventoryItem> load() {
            return new java.util.HashMap<>();
        }

        @Override
        public void save(java.util.HashMap<String, stockie.entities.InventoryItem> snapshot) { }
    }
}
