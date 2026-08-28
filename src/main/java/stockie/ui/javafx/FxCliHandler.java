package stockie.ui.javafx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.CommandResult;
import stockie.application.result.ExpiringItem;
import stockie.application.result.FindQueryResult;
import stockie.application.result.RecallBatchResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.SellItemResult;
import stockie.entities.InventoryItem;
import stockie.ui.javafx.util.FxFormatter;
import stockie.ui.javafx.util.InventoryRow;

/** Executes commands entered in the JavaFX command panel. */
public final class FxCliHandler {
    private final StockieController controller;
    private final Consumer<List<InventoryRow>> rowsConsumer;
    private final Consumer<InventoryRow> selectionConsumer;
    private final Runnable refresh;
    private final Runnable closeApplication;
    private final Function<InventoryItem, InventoryRow> itemMapper;

    public FxCliHandler(StockieController controller, Consumer<List<InventoryRow>> rowsConsumer,
            Consumer<InventoryRow> selectionConsumer, Runnable refresh,
            Runnable closeApplication, Function<InventoryItem, InventoryRow> itemMapper) {
        this.controller = controller;
        this.rowsConsumer = rowsConsumer;
        this.selectionConsumer = selectionConsumer;
        this.refresh = refresh;
        this.closeApplication = closeApplication;
        this.itemMapper = itemMapper;
    }

    public String execute(String input) {
        int separator = input.indexOf(' ');
        String command = (separator < 0 ? input : input.substring(0, separator)).toLowerCase(Locale.ROOT);
        String arguments = separator < 0 ? "" : input.substring(separator + 1).trim();
        return switch (command) {
        case "help" -> "add, recall, remove, sell, update-sku, list, find, undo, redo, bye\n"
                + "Use --item, --sku, --quantity, --invoice, --price, --expiry, and --upc.\n";
        case "bye" -> close();
        case "undo" -> history(controller.undo(), true);
        case "redo" -> history(controller.redo(), false);
        case "list" -> list(arguments);
        case "find" -> find(arguments);
        case "add" -> add(arguments);
        case "sell" -> sell(arguments);
        case "recall" -> recall(arguments);
        case "remove" -> remove(arguments);
        case "update-sku" -> updateSku(arguments);
        default -> "Unknown command. Type help for available commands.\n";
        };
    }

    private String list(String arguments) {
        String option = arguments.trim().toLowerCase(Locale.ROOT);
        StringBuilder output = new StringBuilder();
        if (option.isEmpty()) appendItems(output, controller.listItems(false).items());
        else if (option.equals("depleted")) appendItems(output, controller.listItems(true).items());
        else if (option.equals("expired")) appendExpiring(output, controller.listExpiredBatches().items());
        else if (option.startsWith("expiring-in ")) {
            Integer days = integer(option.substring("expiring-in ".length()), output);
            if (days != null && days >= 0) appendExpiring(output, controller.listExpiringBatches(days).items());
        } else return "Usage: list [depleted | expired | expiring-in <days>]\n";
        refresh.run();
        return output.toString();
    }

    private String find(String arguments) {
        Map<String, String> fields = fields(arguments, List.of("item", "sku"));
        if (fields == null || fields.size() != 1) return "Usage: find --item <name> | --sku <sku>\n";
        FindQueryResult result = fields.containsKey("item") ? controller.findByName(fields.get("item"))
                : controller.findBySku(fields.get("sku"));
        if (result.message() != null) return result.message().trim() + "\n";
        InventoryRow row = itemMapper.apply(result.item());
        rowsConsumer.accept(List.of(row));
        selectionConsumer.accept(row);
        return result.item().getDisplayName() + " | SKU " + result.item().getSku()
                + " | Qty " + result.item().getTotalQuantity() + "\n";
    }

    private String add(String arguments) {
        Map<String, String> fields = fields(arguments,
                List.of("item", "sku", "invoice", "quantity", "price", "expiry", "upc"));
        if (fields == null || !fields.keySet().containsAll(List.of("item", "sku", "invoice", "quantity", "price"))) {
            return "Usage: add --item <name> --sku <sku> --invoice <invoice> --quantity <quantity> --price <price> [--expiry <dd-MM-yyyy>] [--upc <upc>]\n";
        }
        StringBuilder output = new StringBuilder();
        Integer quantity = integer(fields.get("quantity"), output);
        BigDecimal price = decimal(fields.get("price"), output);
        LocalDate expiry = date(fields.get("expiry"), output);
        if (quantity == null || quantity <= 0 || price == null || price.signum() < 0
                || (fields.containsKey("expiry") && expiry == null)) return output.toString();
        var result = controller.addBatch(new AddBatchRequest(fields.get("item"), fields.get("sku"),
                fields.get("invoice"), quantity, price, expiry, fields.get("upc")));
        if (result.message() != null) return result.message().trim() + "\n";
        refresh.run();
        return "Added batch for " + result.item().getDisplayName() + "\n";
    }

