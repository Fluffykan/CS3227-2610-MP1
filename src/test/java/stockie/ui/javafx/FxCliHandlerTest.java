package stockie.ui.javafx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.storage.InventoryRepository;
import stockie.ui.javafx.util.InventoryRow;

class FxCliHandlerTest {
    @Test
    void executeBye_closesApplicationAndReturnsGoodbyeMessage() {
        AtomicBoolean closed = new AtomicBoolean();
        FxCliHandler handler = new FxCliHandler(null, null, null, null,
                () -> closed.set(true), null);

        assertEquals("Goodbye.\n", handler.execute("bye"));
        assertEquals(true, closed.get());
    }

    @Test
    void executeHelpAndUnknownCommand_returnExpectedMessages() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller, () -> { });

        assertTrue(handler.execute("HELP").startsWith("add, recall"));
        assertEquals("Unknown command. Type help for available commands.\n", handler.execute("wat"));
    }

    @Test
    void executeAddFindAndSell_updatesInventoryAndCallbacks() {
        StockieControllerTestDouble setup = new StockieControllerTestDouble();
        FxCliHandler handler = handler(setup.controller, () -> setup.refreshes.incrementAndGet());

        assertEquals("Added batch for Milk\n",
                handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 3 --price 2"));
        assertEquals("Milk | SKU MILK | Qty 3\n", handler.execute("find --sku milk"));
        assertEquals("Sold 2 of Milk\n", handler.execute("sell --item milk --quantity 2"));
        assertEquals(2, setup.refreshes.get());
    }

    @Test
    void executeAddInvalidQuantity_doesNotCallController() {
        StockieControllerTestDouble setup = new StockieControllerTestDouble();
        FxCliHandler handler = handler(setup.controller, () -> { });

        assertEquals("", handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 0 --price 2"));
        assertEquals(0, setup.controller.listItems(false).items().size());
    }

    @Test
    void executeListExpiringInZero_includesBatchExpiringToday() {
        StockieControllerTestDouble setup = new StockieControllerTestDouble();
        setup.controller.addBatch(new stockie.application.request.AddBatchRequest("Milk", "MILK", "INV-1", 1,
                BigDecimal.ONE, LocalDate.now(), "123"));
        FxCliHandler handler = handler(setup.controller, () -> { });

        assertEquals("Milk | batches 1\n", handler.execute("list expiring-in 0"));
    }

    private static FxCliHandler handler(StockieController controller, Runnable refresh) {
        return new FxCliHandler(controller, rows -> { }, row -> { }, refresh, () -> { },
                item -> new InventoryRow(item.getDisplayName(), item.getSku(), item.getCategory().name(),
                        item.getTotalQuantity(), item.getTotalCost(), List.of()));
    }

    private static final class StockieControllerTestDouble {
        private final AtomicInteger refreshes = new AtomicInteger();
        private final StockieController controller;

        private StockieControllerTestDouble() {
            InventoryService inventory = new InventoryService();
            InventoryRepository repository = new InventoryRepository() {
                @Override
                public HashMap<String, InventoryItem> load() { return new HashMap<>(); }

                @Override
                public void save(HashMap<String, InventoryItem> snapshot) { }
            };
            controller = new StockieController(inventory, new CommandManager(inventory, repository), repository);
        }
    }
}
