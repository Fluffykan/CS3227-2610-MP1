package stockie.ui.javafx.CommandHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.storage.InventoryRepository;
import stockie.ui.javafx.util.InventoryRow;

class FxInventoryQueryHandlerTest {
    @Test
    void findByItemNameSelectsMappedRow() {
        StockieController controller = newController();
        controller.addBatch(request("Milk", "MILK", "INV-1", 2, null));
        FxInventoryQueryHandler handler = handler(controller);

        var result = handler.find("--item milk");

        assertEquals("Milk | SKU MILK | Qty 2\n", result.message());
        assertFalse(result.refreshRequired());
        assertNotNull(result.selectedRow());
        assertEquals("Milk", result.selectedRow().itemName());
    }

    @Test
    void listDepletedAndExpiredUseTheirDedicatedQueries() {
        StockieController controller = newController();
        controller.addBatch(request("Milk", "MILK", "INV-1", 1, null));
        controller.addBatch(request("Apple", "APPLE", "INV-2", 1, LocalDate.now().minusDays(1)));
        controller.sellItemByName("Milk", 1);
        FxInventoryQueryHandler handler = handler(controller);

        assertEquals("Milk | SKU MILK | Qty 0\n", handler.list("depleted").message());
        assertEquals("Apple | batches 1\n", handler.list("expired").message());
    }

    @Test
    void listExpiringInNegativeDaysReturnsValidationError() {
        var result = handler(newController()).list("expiring-in -1");

        assertEquals("Days must not be negative.\n", result.message());
        assertTrue(result.refreshRequired());
    }

    @Test
    void listExpiringInMalformedDaysReturnsValidationError() {
        var result = handler(newController()).list("expiring-in many");

        assertEquals("Expected a whole number for days.\n", result.message());
        assertTrue(result.refreshRequired());
    }

    @Test
    void emptyQueriesReturnNoMatchMessages() {
        FxInventoryQueryHandler handler = handler(newController());

        assertEquals("No matching items.\n", handler.list("").message());
        assertEquals("No matching batches.\n", handler.list("expired").message());
        assertNull(handler.find("--sku missing").selectedRow());
    }

    private static FxInventoryQueryHandler handler(StockieController controller) {
        return new FxInventoryQueryHandler(controller, item -> new InventoryRow(item.getDisplayName(),
                item.getSku(), item.getCategory().name(), item.getTotalQuantity(), item.getTotalCost(), List.of()));
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

    private static AddBatchRequest request(String name, String sku, String invoice, int quantity,
            LocalDate expiry) {
        return new AddBatchRequest(name, sku, invoice, quantity, BigDecimal.ONE, expiry, "UPC");
    }
}
