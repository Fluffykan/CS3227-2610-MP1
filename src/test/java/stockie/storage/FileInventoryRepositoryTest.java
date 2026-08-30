package stockie.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.entities.NonPerishableBatch;

class FileInventoryRepositoryTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadMissingFileReturnsEmptyInventory() throws Exception {
        FileInventoryRepository repository = repositoryAt("inventory.dat");

        assertTrue(repository.load().isEmpty());
    }

    @Test
    void loadEmptyFileReturnsEmptyInventory() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");
        Files.createFile(path);

        assertTrue(new FileInventoryRepository(path).load().isEmpty());
    }

    @Test
    void saveAndLoadPreservesInventoryData() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");
        FileInventoryRepository repository = new FileInventoryRepository(path);
        HashMap<String, InventoryItem> inventory = inventoryWithItem("milk", "MILK", 3);

        repository.save(inventory);

        InventoryItem loadedItem = repository.load().get("milk");
        assertEquals("Milk", loadedItem.getDisplayName());
        assertEquals("MILK", loadedItem.getSku());
        assertEquals(ItemCategory.NON_PERISHABLE, loadedItem.getCategory());
        assertEquals(3, loadedItem.getTotalQuantity());
        assertEquals(new BigDecimal("6"), loadedItem.getTotalCost());
    }

    @Test
    void saveCreatesFileAtConfiguredPath() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");

        new FileInventoryRepository(path).save(new HashMap<>());

        assertTrue(Files.exists(path));
    }

    @Test
    void saveReplacesExistingFileWithLatestSnapshot() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");
        FileInventoryRepository repository = new FileInventoryRepository(path);
        repository.save(inventoryWithItem("milk", "MILK", 3));

        repository.save(inventoryWithItem("bread", "BREAD", 2));

        HashMap<String, InventoryItem> loaded = repository.load();
        assertFalse(loaded.containsKey("milk"));
        assertTrue(loaded.containsKey("bread"));
    }

    @Test
    void savePreservesMultipleItems() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");
        FileInventoryRepository repository = new FileInventoryRepository(path);
        HashMap<String, InventoryItem> inventory = inventoryWithItem("milk", "MILK", 3);
        inventory.putAll(inventoryWithItem("bread", "BREAD", 2));

        repository.save(inventory);

        assertEquals(2, repository.load().size());
    }

    @Test
    void loadInvalidSerializedDataThrowsIOException() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");
        Files.write(path, new byte[] {1, 2, 3, 4});

        assertThrows(IOException.class, () -> new FileInventoryRepository(path).load());
    }

    @Test
    void loadSerializedNonInventoryDataThrowsClassCastException() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
            output.writeObject("not an inventory map");
        }

        assertThrows(ClassCastException.class, () -> new FileInventoryRepository(path).load());
    }

    @Test
    void saveToUnavailableLocationThrowsIOExceptionWithoutLeavingTemporaryFile() {
        Path path = temporaryDirectory.resolve("missing-directory").resolve("inventory.dat");

        assertThrows(IOException.class, () -> new FileInventoryRepository(path).save(new HashMap<>()));
        assertFalse(Files.exists(path.resolveSibling("inventory.dat.tmp")));
    }

    @Test
    void saveDoesNotLeaveTemporaryFileAfterSuccessfulSave() throws Exception {
        Path path = temporaryDirectory.resolve("inventory.dat");
        new FileInventoryRepository(path).save(new HashMap<>());

        assertFalse(Files.exists(path.resolveSibling("inventory.dat.tmp")));
    }

    private FileInventoryRepository repositoryAt(String fileName) {
        return new FileInventoryRepository(temporaryDirectory.resolve(fileName));
    }

    private static HashMap<String, InventoryItem> inventoryWithItem(String key, String sku, int quantity) {
        InventoryItem item = new InventoryItem("Milk", sku, ItemCategory.NON_PERISHABLE);
        Batch batch = new NonPerishableBatch("INV-1", quantity, BigDecimal.valueOf(2), "UPC-1");
        item.addBatch("inv-1", batch.getInvoiceNumber(), batch.getQuantity(), batch.getUnitPrice(), null,
                batch.getUpc());
        HashMap<String, InventoryItem> inventory = new HashMap<>();
        inventory.put(key, item);
        return inventory;
    }
}
