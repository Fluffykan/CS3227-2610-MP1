package stockie.application.command;

import stockie.application.service.InventoryService;
import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;

/** Removes an entire item and snapshots it so the operation can be undone. */
public final class RemoveItemCommand implements InventoryCommand {
    private final InventoryService inventory;
    private final String itemKey;
    private InventoryItem previousItem;
    private boolean initialized;
    private boolean executed;

    /** Creates a command that removes an item from inventory. */
    public RemoveItemCommand(InventoryService inventory, String itemKey) {
        this.inventory = inventory;
        this.itemKey = itemKey;
    }

    @Override
    public void execute() {
        if (executed) {
            return;
        }
        if (!initialized) {
            previousItem = inventory.copyItem(itemKey);
            initialized = true;
        }
        inventory.removeItem(itemKey);
        executed = true;
    }

    @Override
    public void undo() {
        if (!executed) {
            return;
        }
        inventory.restoreItem(itemKey, previousItem);
        executed = false;
    }

    @Override
    public String getUndoAction() { return "restored"; }

    @Override
    public String getRedoAction() { return "removed"; }

    @Override
    public String getItemKey() { return itemKey; }

    @Override
    public String getItemName() { return previousItem.getDisplayName(); }

    @Override
    public String getSku() { return previousItem.getSku(); }

    @Override
    public ItemCategory getCategory() { return previousItem.getCategory(); }

    @Override
    public Batch getAffectedBatch() { return null; }

    @Override
    public InventoryItem getAffectedItem() { return previousItem.deepCopy(); }
}
