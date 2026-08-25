package stockie.application.command;

import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.application.service.InventoryService;

/** Adds a batch and snapshots the previous item for undo. */
public final class AddBatchCommand implements InventoryCommand {
    private final InventoryService inventory;
    private final String itemName;
    private final String itemKey;
    private final String sku;
    private final ItemCategory category;
    private final Batch batch;
    private InventoryItem previousItem;
    private boolean initialized;
    private boolean executed;

    public AddBatchCommand(InventoryService inventory, String itemName, String itemKey,
            String sku, ItemCategory category, Batch batch) {
        this.inventory = inventory;
        this.itemName = itemName;
        this.itemKey = itemKey;
        this.sku = sku;
        this.category = category;
        this.batch = batch;
    }

    @Override
    public void execute() {
        if (executed) return;
        if (!initialized) {
            previousItem = inventory.copyItem(itemKey);
            initialized = true;
        }
        inventory.addBatch(itemKey, itemName, sku, category, batch);
        executed = true;
    }

    @Override
    public void undo() {
        if (!executed) return;
        inventory.restoreItem(itemKey, previousItem);
        executed = false;
    }

    public String getUndoAction() { return "removed"; }
    public String getRedoAction() { return "added"; }
    public String getItemKey() { return itemKey; }
    public String getItemName() { return itemName; }
    public String getSku() { return sku; }
    public ItemCategory getCategory() { return category; }
    public Batch getAffectedBatch() { return batch; }
}
