package stockie.ui.javafx;

import stockie.ui.javafx.command.CommandParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.storage.InventoryRepository;
import stockie.ui.javafx.util.InventoryRow;

class FxCliHandlerTest {
    @Test
    void parseCommand_extractsLowercaseCommandAndTrimmedArguments() {
        CommandParser.ParsedCommand parsed = CommandParser.parse("  HELP   --item Milk  ");

        assertEquals("help", parsed.command());
        assertEquals("--item Milk", parsed.arguments());
    }

    @Test
    void executeBye_closesApplicationAndReturnsGoodbyeMessage() {
        AtomicBoolean closed = new AtomicBoolean();
        FxCliHandler handler = new FxCliHandler(null, () -> closed.set(true), null);

        assertEquals("Goodbye.\n", handler.execute("bye").message());
        assertEquals(true, closed.get());
    }

    @Test
    void executeHelpAndUnknownCommand_returnExpectedMessages() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertTrue(handler.execute("HELP").message().contains("add -"));
        assertEquals("Unknown command. Type help for available commands.\n", handler.execute("wat").message());
    }

    @Test
    void executeEmptyInput_returnsUnknownCommandMessage() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertEquals("Unknown command. Type help for available commands.\n", handler.execute("   ").message());
    }

    @Test
    void executeRepeatedOption_returnsUsageMessage() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertTrue(handler.execute("add --item Milk --item Bread --sku MILK --invoice INV-1 "
                + "--quantity 3 --price 2").message().startsWith("Usage: add"));
    }

    @Test
    void executeMissingOptionValue_returnsUsageMessage() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertTrue(handler.execute("add --item --sku MILK --invoice INV-1 --quantity 3 --price 2")
                .message().startsWith("Usage: add"));
    }

    @Test
    void executeUnknownOption_returnsUsageMessage() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertTrue(handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 3 "
                + "--price 2 --unexpected value").message().startsWith("Usage: add"));
    }

    @Test
    void executeFindWithBothIdentifiers_returnsUsageMessage() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertEquals("Usage: find --item <name> | --sku <sku>\n",
                handler.execute("find --item Milk --sku MILK").message());
    }

    @Test
    void executeNegativeQuantity_returnsExplanation() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertEquals("Quantity must be greater than zero.\n",
                handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity -1 --price 2").message());
    }

    @Test
    void executeInvalidPrice_returnsExplanation() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertEquals("Expected a valid price.\n",
                handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 1 --price invalid").message());
    }

    @Test
    void executeInvalidDate_returnsExplanation() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertEquals("Expiry must use dd-MM-yyyy.\n",
                handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 1 --price 2 "
                        + "--expiry 2025-01-01").message());
    }

    @Test
    void executeAddFindAndSell_returnsExpectedResults() {
        StockieControllerTestDouble setup = new StockieControllerTestDouble();
        FxCliHandler handler = handler(setup.controller);

        assertEquals("Added batch for Milk\n",
                handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 3 --price 2").message());
        assertEquals("Milk | SKU MILK | Qty 3\n", handler.execute("find --sku milk").message());
        assertEquals("Sold 2 of Milk\n", handler.execute("sell --item milk --quantity 2").message());
        assertTrue(handler.execute("sell --item milk --quantity 1").refreshRequired());
    }

    @Test
    void executeAddInvalidQuantity_doesNotCallController() {
        StockieControllerTestDouble setup = new StockieControllerTestDouble();
        FxCliHandler handler = handler(setup.controller);

        assertEquals("Quantity must be greater than zero.\n",
                handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 0 --price 2").message());
        assertEquals(0, setup.controller.listItems(false).items().size());
    }

    @Test
    void failedAdd_doesNotRequireRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertFalse(handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 0 --price 2")
                .refreshRequired());
    }

    @Test
    void failedSell_doesNotRequireRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertFalse(handler.execute("sell --item Missing --quantity -1").refreshRequired());
    }

    @Test
    void failedRecall_doesNotRequireRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertFalse(handler.execute("recall --item Missing --invoice INV-1").refreshRequired());
    }

    @Test
    void failedRemove_doesNotRequireRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertFalse(handler.execute("remove --item Missing").refreshRequired());
    }

    @Test
    void failedUpdateSku_doesNotRequireRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertFalse(handler.execute("update-sku --item Missing --sku NEW-MILK").refreshRequired());
    }

    @Test
    void failedUndo_doesNotRequireRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertFalse(handler.execute("undo").refreshRequired());
    }

    @Test
    void failedRedo_doesNotRequireRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertFalse(handler.execute("redo").refreshRequired());
    }

    @Test
    void successfulAdd_requiresRefresh() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);

        assertTrue(handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 3 --price 2")
                .refreshRequired());
    }

    @Test
    void successfulSell_requiresRefresh() {
        FxCliHandler handler = handlerWithMilk();

        assertTrue(handler.execute("sell --item Milk --quantity 1").refreshRequired());
    }

    @Test
    void successfulRecall_requiresRefresh() {
        FxCliHandler handler = handlerWithMilk();

        assertTrue(handler.execute("recall --item Milk --invoice INV-1").refreshRequired());
    }

    @Test
    void successfulUpdateSku_requiresRefresh() {
        FxCliHandler handler = handlerWithMilk();

        assertTrue(handler.execute("update-sku --item Milk --sku NEW-MILK").refreshRequired());
    }

    @Test
    void successfulRemove_requiresRefresh() {
        FxCliHandler handler = handlerWithMilk();

        assertTrue(handler.execute("remove --sku MILK").refreshRequired());
    }

    @Test
    void successfulUndo_requiresRefresh() {
        FxCliHandler handler = handlerWithMilk();

        assertTrue(handler.execute("undo").refreshRequired());
    }

    @Test
    void successfulRedo_requiresRefresh() {
        FxCliHandler handler = handlerWithMilk();
        handler.execute("undo");

        assertTrue(handler.execute("redo").refreshRequired());
    }

    @Test
    void executeListExpiringInZero_includesBatchExpiringToday() {
        StockieControllerTestDouble setup = new StockieControllerTestDouble();
        setup.controller.addBatch(new stockie.application.request.AddBatchRequest("Milk", "MILK", "INV-1", 1,
                BigDecimal.ONE, LocalDate.now(), "123"));
        FxCliHandler handler = handler(setup.controller);

        assertEquals("Milk | batches 1\n", handler.execute("list expiring-in 0").message());
    }

    private static FxCliHandler handler(StockieController controller) {
        return new FxCliHandler(controller, () -> { },
                item -> new InventoryRow(item.getDisplayName(), item.getSku(), item.getCategory().name(),
                        item.getTotalQuantity(), item.getTotalCost(), List.of()));
    }

    private static FxCliHandler handlerWithMilk() {
        FxCliHandler handler = handler(new StockieControllerTestDouble().controller);
        handler.execute("add --item Milk --sku MILK --invoice INV-1 --quantity 3 --price 2");
        return handler;
    }

    private static final class StockieControllerTestDouble {
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
