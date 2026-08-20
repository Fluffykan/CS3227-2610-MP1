package stockie.application;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.ExpiringItem;
import stockie.application.result.ExpiringBatchQueryResult;
import stockie.application.result.FindQueryResult;
import stockie.application.result.ListQueryResult;
import stockie.application.result.RecallBatchResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.SellItemResult;
import stockie.application.result.SoldBatch;
import stockie.application.result.UpdateSkuResult;
import stockie.command.AddBatchCommand;
import stockie.command.CommandManager;
import stockie.command.InventoryCommand;
import stockie.command.RecallBatchCommand;
import stockie.command.RemoveItemCommand;
import stockie.command.SellItemCommand;
import stockie.command.UpdateSkuCommand;
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

    /** Removes an entire item identified by its display name. */
    public RemoveItemResult removeItemByName(String itemName) {
        String itemKey = TextNormalizer.normalize(itemName);
        return removeItem(itemKey, itemName);
    }

    /** Removes an entire item identified by its SKU. */
    public RemoveItemResult removeItemBySku(String sku) {
        InventoryItem item = inventory.getBySku(TextNormalizer.normalize(sku));
        if (item == null) {
            return new RemoveItemResult(null, " item not found: " + sku);
        }
        return removeItem(TextNormalizer.normalize(item.getDisplayName()), sku);
    }

    /** Executes the reversible removal for an item already selected by name or SKU. */
    private RemoveItemResult removeItem(String itemKey, String identifier) {
        InventoryItem item = inventory.get(itemKey);
        if (item == null) {
            return new RemoveItemResult(null, " item not found: " + identifier);
        }
        RemoveItemCommand command = new RemoveItemCommand(inventory, itemKey);
        try {
            commandManager.execute(command);
            return new RemoveItemResult(command.getAffectedItem(), null);
        } catch (IOException exception) {
            return new RemoveItemResult(null, " unable to save inventory; item removal cancelled");
        }
    }

    /** Updates an item's SKU after locating it by display name. */
    public UpdateSkuResult updateSkuByName(String itemName, String newSku) {
        return updateSku(TextNormalizer.normalize(itemName), itemName, newSku);
    }

    /** Updates an item's SKU after locating it by its current SKU. */
    public UpdateSkuResult updateSkuByCurrentSku(String currentSku, String newSku) {
        InventoryItem item = inventory.getBySku(TextNormalizer.normalize(currentSku));
        if (item == null) {
            return new UpdateSkuResult(null, null, " item not found: " + currentSku);
        }
        return updateSku(TextNormalizer.normalize(item.getDisplayName()), currentSku, newSku);
    }

    /** Validates and executes a reversible SKU change for an already selected item. */
    private UpdateSkuResult updateSku(String itemKey, String identifier, String newSku) {
        InventoryItem item = inventory.get(itemKey);
        if (item == null) {
            return new UpdateSkuResult(null, null, " item not found: " + identifier);
        }
        String oldSku = item.getSku();
        if (TextNormalizer.normalize(oldSku).equals(TextNormalizer.normalize(newSku))) {
            return new UpdateSkuResult(null, null, " new sku is the same as the current sku");
        }
        if (inventory.getBySku(TextNormalizer.normalize(newSku)) != null) {
            return new UpdateSkuResult(null, null, " sku already exists: " + newSku);
        }
        try {
            commandManager.execute(new UpdateSkuCommand(inventory, itemKey, newSku));
            return new UpdateSkuResult(inventory.get(itemKey), oldSku, null);
        } catch (IOException exception) {
            return new UpdateSkuResult(null, null, " unable to save inventory; sku update cancelled");
        }
    }

    /** Returns all items, optionally restricted to items with zero quantity. */
    public ListQueryResult listItems(boolean depletedOnly) {
        List<InventoryItem> items = queries.list(depletedOnly);
        return items.isEmpty() ? new ListQueryResult(items,
                depletedOnly ? " No depleted items in list" : " No items in list")
                : new ListQueryResult(items, null);
    }

    /** Returns perishable batches that expire from today through the requested number of days. */
    public ExpiringBatchQueryResult listExpiringBatches(int days) {
        List<ExpiringItem> items = queries.listExpiringIn(LocalDate.now(), days);
        return items.isEmpty() ? new ExpiringBatchQueryResult(items,
                " No batches expiring in " + days + " days")
                : new ExpiringBatchQueryResult(items, null);
    }

    /** Returns perishable batches that expired before today. */
    public ExpiringBatchQueryResult listExpiredBatches() {
        List<ExpiringItem> items = queries.listExpired(LocalDate.now());
        return items.isEmpty() ? new ExpiringBatchQueryResult(items, " No expired batches in list")
                : new ExpiringBatchQueryResult(items, null);
    }

    /** Sells stock after locating an item by its display name. */
    public SellItemResult sellItemByName(String itemName, int quantity) {
        return sellItem(TextNormalizer.normalize(itemName), itemName, quantity);
    }

    /** Sells stock after locating an item by its SKU. */
    public SellItemResult sellItemBySku(String sku, int quantity) {
        InventoryItem item = inventory.getBySku(TextNormalizer.normalize(sku));
        if (item == null) return new SellItemResult(null, List.of(), " item not found: " + sku);
        return sellItem(TextNormalizer.normalize(item.getDisplayName()), sku, quantity);
    }

    /** Validates stock availability, then executes a reversible sale. */
    private SellItemResult sellItem(String itemKey, String identifier, int quantity) {
        InventoryItem item = inventory.get(itemKey);
        if (item == null) return new SellItemResult(null, List.of(), " item not found: " + identifier);
        if (quantity > item.getTotalQuantity()) {
            return new SellItemResult(null, List.of(), " insufficient stock: " + item.getDisplayName());
        }
        SellItemCommand command = new SellItemCommand(inventory, itemKey, quantity);
        try {
            commandManager.execute(command);
            return new SellItemResult(inventory.get(itemKey), command.getSoldBatches(), null);
        } catch (IOException exception) {
            return new SellItemResult(null, List.of(), " unable to save inventory; sale cancelled");
        }
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
