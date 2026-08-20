package stockie.command;

import stockie.model.Batch;
import stockie.model.ItemCategory;

/** Represents one reversible inventory mutation. */
public interface InventoryCommand {
    void execute();
    void undo();
    String getUndoAction();
    String getRedoAction();
    String getItemKey();
    String getItemName();
    String getSku();
    ItemCategory getCategory();
    Batch getAffectedBatch();
}

