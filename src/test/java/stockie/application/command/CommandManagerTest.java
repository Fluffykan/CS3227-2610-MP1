package stockie.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import stockie.application.exception.ItemNotFoundException;
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
    void executingNewCommandClearsRedoHistory() throws IOException {
        InventoryService inventory = new InventoryService();
        RecordingRepository repository = new RecordingRepository();
        CommandManager manager = new CommandManager(inventory, repository);
        AddBatchCommand milkCommand = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));
        AddBatchCommand breadCommand = new AddBatchCommand(inventory, "Bread", "bread", "BREAD",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-2", 1, BigDecimal.ONE, "456"));

        manager.execute(milkCommand);
        manager.undo();
        manager.execute(breadCommand);

        assertThrows(IllegalStateException.class, manager::redo);
        assertEquals(1, inventory.get("bread").getTotalQuantity());
        assertEquals(1, inventory.size());
    }

    @Test
    void undoAndRedoUseLastInFirstOutOrder() throws IOException {
        InventoryService inventory = new InventoryService();
        CommandManager manager = new CommandManager(inventory, new RecordingRepository());
        AddBatchCommand milkCommand = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 1, BigDecimal.TEN, "123"));
        AddBatchCommand breadCommand = new AddBatchCommand(inventory, "Bread", "bread", "BREAD",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-2", 1, BigDecimal.ONE, "456"));
        AddBatchCommand teaCommand = new AddBatchCommand(inventory, "Tea", "tea", "TEA",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-3", 1, BigDecimal.ONE, "789"));

        manager.execute(milkCommand);
        manager.execute(breadCommand);
        manager.execute(teaCommand);

        assertEquals("tea", manager.undo().getItemKey());
        assertEquals("bread", manager.undo().getItemKey());
        assertEquals("milk", manager.undo().getItemKey());
        assertEquals(0, inventory.size());

        assertEquals("milk", manager.redo().getItemKey());
        assertEquals("bread", manager.redo().getItemKey());
        assertEquals("tea", manager.redo().getItemKey());
        assertEquals(3, inventory.size());
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
    void executeRecallSaveFailureRollsBackRecall() throws IOException {
        InventoryService inventory = inventoryWithItem();
        FailingOnNextSaveRepository repository = repositoryWithInitialSave(inventory);
        CommandManager manager = new CommandManager(inventory, repository);
        RecallBatchCommand command = new RecallBatchCommand(inventory, "milk", "inv-1");
        repository.failNextSave = true;

        assertThrows(IOException.class, () -> manager.execute(command));
        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals("MILK", inventory.getBySku("milk").getSku());
    }

    @Test
    void executeRemoveSaveFailureRollsBackRemoval() throws IOException {
        InventoryService inventory = inventoryWithItem();
        FailingOnNextSaveRepository repository = repositoryWithInitialSave(inventory);
        CommandManager manager = new CommandManager(inventory, repository);
        RemoveItemCommand command = new RemoveItemCommand(inventory, "milk");
        repository.failNextSave = true;

        assertThrows(IOException.class, () -> manager.execute(command));
        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals("MILK", inventory.getBySku("milk").getSku());
    }

    @Test
    void executeSellSaveFailureRollsBackSale() throws IOException {
        InventoryService inventory = inventoryWithItem();
        FailingOnNextSaveRepository repository = repositoryWithInitialSave(inventory);
        CommandManager manager = new CommandManager(inventory, repository);
        SellItemCommand command = new SellItemCommand(inventory, "milk", 2);
        repository.failNextSave = true;

        assertThrows(IOException.class, () -> manager.execute(command));
        assertEquals(3, inventory.get("milk").getTotalQuantity());
        assertEquals(3, inventory.get("milk").getBatches().get("inv-1").getQuantity());
    }

    @Test
    void executeUpdateSkuSaveFailureRollsBackSkuUpdate() throws IOException {
        InventoryService inventory = inventoryWithItem();
        FailingOnNextSaveRepository repository = repositoryWithInitialSave(inventory);
        CommandManager manager = new CommandManager(inventory, repository);
        UpdateSkuCommand command = new UpdateSkuCommand(inventory, "milk", "NEW-MILK");
        repository.failNextSave = true;

        assertThrows(IOException.class, () -> manager.execute(command));
        assertEquals("MILK", inventory.get("milk").getSku());
        assertEquals("MILK", inventory.getBySku("MILK").getSku());
        assertNull(inventory.getBySku("NEW-MILK"));
    }

    @Test
    void failedCommandIsNotAddedToUndoHistory() throws IOException {
        InventoryService inventory = new InventoryService();
        CommandManager manager = new CommandManager(inventory, new RecordingRepository());
        AddBatchCommand addCommand = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));
        RecallBatchCommand failedCommand = new RecallBatchCommand(inventory, "milk", "missing-invoice");

        manager.execute(addCommand);

        assertThrows(IllegalArgumentException.class, () -> manager.execute(failedCommand));
        assertEquals("milk", manager.undo().getItemKey());
        assertEquals(0, inventory.size());
        assertThrows(IllegalStateException.class, manager::undo);
    }

    @Test
    void mixedCommandSequenceCanBeUndoneAndRedoneInOrder() throws IOException {
        InventoryService inventory = new InventoryService();
        CommandManager manager = new CommandManager(inventory, new RecordingRepository());
        AddBatchCommand add = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 2, BigDecimal.TEN, "123"));
        SellItemCommand sell = new SellItemCommand(inventory, "milk", 1);
        RecallBatchCommand recall = new RecallBatchCommand(inventory, "milk", "inv-1");
        UpdateSkuCommand updateSku = new UpdateSkuCommand(inventory, "milk", "NEW-MILK");
        RemoveItemCommand remove = new RemoveItemCommand(inventory, "milk");

        manager.execute(add);
        manager.execute(sell);
        manager.execute(recall);
        manager.execute(updateSku);
        manager.execute(remove);
        assertEquals(0, inventory.size());

        assertEquals("milk", manager.undo().getItemKey());
        assertEquals("NEW-MILK", inventory.get("milk").getSku());
        assertEquals("milk", manager.undo().getItemKey());
        assertEquals("MILK", inventory.get("milk").getSku());
        assertEquals("milk", manager.undo().getItemKey());
        assertEquals(1, inventory.get("milk").getTotalQuantity());
        assertEquals("milk", manager.undo().getItemKey());
        assertEquals(2, inventory.get("milk").getTotalQuantity());
        assertEquals("milk", manager.undo().getItemKey());
        assertEquals(0, inventory.size());

        assertThrows(IllegalStateException.class, manager::undo);

        assertEquals("milk", manager.redo().getItemKey());
        assertEquals(2, inventory.get("milk").getTotalQuantity());
        assertEquals("milk", manager.redo().getItemKey());
        assertEquals(1, inventory.get("milk").getTotalQuantity());
        assertEquals("milk", manager.redo().getItemKey());
        assertEquals(0, inventory.get("milk").getTotalQuantity());
        assertEquals("milk", manager.redo().getItemKey());
        assertEquals("NEW-MILK", inventory.get("milk").getSku());
        assertEquals("milk", manager.redo().getItemKey());
        assertEquals(0, inventory.size());

        assertThrows(IllegalStateException.class, manager::redo);
    }

    @Test
    void skuIndexTracksUndoRedoAndCaseInsensitiveLookup() throws IOException {
        InventoryService inventory = new InventoryService();
        CommandManager manager = new CommandManager(inventory, new RecordingRepository());
        AddBatchCommand add = new AddBatchCommand(inventory, "Milk", "milk", "MILK",
                ItemCategory.NON_PERISHABLE, new NonPerishableBatch("INV-1", 1, BigDecimal.TEN, "123"));
        UpdateSkuCommand updateSku = new UpdateSkuCommand(inventory, "milk", "NEW-MILK");

        manager.execute(add);
        manager.execute(updateSku);
        assertNull(inventory.getBySku("milk"));
        assertEquals("NEW-MILK", inventory.getBySku("new-milk").getSku());
        assertEquals("NEW-MILK", inventory.getBySku("NEW-MILK").getSku());

        manager.undo();
        assertEquals("MILK", inventory.getBySku("milk").getSku());
        assertNull(inventory.getBySku("new-milk"));

        manager.redo();
        assertNull(inventory.getBySku("MILK"));
        assertEquals("NEW-MILK", inventory.getBySku("new-milk").getSku());
        assertEquals("NEW-MILK", inventory.getBySku("New-Milk").getSku());
    }

    @Test
    void failedCommandLeavesBothHistoryStacksEmpty() throws IOException {
        InventoryService inventory = new InventoryService();
        CommandManager manager = new CommandManager(inventory, new RecordingRepository());
        RecallBatchCommand command = new RecallBatchCommand(inventory, "missing-item", "inv-1");

        assertThrows(ItemNotFoundException.class, () -> manager.execute(command));
        assertThrows(IllegalStateException.class, manager::undo);
        assertThrows(IllegalStateException.class, manager::redo);
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

    private static InventoryService inventoryWithItem() {
        InventoryService inventory = new InventoryService();
        inventory.addBatch("milk", "Milk", "MILK", ItemCategory.NON_PERISHABLE,
                new NonPerishableBatch("INV-1", 3, BigDecimal.TEN, "123"));
        return inventory;
    }

    private static FailingOnNextSaveRepository repositoryWithInitialSave(InventoryService inventory)
            throws IOException {
        FailingOnNextSaveRepository repository = new FailingOnNextSaveRepository();
        repository.save(inventory.snapshot());
        return repository;
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
