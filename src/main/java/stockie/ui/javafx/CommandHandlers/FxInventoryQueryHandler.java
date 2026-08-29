package stockie.ui.javafx.CommandHandlers;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import stockie.application.controller.StockieController;
import stockie.application.result.ExpiringItem;
import stockie.application.result.FindQueryResult;
import stockie.entities.InventoryItem;
import stockie.ui.javafx.command.CommandArgumentParser;
import stockie.ui.javafx.command.CommandConstants;
import stockie.ui.javafx.command.CommandResponseFormatter;
import stockie.ui.javafx.command.FxCommandResult;
import stockie.ui.javafx.util.InventoryRow;

/** Handles inventory query commands: list and find. */
public final class FxInventoryQueryHandler {
    private final StockieController controller;
    private final Function<InventoryItem, InventoryRow> itemMapper;

    public FxInventoryQueryHandler(StockieController controller, Function<InventoryItem, InventoryRow> itemMapper) {
        this.controller = controller;
        this.itemMapper = itemMapper;
    }

    public FxCommandResult list(String arguments) {
        String option = arguments.trim().toLowerCase(Locale.ROOT);
        StringBuilder output = new StringBuilder();
        if (option.isEmpty() || option.equals("depleted")) {
            appendItems(output, controller.listItems(option.equals("depleted")).items());
        } else if (option.equals("expired")) {
            appendExpiring(output, controller.listExpiredBatches().items());
        } else if (option.startsWith("expiring-in ")) {
            try {
                int days = Integer.parseInt(option.substring("expiring-in ".length()));
                if (days >= 0) {
                    appendExpiring(output, controller.listExpiringBatches(days).items());
                }
            } catch (NumberFormatException exception) {
                output.append("Expected a whole number for days.\n");
            }
        } else {
            return FxCommandResult.message("Usage: list [depleted | expired | expiring-in <days>]\n");
        }
        return FxCommandResult.refresh(output.toString());
    }

    public FxCommandResult find(String arguments) {
        CommandArgumentParser fields = CommandArgumentParser.parse(arguments,
                List.of(CommandConstants.ITEM, CommandConstants.SKU));
        if (fields == null || fields.size() != 1) {
            return FxCommandResult.message("Usage: find --item <name> | --sku <sku>\n");
        }
        FindQueryResult result = fields.has(CommandConstants.ITEM)
                ? controller.findByName(fields.get(CommandConstants.ITEM))
                : controller.findBySku(fields.get(CommandConstants.SKU));
        if (result.message() != null) {
            return FxCommandResult.message(result.message().trim() + "\n");
        }
        InventoryRow row = itemMapper.apply(result.item());
        return FxCommandResult.select(CommandResponseFormatter.inventoryItem(result.item()), row);
    }

    private void appendItems(StringBuilder output, List<InventoryItem> items) {
        if (items.isEmpty()) {
            output.append(CommandResponseFormatter.noMatchingItems());
            return;
        }
        for (InventoryItem item : items) {
            output.append(CommandResponseFormatter.inventoryItem(item));
        }
    }

    private void appendExpiring(StringBuilder output, List<ExpiringItem> items) {
        if (items.isEmpty()) {
            output.append(CommandResponseFormatter.noMatchingBatches());
            return;
        }
        for (ExpiringItem item : items) {
            output.append(item.item().getDisplayName()).append(" | batches ")
                    .append(item.batches().size()).append('\n');
        }
    }
}