    private String sell(String arguments) {
        Map<String, String> fields = fields(arguments, List.of("item", "sku", "quantity"));
        if (fields == null || (!fields.containsKey("item") && !fields.containsKey("sku"))
                || !fields.containsKey("quantity")) {
            return "Usage: sell (--item <name> | --sku <sku>) --quantity <quantity>\n";
        }
        StringBuilder output = new StringBuilder();
        Integer quantity = integer(fields.get("quantity"), output);
        if (quantity == null || quantity <= 0) return output.toString();
        SellItemResult result = fields.containsKey("item") ? controller.sellItemByName(fields.get("item"), quantity)
                : controller.sellItemBySku(fields.get("sku"), quantity);
        return mutation(result.message(), result.message() == null ? "Sold " + quantity + " of "
                + result.item().getDisplayName() : null);
    }

    private String recall(String arguments) {
        Map<String, String> fields = fields(arguments, List.of("item", "sku", "invoice"));
        if (fields == null || !fields.containsKey("invoice")
                || (!fields.containsKey("item") && !fields.containsKey("sku"))) {
            return "Usage: recall (--item <name> | --sku <sku>) --invoice <invoice>\n";
        }
        RecallBatchResult result = fields.containsKey("item") ? controller.recallBatchByName(fields.get("item"), fields.get("invoice"))
                : controller.recallBatchBySku(fields.get("sku"), fields.get("invoice"));
        return mutation(result.message(), "Batch recalled.");
    }

    private String remove(String arguments) {
        Map<String, String> fields = fields(arguments, List.of("item", "sku"));
        if (fields == null || (!fields.containsKey("item") && !fields.containsKey("sku"))) {
            return "Usage: remove (--item <name> | --sku <sku>)\n";
        }
        RemoveItemResult result = fields.containsKey("item") ? controller.removeItemByName(fields.get("item"))
                : controller.removeItemBySku(fields.get("sku"));
        return mutation(result.message(), "Item removed.");
    }

    private String updateSku(String arguments) {
        Map<String, String> fields = fields(arguments, List.of("item", "current-sku", "sku"));
        if (fields == null || !fields.containsKey("sku")
                || (!fields.containsKey("item") && !fields.containsKey("current-sku"))) {
            return "Usage: update-sku (--item <name> | --current-sku <old sku>) --sku <new sku>\n";
        }
        var result = fields.containsKey("item") ? controller.updateSkuByName(fields.get("item"), fields.get("sku"))
                : controller.updateSkuByCurrentSku(fields.get("current-sku"), fields.get("sku"));
        return mutation(result.message(), "SKU updated.");
    }

    private String mutation(String message, String success) {
        if (message == null) refresh.run();
        return (message == null ? success : message.trim()) + "\n";
    }

    private String history(CommandResult result, boolean undo) {
        if (result.message() == null) refresh.run();
        return (result.message() == null ? (undo ? "Undo" : "Redo") + " applied." : result.message().trim()) + "\n";
    }

    private Map<String, String> fields(String arguments, List<String> supported) {
        String[] tokens = arguments.trim().isEmpty() ? new String[0] : arguments.trim().split("\\s+");
        Map<String, String> values = new HashMap<>();
        String key = null;
        StringBuilder value = new StringBuilder();
        for (String token : tokens) {
            if (token.startsWith("--")) {
                if (key != null && !store(values, key, value.toString())) return null;
                key = token.substring(2).toLowerCase(Locale.ROOT);
                if (!supported.contains(key)) return null;
                value.setLength(0);
            } else if (key == null) return null;
            else { if (value.length() > 0) value.append(' '); value.append(token); }
        }
        if (key != null && !store(values, key, value.toString())) return null;
        return values;
    }

    private boolean store(Map<String, String> values, String key, String value) {
        return !value.trim().isEmpty() && values.putIfAbsent(key, value.trim()) == null;
    }

    private Integer integer(String text, StringBuilder output) { try { return Integer.parseInt(text.trim()); }
        catch (NumberFormatException exception) { output.append("Expected a whole number.\n"); return null; } }
    private BigDecimal decimal(String text, StringBuilder output) { try { return new BigDecimal(text.trim()); }
        catch (NumberFormatException exception) { output.append("Expected a valid price.\n"); return null; } }
    private LocalDate date(String text, StringBuilder output) { if (text == null) return null; try {
        return LocalDate.parse(text.trim(), FxFormatter.DATE_FORMAT); }
        catch (RuntimeException exception) { output.append("Expiry must use dd-MM-yyyy.\n"); return null; } }
    private void appendItems(StringBuilder output, List<InventoryItem> items) {
        if (items.isEmpty()) {
            output.append("No matching items.\n");
            return;
        }
        for (InventoryItem item : items) {
            output.append(item.getDisplayName()).append(" | SKU ").append(item.getSku())
                    .append(" | Qty ").append(item.getTotalQuantity()).append('\n');
        }
    }

    /** Closes the application and returns the session termination message. */
    private String close() {
        closeApplication.run();
        return "Goodbye.\n";
    }

    private void appendExpiring(StringBuilder output, List<ExpiringItem> items) {
        if (items.isEmpty()) {
            output.append("No matching batches.\n");
            return;
        }
        for (ExpiringItem item : items) {
            output.append(item.item().getDisplayName()).append(" | batches ")
                    .append(item.batches().size()).append('\n');
        }
    }
}
