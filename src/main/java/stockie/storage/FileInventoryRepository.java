package stockie.storage;

import stockie.model.InventoryItem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

/** Persists inventory snapshots using an atomic temporary-file replacement. */
public final class FileInventoryRepository implements InventoryRepository {
    private final Path path;

    public FileInventoryRepository(Path path) {
        this.path = path;
    }

    @Override
    public HashMap<String, InventoryItem> load() throws IOException, ClassNotFoundException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            return new HashMap<>();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            @SuppressWarnings("unchecked")
            HashMap<String, InventoryItem> snapshot = (HashMap<String, InventoryItem>) input.readObject();
            return snapshot;
        }
    }

    @Override
    public void save(HashMap<String, InventoryItem> snapshot) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
            output.writeObject(snapshot);
            output.flush();
        }
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }
}

