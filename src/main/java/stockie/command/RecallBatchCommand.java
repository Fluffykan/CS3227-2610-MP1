package stockie.command;

import stockie.model.Batch;
import stockie.model.InventoryItem;
import stockie.model.ItemCategory;
import stockie.application.InventoryService;

/** Recalls a batch and snapshots the complete item for undo. */
public final class RecallBatchCommand implements InventoryCommand {
    private final InventoryService inventory;
    private final String itemKey;
    private final String invoiceKey;
    private InventoryItem previousItem;
    private boolean initialized;
    private boolean executed;

    public RecallBatchCommand(InventoryService inventory, String itemKey, String invoiceKey) {
        this.inventory = inventory;
        this.itemKey = itemKey;
        this.invoiceKey = invoiceKey;
    }

    @Override
    public void execute() {
        if (executed) return;
        if (!initialized) {
            previousItem = inventory.copyItem(itemKey);
            initialized = true;
        }
        inventory.recallBatch(itemKey, invoiceKey);
        executed = true;
    }

    @Override
    public void undo() {
        if (!executed) return;
        inventory.restoreItem(itemKey, previousItem);
        executed = false;
    }

    public String getUndoAction() { return "added"; }
    public String getRedoAction() { return "recalled"; }
    public String getItemKey() { return itemKey; }
    public String getItemName() { return previousItem.getDisplayName(); }
    public String getSku() { return previousItem.getSku(); }
    public ItemCategory getCategory() { return previousItem.getCategory(); }
    public Batch getAffectedBatch() { return previousItem.getBatches().get(invoiceKey); }
}
