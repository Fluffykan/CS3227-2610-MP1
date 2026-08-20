package stockie.command;

import stockie.application.InventoryService;
import stockie.model.Batch;
import stockie.model.ItemCategory;

/** Changes an item's SKU and retains its former SKU for undo and redo. */
public final class UpdateSkuCommand implements InventoryCommand {
    private final InventoryService inventory;
    private final String itemKey;
    private final String newSku;
    private String oldSku;
    private boolean initialized;
    private boolean executed;

    public UpdateSkuCommand(InventoryService inventory, String itemKey, String newSku) {
        this.inventory = inventory;
        this.itemKey = itemKey;
        this.newSku = newSku;
    }

    @Override
    public void execute() {
        if (executed) return;
        if (!initialized) {
            oldSku = inventory.get(itemKey).getSku();
            initialized = true;
        }
        inventory.updateSku(itemKey, newSku);
        executed = true;
    }

    @Override
    public void undo() {
        if (!executed) return;
        inventory.updateSku(itemKey, oldSku);
        executed = false;
    }

    @Override
    public String getUndoAction() { return "restored sku"; }

    @Override
    public String getRedoAction() { return "updated sku"; }

    @Override
    public String getItemKey() { return itemKey; }

    @Override
    public String getItemName() { return inventory.get(itemKey).getDisplayName(); }

    @Override
    public String getSku() { return inventory.get(itemKey).getSku(); }

    @Override
    public ItemCategory getCategory() { return inventory.get(itemKey).getCategory(); }

    @Override
    public Batch getAffectedBatch() { return null; }

    /** Returns the SKU that was replaced by this command. */
    public String getOldSku() { return oldSku; }

    /** Returns the SKU applied by this command. */
    public String getNewSku() { return newSku; }
}
