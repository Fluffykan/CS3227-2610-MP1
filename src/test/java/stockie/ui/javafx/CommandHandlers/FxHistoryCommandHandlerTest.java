package stockie.ui.javafx.CommandHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.storage.InventoryRepository;
import stockie.ui.javafx.command.FxCommandResult;

class FxHistoryCommandHandlerTest {
    @Test
    void undoAndRedoSuccessfulOperationsRequireRefresh() {
        StockieController controller = newController();
        controller.addBatch(new AddBatchRequest("Milk", "MILK", "INV-1", 1, BigDecimal.ONE, null, "UPC"));
        FxHistoryCommandHandler handler = new FxHistoryCommandHandler(controller);

        assertEquals("Undo applied.\n", handler.undo().message());
        assertTrue(handler.undo().message().contains("nothing to undo"));
        assertTrue(handler.redo().refreshRequired());
    }

    @Test
    void failedHistoryOperationDoesNotRequireRefresh() {
        FxCommandResult result = new FxHistoryCommandHandler(newController()).redo();

        assertEquals("nothing to redo\n", result.message());
        assertFalse(result.refreshRequired());
    }

    private static StockieController newController() {
        InventoryService inventory = new InventoryService();
        InventoryRepository repository = new InventoryRepository() {
            @Override
            public HashMap<String, InventoryItem> load() {
                return new HashMap<>();
            }

            @Override
            public void save(HashMap<String, InventoryItem> snapshot) { }
        };
        return new StockieController(inventory, new CommandManager(inventory, repository), repository);
    }
}
