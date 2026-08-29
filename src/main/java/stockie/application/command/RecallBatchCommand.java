package stockie.application.command;

import stockie.application.service.InventoryService;
import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;

/** Recalls a batch and snapshots the complete item for undo. */
public final class RecallBatchCommand implements InventoryCommand {
    private final InventoryService inventory;
    private final String itemKey;
    private final String invoiceKey;
    private InventoryItem previousItem;
    private boolean initialized;
    private boolean executed;

    /** Creates a command that recalls a batch from an item. */
    public RecallBatchCommand(InventoryService inventory, String itemKey, String invoiceKey) {
        this.inventory = inventory;
        this.itemKey = itemKey;
        this.invoiceKey = invoiceKey;
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
        inventory.recallBatch(itemKey, invoiceKey);
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

    public String getUndoAction() { return "added"; }
    public String getRedoAction() { return "recalled"; }
    public String getItemKey() { return itemKey; }
    public String getItemName() { return metadataItem().getDisplayName(); }
    public String getSku() { return metadataItem().getSku(); }
    public ItemCategory getCategory() { return metadataItem().getCategory(); }
    public Batch getAffectedBatch() { return metadataItem().getBatches().get(invoiceKey); }

    private InventoryItem metadataItem() {
        InventoryItem item = previousItem == null ? inventory.get(itemKey) : previousItem;
        if (item == null) {
            throw new IllegalStateException("Command item is unavailable");
        }
        return item;
    }
}
