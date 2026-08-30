package stockie.ui.javafx.CommandHandlers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.RecallBatchResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.SellItemResult;
import stockie.ui.javafx.command.CommandArgumentParser;
import stockie.ui.javafx.command.CommandConstants;
import stockie.ui.javafx.command.CommandResponseFormatter;
import stockie.ui.javafx.command.FxCommandResult;
import stockie.ui.javafx.util.FxFormatter;

/** Handles inventory-changing commands: add, sell, recall, remove, and update-sku. */
public final class FxInventoryCommandHandler {
    private final StockieController controller;

    public FxInventoryCommandHandler(StockieController controller) {
        this.controller = controller;
    }

    /** Adds a batch from the supplied command arguments. */
    public FxCommandResult add(String arguments) {
        CommandArgumentParser fields = CommandArgumentParser.parse(arguments,
                List.of(CommandConstants.ITEM, CommandConstants.SKU, CommandConstants.INVOICE,
                        CommandConstants.QUANTITY, CommandConstants.PRICE, CommandConstants.EXPIRY,
                        CommandConstants.UPC));
        if (fields == null || !fields.has(CommandConstants.ITEM) || !fields.has(CommandConstants.SKU)
                || !fields.has(CommandConstants.INVOICE) || !fields.has(CommandConstants.QUANTITY)
                || !fields.has(CommandConstants.PRICE)) {
            return FxCommandResult.message("Usage: add --item <name> --sku <sku> --invoice <invoice> "
                    + "--quantity <quantity> --price <price> [--expiry <dd-MM-yyyy>] [--upc <upc>]\n");
        }
        StringBuilder output = new StringBuilder();
        Integer quantity = integer(fields.get(CommandConstants.QUANTITY), output, CommandConstants.QUANTITY);
        BigDecimal price = decimal(fields.get(CommandConstants.PRICE), output);
        LocalDate expiry = date(fields.get(CommandConstants.EXPIRY), output);
        if (quantity != null && quantity <= 0) {
            output.append("Quantity must be greater than zero.\n");
        }
        if (price != null && price.signum() < 0) {
            output.append("Price must not be negative.\n");
        }
        if (quantity == null || price == null || (fields.has(CommandConstants.EXPIRY) && expiry == null)
                || output.length() > 0) {
            return FxCommandResult.message(output.toString());
        }
        var result = controller.addBatch(new AddBatchRequest(fields.get(CommandConstants.ITEM),
                fields.get(CommandConstants.SKU), fields.get(CommandConstants.INVOICE), quantity, price,
                expiry, fields.get(CommandConstants.UPC)));
        if (result.message() != null) {
            return FxCommandResult.message(result.message().trim() + "\n");
        }
        return FxCommandResult.refresh(CommandResponseFormatter.addedBatch(result.item().getDisplayName()));
    }

    /** Sells inventory from the item identified by the supplied command arguments. */
    public FxCommandResult sell(String arguments) {
        CommandArgumentParser fields = CommandArgumentParser.parse(arguments,
                List.of(CommandConstants.ITEM, CommandConstants.SKU, CommandConstants.QUANTITY));
        if (fields == null || (!fields.has(CommandConstants.ITEM) && !fields.has(CommandConstants.SKU))
                || !fields.has(CommandConstants.QUANTITY)) {
            return FxCommandResult.message("Usage: sell (--item <name> | --sku <sku>) --quantity <quantity>\n");
        }
        StringBuilder output = new StringBuilder();
        Integer quantity = integer(fields.get(CommandConstants.QUANTITY), output, CommandConstants.QUANTITY);
        if (quantity != null && quantity <= 0) {
            output.append("Quantity must be greater than zero.\n");
        }
        if (quantity == null || output.length() > 0) {
            return FxCommandResult.message(output.toString());
        }
        SellItemResult result = fields.has(CommandConstants.ITEM)
                ? controller.sellItemByName(fields.get(CommandConstants.ITEM), quantity)
                : controller.sellItemBySku(fields.get(CommandConstants.SKU), quantity);
        return mutation(result.message(), result.message() == null
                ? CommandResponseFormatter.soldItems(quantity, result.item().getDisplayName()).trim() : null);
    }

    /** Recalls a batch from the item identified by the supplied command arguments. */
    public FxCommandResult recall(String arguments) {
        CommandArgumentParser fields = CommandArgumentParser.parse(arguments,
                List.of(CommandConstants.ITEM, CommandConstants.SKU, CommandConstants.INVOICE));
        if (fields == null || !fields.has(CommandConstants.INVOICE)
                || (!fields.has(CommandConstants.ITEM) && !fields.has(CommandConstants.SKU))) {
            return FxCommandResult.message("Usage: recall (--item <name> | --sku <sku>) --invoice <invoice>\n");
        }
        RecallBatchResult result = fields.has(CommandConstants.ITEM)
                ? controller.recallBatchByName(fields.get(CommandConstants.ITEM), fields.get(CommandConstants.INVOICE))
                : controller.recallBatchBySku(fields.get(CommandConstants.SKU), fields.get(CommandConstants.INVOICE));
        return mutation(result.message(), CommandResponseFormatter.batchRecalled());
    }

    /** Removes the item identified by the supplied command arguments. */
    public FxCommandResult remove(String arguments) {
        CommandArgumentParser fields = CommandArgumentParser.parse(arguments,
                List.of(CommandConstants.ITEM, CommandConstants.SKU));
        if (fields == null || (!fields.has(CommandConstants.ITEM) && !fields.has(CommandConstants.SKU))) {
            return FxCommandResult.message("Usage: remove (--item <name> | --sku <sku>)\n");
        }
        RemoveItemResult result = fields.has(CommandConstants.ITEM)
                ? controller.removeItemByName(fields.get(CommandConstants.ITEM))
                : controller.removeItemBySku(fields.get(CommandConstants.SKU));
        return mutation(result.message(), CommandResponseFormatter.itemRemoved());
    }

    /** Updates an item's SKU using the supplied command arguments. */
    public FxCommandResult updateSku(String arguments) {
        CommandArgumentParser fields = CommandArgumentParser.parse(arguments,
                List.of(CommandConstants.ITEM, CommandConstants.CURRENT_SKU, CommandConstants.SKU));
        if (fields == null || !fields.has(CommandConstants.SKU)
                || (!fields.has(CommandConstants.ITEM) && !fields.has(CommandConstants.CURRENT_SKU))) {
            return FxCommandResult.message("Usage: update-sku (--item <name> | --current-sku <old sku>) "
                    + "--sku <new sku>\n");
        }
        var result = fields.has(CommandConstants.ITEM)
                ? controller.updateSkuByName(fields.get(CommandConstants.ITEM), fields.get(CommandConstants.SKU))
                : controller.updateSkuByCurrentSku(fields.get(CommandConstants.CURRENT_SKU),
                        fields.get(CommandConstants.SKU));
        return mutation(result.message(), CommandResponseFormatter.skuUpdated());
    }

    private FxCommandResult mutation(String message, String success) {
        if (message == null) {
            return FxCommandResult.refresh(success + "\n");
        }
        return FxCommandResult.message(message.trim() + "\n");
    }

    private Integer integer(String text, StringBuilder output, String fieldDescription) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            output.append("Expected a whole number for ").append(fieldDescription).append(".\n");
            return null;
        }
    }

    private BigDecimal decimal(String text, StringBuilder output) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException exception) {
            output.append("Expected a valid price.\n");
            return null;
        }
    }

    private LocalDate date(String text, StringBuilder output) {
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), FxFormatter.DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            output.append("Expiry must use dd-MM-yyyy.\n");
            return null;
        }
    }
}
