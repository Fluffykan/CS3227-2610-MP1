package stockie.ui.javafx.CommandHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class FxInventoryCommandHandlerTest {
    @Test
    void addWithNegativePriceReturnsValidationErrorWithoutRefresh() {
        FxCommandResult result = handler(newController()).add(
                "--item Milk --sku MILK --invoice INV-1 --quantity 1 --price -1");

        assertEquals("Price must not be negative.\n", result.message());
        assertFalse(result.refreshRequired());
    }

    @Test
    void recallBySkuSuccessfullyRefreshes() {
        StockieController controller = newController();
        controller.addBatch(request("Milk", "MILK", "INV-1", 2));

        FxCommandResult result = handler(controller).recall("--sku milk --invoice inv-1");

        assertEquals("Batch recalled.\n", result.message());
        assertTrue(result.refreshRequired());
        assertEquals(0, controller.findBySku("milk").item().getTotalQuantity());
    }

    @Test
    void updateSkuByCurrentSkuSuccessfullyRefreshes() {
        StockieController controller = newController();
        controller.addBatch(request("Milk", "MILK", "INV-1", 2));

        FxCommandResult result = handler(controller).updateSku("--current-sku milk --sku NEW-MILK");

        assertEquals("SKU updated.\n", result.message());
        assertTrue(result.refreshRequired());
        assertNull(controller.findBySku("milk").item());
        assertEquals("Milk", controller.findBySku("new-milk").item().getDisplayName());
    }

    @Test
    void failedMutationReturnsMessageWithoutRefresh() {
        FxCommandResult result = handler(newController()).sell("--sku missing --quantity 1");

        assertEquals("item not found: missing\n", result.message());
        assertFalse(result.refreshRequired());
    }

    private static FxInventoryCommandHandler handler(StockieController controller) {
        return new FxInventoryCommandHandler(controller);
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

    private static AddBatchRequest request(String name, String sku, String invoice, int quantity) {
        return new AddBatchRequest(name, sku, invoice, quantity, BigDecimal.ONE, null, "UPC");
    }
}
