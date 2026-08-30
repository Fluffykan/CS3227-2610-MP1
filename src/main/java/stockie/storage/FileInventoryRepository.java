package stockie.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import stockie.entities.InventoryItem;

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
        try (InputStream raw = Files.newInputStream(path);
            ObjectInputStream input = new ObjectInputStream(raw)) {
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
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

