package stockie.application.command;

import java.util.List;
import stockie.application.service.InventoryService;
import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.ItemCategory;
import stockie.application.result.SoldBatch;

/** Sells quantities from an item while retaining a snapshot for undo and redo. */
public final class SellItemCommand implements InventoryCommand {
    private final InventoryService inventory;
    private final String itemKey;
    private final int quantity;
    private InventoryItem previousItem;
    private List<SoldBatch> soldBatches;
    private boolean initialized;
    private boolean executed;

    public SellItemCommand(InventoryService inventory, String itemKey, int quantity) {
        this.inventory = inventory;
        this.itemKey = itemKey;
        this.quantity = quantity;
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
        soldBatches = inventory.sell(itemKey, quantity);
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

    public List<SoldBatch> getSoldBatches() { return soldBatches; }
    public String getUndoAction() { return "restored sale for"; }
    public String getRedoAction() { return "sold"; }
    public String getItemKey() { return itemKey; }
    public String getItemName() { return previousItem.getDisplayName(); }
    public String getSku() { return previousItem.getSku(); }
    public ItemCategory getCategory() { return previousItem.getCategory(); }
    public Batch getAffectedBatch() { return null; }

    /** Returns the item's current state so history output reflects the completed operation. */
    @Override
    public InventoryItem getAffectedItem() { return inventory.copyItem(itemKey); }
}
