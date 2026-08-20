package stockie.application;

import java.io.IOException;
import java.util.List;

import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.FindQueryResult;
import stockie.application.result.ListQueryResult;
import stockie.application.result.RecallBatchResult;
import stockie.command.AddBatchCommand;
import stockie.command.CommandManager;
import stockie.command.InventoryCommand;
import stockie.command.RecallBatchCommand;
import stockie.model.Batch;
import stockie.model.InventoryItem;
import stockie.model.ItemCategory;
import stockie.model.NonPerishableBatch;
import stockie.model.PerishableBatch;
import stockie.storage.InventoryRepository;
import stockie.util.TextNormalizer;

/** Provides UI-independent inventory operations for console and JavaFX clients. */
public final class StockieController {
    private static final int MAX_ITEMS = 200_000;
    private final InventoryService inventory;
    private final InventoryQueryService queries;
    private final CommandManager commandManager;
    private final InventoryRepository repository;

    public StockieController(InventoryService inventory, CommandManager commandManager,
            InventoryRepository repository) {
        this.inventory = inventory;
        this.queries = new InventoryQueryService(inventory);
        this.commandManager = commandManager;
        this.repository = repository;
    }

    /** Loads the most recently persisted inventory snapshot. */
    public void load() throws IOException, ClassNotFoundException { inventory.load(repository); }

    /** Adds a batch after enforcing inventory-wide uniqueness and consistency rules. */
    public AddBatchResult addBatch(AddBatchRequest request) {
        ItemCategory category = request.expiryDate() == null
                ? ItemCategory.NON_PERISHABLE : ItemCategory.PERISHABLE;
        String itemKey = TextNormalizer.normalize(request.itemName());
        String invoiceKey = TextNormalizer.normalize(request.invoiceNumber());
        InventoryItem item = inventory.get(itemKey);
        if (item == null && inventory.size() >= MAX_ITEMS) {
            return new AddBatchResult(null, " cannot track more than " + MAX_ITEMS + " items");
        }
        if (item != null && item.getCategory() != category) {
            return new AddBatchResult(null, " item category does not match existing item: "
                    + request.itemName());
        }
        if (item != null && !TextNormalizer.normalize(item.getSku())
                .equals(TextNormalizer.normalize(request.sku()))) {
            return new AddBatchResult(null, " sku does not match existing item: " + request.itemName());
        }
        if (item == null && inventory.getBySku(TextNormalizer.normalize(request.sku())) != null) {
            return new AddBatchResult(null, " sku already exists: " + request.sku());
        }
        if (item != null && item.hasBatch(invoiceKey)) {
            return new AddBatchResult(null, " invoice already exists: " + request.invoiceNumber());
        }
        Batch batch = category == ItemCategory.PERISHABLE
                ? new PerishableBatch(request.invoiceNumber(), request.quantity(), request.unitPrice(),
                        request.expiryDate(), request.upc())
                : new NonPerishableBatch(request.invoiceNumber(), request.quantity(), request.unitPrice(),
                        request.upc());
        try {
            commandManager.execute(new AddBatchCommand(inventory, request.itemName(), itemKey,
                    request.sku(), category, batch));
            return new AddBatchResult(inventory.get(itemKey), null);
        } catch (IOException exception) {
            return new AddBatchResult(null, " unable to save inventory; addition cancelled");
        }
    }

    /** Recalls a batch identified by its item name and invoice number. */
    public RecallBatchResult recallBatchByName(String itemName, String invoiceNumber) {
        String itemKey = TextNormalizer.normalize(itemName);
        return recallBatch(itemKey, itemName, invoiceNumber);
    }

    /** Recalls a batch identified by its SKU and invoice number. */
    public RecallBatchResult recallBatchBySku(String sku, String invoiceNumber) {
        InventoryItem item = inventory.getBySku(TextNormalizer.normalize(sku));
        if (item == null) {
            return new RecallBatchResult(null, " batch not found: " + sku + " / " + invoiceNumber);
        }
        return recallBatch(TextNormalizer.normalize(item.getDisplayName()), sku, invoiceNumber);
    }

    /** Performs a recall after the caller has selected an item by name or SKU. */
    private RecallBatchResult recallBatch(String itemKey, String identifier, String invoiceNumber) {
        String invoiceKey = TextNormalizer.normalize(invoiceNumber);
        InventoryItem item = inventory.get(itemKey);
        if (item == null || !item.hasBatch(invoiceKey)) {
            return new RecallBatchResult(null, " batch not found: " + identifier + " / " + invoiceNumber);
        }
        try {
            commandManager.execute(new RecallBatchCommand(inventory, itemKey, invoiceKey));
            return new RecallBatchResult(inventory.get(itemKey), null);
        } catch (IOException exception) {
            return new RecallBatchResult(null, " unable to save inventory; recall cancelled");
        }
    }

    /** Returns all items, optionally restricted to items with zero quantity. */
    public ListQueryResult listItems(boolean depletedOnly) {
        List<InventoryItem> items = queries.list(depletedOnly);
        return items.isEmpty() ? new ListQueryResult(items,
                depletedOnly ? " No depleted items in list" : " No items in list")
                : new ListQueryResult(items, null);
    }

    /** Looks up an item by display name. */
    public FindQueryResult findByName(String name) { return find(queries.findByName(name), "item", name); }

    /** Looks up an item by SKU. */
    public FindQueryResult findBySku(String sku) { return find(queries.findBySku(sku), "sku", sku); }

    /** Undoes the most recent persisted inventory change. */
    public CommandResult undo() { return changeHistory(true); }

    /** Reapplies the most recently undone persisted inventory change. */
    public CommandResult redo() { return changeHistory(false); }

    private FindQueryResult find(InventoryItem item, String field, String value) {
        return item == null ? new FindQueryResult(null, " no item found with " + field + ": " + value)
                : new FindQueryResult(item, null);
    }

    private CommandResult changeHistory(boolean undo) {
        try {
            InventoryCommand command = undo ? commandManager.undo() : commandManager.redo();
            return new CommandResult(command, null);
        } catch (IllegalStateException exception) {
            return new CommandResult(null, undo ? " nothing to undo" : " nothing to redo");
        } catch (IOException exception) {
            return new CommandResult(null, undo ? " unable to save inventory; undo cancelled"
                    : " unable to save inventory; redo cancelled");
        }
    }
}
