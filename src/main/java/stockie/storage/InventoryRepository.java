package stockie.storage;

import stockie.model.InventoryItem;

import java.io.IOException;
import java.util.HashMap;

/** Abstraction for loading and saving complete inventory snapshots. */
public interface InventoryRepository {
    HashMap<String, InventoryItem> load() throws IOException, ClassNotFoundException;
    void save(HashMap<String, InventoryItem> snapshot) throws IOException;
}

