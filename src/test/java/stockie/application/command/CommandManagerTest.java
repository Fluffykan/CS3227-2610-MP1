package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import stockie.application.service.InventoryService;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;
import stockie.storage.InventoryRepository;

class CommandManagerTest {
    @Test
    void executeUndoAndRedoPersistExpectedInventoryStates() throws IOException {
        InventoryService inventory = new InventoryService();
        RecordingRepository repository = new RecordingRepository();
        CommandManager manager = new CommandManager(inventory, repository);
        AddBatchCommand command = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));

        manager.execute(command);
        assertEquals(2, repository.saved.get("milk").getTotalQuantity());
        manager.undo();
        assertEquals(0, repository.saved.size());
        manager.redo();

        assertEquals(2, repository.saved.get("milk").getTotalQuantity());
    }

    @Test
    void undoWithoutHistoryThrowsException() {
        InventoryService inventory = new InventoryService();
        CommandManager manager = new CommandManager(inventory, new RecordingRepository());

        assertThrows(IllegalStateException.class, manager::undo);
        assertThrows(IllegalStateException.class, manager::redo);
    }

    @Test
    void executeSaveFailureRollsBackInventory() {
        InventoryService inventory = new InventoryService();
        CommandManager manager = new CommandManager(inventory, new FailingRepository());
        AddBatchCommand command = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));

        assertThrows(IOException.class, () -> manager.execute(command));
        assertEquals(0, inventory.size());
    }

    @Test
    void undoSaveFailureRestoresTheUndoneState() throws IOException {
        InventoryService inventory = new InventoryService();
        FailingOnNextSaveRepository repository = new FailingOnNextSaveRepository();
        CommandManager manager = new CommandManager(inventory, repository);
        AddBatchCommand command = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));

        manager.execute(command);
        repository.failNextSave = true;

        assertThrows(IOException.class, manager::undo);
        assertEquals(2, inventory.get("milk").getTotalQuantity());
    }

    @Test
    void redoSaveFailureRestoresTheUndoneState() throws IOException {
        InventoryService inventory = new InventoryService();
        FailingOnNextSaveRepository repository = new FailingOnNextSaveRepository();
        CommandManager manager = new CommandManager(inventory, repository);
        AddBatchCommand command = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));

        manager.execute(command);
        manager.undo();
        repository.failNextSave = true;

        assertThrows(IOException.class, manager::redo);
        assertEquals(0, inventory.size());
    }

    private static class RecordingRepository implements InventoryRepository {
        private HashMap<String, stockie.entities.InventoryItem> saved = new HashMap<>();

        @Override
        public HashMap<String, stockie.entities.InventoryItem> load() {
            return saved;
        }

        @Override
        public void save(HashMap<String, InventoryItem> snapshot) throws IOException {
            saved = snapshot;
        }
    }

    private static final class FailingRepository extends RecordingRepository {
        @Override
        public void save(HashMap<String, InventoryItem> snapshot) throws IOException {
            throw new IOException("save failed");
        }
    }

    private static final class FailingOnNextSaveRepository extends RecordingRepository {
        private boolean failNextSave;

        @Override
        public void save(HashMap<String, InventoryItem> snapshot) throws IOException {
            if (failNextSave) {
                failNextSave = false;
                throw new IOException("save failed");
            }
            super.save(snapshot);
        }
    }
}
