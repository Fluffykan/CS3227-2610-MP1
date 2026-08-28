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
        assertEquals(new DashboardMetrics(2, 6, new BigDecimal("14.00"), 1), metrics.get(0));
        assertEquals("Showing all items", statuses.get(0));
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
