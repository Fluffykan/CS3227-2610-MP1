package stockie;

import java.nio.file.Path;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.service.InventoryService;
import stockie.storage.FileInventoryRepository;
import stockie.storage.InventoryRepository;
import stockie.ui.console.ConsoleUi;

/** Starts Stockie after assembling the application's shared dependencies. */
public final class Stockie {
    private Stockie() { }

    /** Starts the current console interface. A future JavaFX UI can reuse this wiring. */
    public static void main(String[] args) {
        InventoryRepository repository = new FileInventoryRepository(
                Path.of(System.getProperty("stockie.data.file", "stockie-inventory.dat")));
        InventoryService inventoryService = new InventoryService();
        CommandManager commandManager = new CommandManager(inventoryService, repository);
        StockieController controller = new StockieController(inventoryService, commandManager, repository);
        new ConsoleUi(controller).start();
    }
}
