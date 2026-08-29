package stockie.ui.javafx;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import stockie.application.command.CommandManager;
import stockie.application.controller.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.FindQueryResult;
import stockie.application.result.RecallBatchResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.SellItemResult;
import stockie.application.result.UpdateSkuResult;
import stockie.application.service.InventoryService;
import stockie.entities.Batch;
import stockie.entities.InventoryItem;
import stockie.entities.PerishableBatch;
import stockie.storage.FileInventoryRepository;
import stockie.storage.InventoryRepository;
import stockie.ui.javafx.util.BatchRow;
import stockie.ui.javafx.util.DashboardMetrics;
import stockie.ui.javafx.util.FxFormatter;
import stockie.ui.javafx.util.InventoryRow;
import stockie.ui.javafx.view.InventoryDetailsView;
import stockie.ui.javafx.view.InventoryTableView;
import stockie.ui.javafx.view.ViewMode;

/**
 * JavaFX application UI for Stockie.
 *
 * <p>This class reuses the existing StockieController API and presents the same features
 * that were available in the console UI.</p>
 */
public final class StockieFxApp extends Application {
    private static final DateTimeFormatter DATE_FORMAT = FxFormatter.DATE_FORMAT;

    private final Label statusLabel = new Label("Ready");

    private final Label trackedItemsValue = new Label("0");
    private final Label totalStockValue = new Label("0");
    private final Label inventoryCostValue = new Label("0.00");
    private final Label expiringSoonValue = new Label("0");

    private final InventoryDetailsView detailsView = new InventoryDetailsView();
    private InventoryTableView inventoryView;

    private StockieController controller;
    private StockieFxPresenter presenter;
    private FxCliHandler cliHandler;
    private final FxDialogs dialogs = new FxDialogs();
    private ViewMode currentViewMode = ViewMode.ALL;
    private int expiringInDays = 7;

    /** Starts the application and loads persisted inventory data. */
    @Override
    public void start(Stage stage) {
        this.controller = createController();
        this.presenter = new StockieFxPresenter(controller, this::replaceInventoryRows,
            this::applyDashboardMetrics, this::showStatus, this::clearDetails);
        this.cliHandler = new FxCliHandler(controller, this::replaceInventoryRows,
            this::bindDetailPanel, this::refreshCurrentView,
            stage::close, this::toInventoryRow);
        try {
            List<String> skippedItems = controller.load();
            if (!skippedItems.isEmpty()) {
                showWarning("Skipped " + skippedItems.size() + " corrupted inventory entr"
                        + (skippedItems.size() == 1 ? "y" : "ies") + ".");
            }
        } catch (Exception exception) {
            showWarning("Unable to load saved inventory. Starting with an empty inventory.");
        }

        BorderPane root = buildRoot();
        Scene scene = new Scene(root, 1360, 820);
        scene.getStylesheets().add(getClass().getResource("/stockie/ui/javafx/mockup.css").toExternalForm());

        stage.setTitle("Stockie Inventory Workbench");
        stage.setScene(scene);
        stage.setMinWidth(1180);
        stage.setMinHeight(760);
        stage.show();

        refreshCurrentView();
    }

    /** Builds app dependencies in the same way as the CLI entry point. */
    private static StockieController createController() {
        InventoryRepository repository = new FileInventoryRepository(
                Path.of(System.getProperty("stockie.data.file", "stockie-inventory.dat")));
        InventoryService inventoryService = new InventoryService();
        CommandManager commandManager = new CommandManager(inventoryService, repository);
        return new StockieController(inventoryService, commandManager, repository);
    }

    /** Builds the root container with top, left, center, and bottom regions. */
    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildTopSection());
        root.setLeft(buildSidebar());
        root.setCenter(buildMainContent());
        root.setBottom(buildActionBar());
        return root;
    }

    /** Builds title area, search controls, and undo/redo operations. */
    private VBox buildTopSection() {
        Label title = new Label("Stockie Inventory Workbench");
        title.getStyleClass().add("title-text");

        Label subtitle = new Label("Track batches, monitor expiries, and perform inventory actions");
        subtitle.getStyleClass().add("subtitle-text");

        TextField searchField = new TextField();
        searchField.setPromptText("Search inventory...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ToggleGroup searchMode = new ToggleGroup();
        RadioButton byItem = new RadioButton("Item");
        byItem.setToggleGroup(searchMode);
        byItem.setSelected(true);
        byItem.getStyleClass().add("radio-button-dark");
        RadioButton bySku = new RadioButton("SKU");
        bySku.setToggleGroup(searchMode);
        bySku.getStyleClass().add("radio-button-dark");

        Button findButton = new Button("Find");
        findButton.getStyleClass().add("primary-button");
        findButton.setOnAction(event -> performFind(searchField.getText(), byItem.isSelected()));

        Button undoButton = new Button("Undo");
        undoButton.setOnAction(event -> applyHistoryResult(controller.undo(), true));

        Button redoButton = new Button("Redo");
        redoButton.setOnAction(event -> applyHistoryResult(controller.redo(), false));

        HBox controls = new HBox(10, searchField, byItem, bySku, findButton, undoButton, redoButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().add("status-label");

        VBox top = new VBox(8, title, subtitle, controls, statusLabel);
        top.getStyleClass().add("top-section");
        top.setPadding(new Insets(18, 20, 12, 20));
        return top;
    }

    /** Builds left navigation and quick list filters. */
    private VBox buildSidebar() {
        Label filterLabel = new Label("Quick Filters");
        filterLabel.getStyleClass().add("panel-header");

        Button allItems = buildFilterButton("All Items", () -> {
            currentViewMode = ViewMode.ALL;
            refreshCurrentView();
        });
        Button depleted = buildFilterButton("Depleted", () -> {
            currentViewMode = ViewMode.DEPLETED;
            refreshCurrentView();
        });
        Button expired = buildFilterButton("Expired", () -> {
            currentViewMode = ViewMode.EXPIRED;
            refreshCurrentView();
        });

        // findme 
        Label expiringLabel = new Label("Expiring In:");

        Label expiringDaysLabel = new Label("Days");
        TextField expiringDaysField = new TextField();
        expiringDaysField.setPromptText("7");
        HBox expiringDaysInput = new HBox(6, expiringDaysField, expiringDaysLabel);

        Button expiringCustom = buildFilterButton("Apply", () -> {
            String text = expiringDaysField.getText().trim();
            List<String> errors = new ArrayList<>();
            Integer days = parseIntConditional(text, value -> value > 0,
                    "Days must be a positive whole number less than " + Integer.MAX_VALUE + ".", errors);
            if (!errors.isEmpty()) {
                showWarning(String.join("\n", errors));
                return;
            }
            if (days == null) {
                return;
            }
            expiringInDays = days;
            currentViewMode = ViewMode.EXPIRING;
            refreshCurrentView();
        });
        VBox expiringControls = new VBox(6, expiringLabel, expiringDaysInput, expiringCustom);

        VBox sidebar = new VBox(10,
                filterLabel,
                allItems,
                depleted,
                expired,
                expiringControls);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(240);
        return sidebar;
    }

    /** Creates one quick-filter button with shared style and behavior. */
    private Button buildFilterButton(String label, Runnable onClick) {
        Button button = new Button(label);
        button.getStyleClass().add("sidebar-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> onClick.run());
        return button;
    }

    /** Builds the central dashboard cards and split detail area. */
    private VBox buildMainContent() {
        HBox cards = buildDashboardCards();
        inventoryView = new InventoryTableView(this::bindDetailPanel);
        SplitPane splitPane = new SplitPane(inventoryView.node(), detailsView.node());
        splitPane.setDividerPositions(0.62);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox content = new VBox(14, cards, splitPane);
        content.setPadding(new Insets(14, 16, 14, 16));
        return content;
    }

    /** Builds summary cards that reflect current persisted data. */
    private HBox buildDashboardCards() {
        trackedItemsValue.getStyleClass().add("card-value");
        totalStockValue.getStyleClass().add("card-value");
        inventoryCostValue.getStyleClass().add("card-value");
        expiringSoonValue.getStyleClass().add("card-value");

        HBox cards = new HBox(12,
                createMetricCard("Tracked Items", trackedItemsValue, "catalog size"),
                createMetricCard("Total Stock", totalStockValue, "units on hand"),
                createMetricCard("Inventory Cost", inventoryCostValue, "total value"),
                createMetricCard("Expiring Soon", expiringSoonValue, "next " + expiringInDays + " days"));
        cards.getStyleClass().add("cards-row");
        return cards;
    }

    /** Creates one metric card in the dashboard area. */
    private StackPane createMetricCard(String title, Label valueLabel, String caption) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("card-caption");

        VBox content = new VBox(4, titleLabel, valueLabel, captionLabel);
        StackPane card = new StackPane(content);
        card.getStyleClass().add("metric-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    /** Builds operation buttons that call corresponding controller methods. */
    private HBox buildActionBar() {
        Button addButton = new Button("Add Batch");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> showAddBatchDialog());

        Button sellButton = new Button("Sell Item");
        sellButton.setOnAction(event -> showSellDialog());

        Button recallButton = new Button("Recall Batch");
        recallButton.setOnAction(event -> showRecallDialog());

        Button removeButton = new Button("Remove Item");
        removeButton.getStyleClass().add("danger-button");
        removeButton.setOnAction(event -> showRemoveDialog());

        Button updateSkuButton = new Button("Update SKU");
        updateSkuButton.setOnAction(event -> showUpdateSkuDialog());

        TextArea cliOutput = new TextArea();
        cliOutput.setEditable(false);
        cliOutput.setWrapText(true);
        cliOutput.setPromptText("Command output");
        cliOutput.setPrefRowCount(2);
        cliOutput.getStyleClass().add("cli-output");

        TextField cliInput = new TextField();
        cliInput.setPromptText("CLI command, e.g. list or sell --item Apples --quantity 2");
        cliInput.setOnAction(event -> {
            String command = cliInput.getText().trim();
            if (!command.isEmpty()) {
                cliOutput.appendText("> " + command + "\n");
                cliOutput.appendText(cliHandler.execute(command));
                cliInput.clear();
            }
        });
        HBox.setHgrow(cliInput, Priority.ALWAYS);
        VBox cliPane = new VBox(4, cliOutput, cliInput);
        cliPane.getStyleClass().add("cli-pane");
        HBox.setHgrow(cliPane, Priority.ALWAYS);

        HBox bar = new HBox(10, addButton, sellButton, recallButton, removeButton, updateSkuButton, cliPane);
        bar.setPadding(new Insets(12, 20, 18, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("action-bar");
        return bar;
    }

    /** Refreshes card metrics and table data according to the current quick filter. */
    private void refreshCurrentView() {
        presenter.refresh(currentViewMode, expiringInDays);
    }

    /** Applies presenter-calculated values to the dashboard cards. */
    private void applyDashboardMetrics(DashboardMetrics metrics) {
        trackedItemsValue.setText(String.valueOf(metrics.trackedItems()));
        totalStockValue.setText(String.valueOf(metrics.totalStock()));
        inventoryCostValue.setText("$" + formatPrice(metrics.inventoryCost()));
        expiringSoonValue.setText(String.valueOf(metrics.expiringSoon()));
    }

    /** Updates table content while preserving a sensible initial selection. */
    private void replaceInventoryRows(List<InventoryRow> rows) {
        inventoryView.setRows(rows);
        if (rows.isEmpty()) {
            clearDetails();
            return;
        }
        inventoryView.selectFirst();
    }

    /** Handles item lookup by name or SKU and focuses the matching row. */
    private void performFind(String query, boolean byItem) {
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.isEmpty()) {
            showWarning("Enter a value to find.");
            return;
        }

        FindQueryResult result = byItem ? controller.findByName(trimmedQuery) : controller.findBySku(trimmedQuery);
        if (result.message() != null) {
            showWarning(result.message());
            return;
        }

        currentViewMode = ViewMode.ALL;
        InventoryRow row = toInventoryRow(result.item());
        inventoryView.setRows(List.of(row));
        bindDetailPanel(row);
        showStatus("Found: " + result.item().getDisplayName());
    }

    /** Handles undo/redo response and refreshes the current list filter. */
    private void applyHistoryResult(CommandResult result, boolean undo) {
        if (result.message() != null) {
            showWarning(result.message());
            return;
        }
        refreshCurrentView();
        showStatus((undo ? "Undo" : "Redo") + " applied successfully");
    }

    /** Updates the right detail pane based on selected item. */
    private void bindDetailPanel(InventoryRow row) {
        detailsView.show(row);
    }

    /** Clears the details panel when there is no selected row. */
    private void clearDetails() {
        detailsView.clear();
    }

    /** Opens and processes the add-batch form. */
    private void showAddBatchDialog() {
        TextField itemField = new TextField();
        TextField skuField = new TextField();
        TextField invoiceField = new TextField();
        TextField quantityField = new TextField();
        TextField priceField = new TextField();
        DatePicker expiryPicker = new DatePicker();
        TextField upcField = new TextField();

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Item"), itemField);
        form.addRow(1, new Label("SKU"), skuField);
        form.addRow(2, new Label("Invoice"), invoiceField);
        form.addRow(3, new Label("Quantity"), quantityField);
        form.addRow(4, new Label("Unit Price"), priceField);
        form.addRow(5, new Label("Expiry (optional)"), expiryPicker);
        form.addRow(6, new Label("UPC (optional)"), upcField);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Add Batch");
        alert.setHeaderText("Add Inventory Batch");
        alert.getDialogPane().setContent(form);
        alert.getDialogPane().setPrefWidth(500);

        Optional<ButtonType> selection = alert.showAndWait();
        if (selection.isEmpty() || selection.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        String itemName = itemField.getText().trim();
        String sku = skuField.getText().trim();
        String invoice = invoiceField.getText().trim();
        List<String> errors = new ArrayList<>();
        Integer quantity = parseIntConditional(quantityField.getText(), value -> value > 0,
                "Quantity must be a positive whole number.", errors);
        BigDecimal unitPrice = parseNonNegativeDecimal(priceField.getText(),
                "Unit price must be a non-negative number.", errors);
        LocalDate expiry = expiryPicker.getValue();
        String upc = upcField.getText().trim().isEmpty() ? null : upcField.getText().trim();

        if (itemName.isEmpty()) {
            errors.add("Item is required.");
        }
        if (sku.isEmpty()) {
            errors.add("SKU is required.");
        }
        if (invoice.isEmpty()) {
            errors.add("Invoice is required.");
        }
        if (!errors.isEmpty()) {
            showWarning(String.join("\n", errors));
            return;
        }

        if (expiry != null && expiry.isBefore(LocalDate.now()) && !confirm(
                "Expired Batch Warning",
                "This batch expired on " + DATE_FORMAT.format(expiry) + ". Add it anyway?")) {
            return;
        }

        AddBatchResult result = controller.addBatch(new AddBatchRequest(itemName, sku, invoice, quantity,
                unitPrice, expiry, upc));
        if (result.message() != null) {
            showWarning(result.message());
            return;
        }
        refreshCurrentView();
        showStatus("Added batch for " + result.item().getDisplayName());
    }

    /** Opens and processes the sell-item form. */
    private void showSellDialog() {
        IdentifierInput input = showIdentifierDialog("Sell Item", "Quantity", true);
        if (input == null) {
            return;
        }

        SellItemResult result = input.byItem
                ? controller.sellItemByName(input.identifier, input.quantity)
                : controller.sellItemBySku(input.identifier, input.quantity);

        if (result.message() != null) {
            showWarning(result.message());
            return;
        }

        refreshCurrentView();
        dialogs.showSaleBreakdown(result);
        showStatus("Sold " + input.quantity + " of " + result.item().getDisplayName());
    }

    /** Opens and processes the recall-batch form. */
    private void showRecallDialog() {
        TextField invoiceField = new TextField();
        IdentifierInput input = showIdentifierDialog("Recall Batch", "Invoice", false);
        if (input == null) {
            return;
        }
        invoiceField.setText(input.extraFieldValue);
        String invoice = invoiceField.getText().trim();

        RecallBatchResult result = input.byItem
                ? controller.recallBatchByName(input.identifier, invoice)
                : controller.recallBatchBySku(input.identifier, invoice);

        if (result.message() != null) {
            showWarning(result.message());
            return;
        }

        refreshCurrentView();
        showStatus("Recalled batch " + invoice + " from " + result.item().getDisplayName());
    }

    /** Opens and processes the remove-item form. */
    private void showRemoveDialog() {
        IdentifierInput input = showIdentifierDialog("Remove Item", null, false);
        if (input == null) {
            return;
        }
        boolean approved = confirm("Confirm Removal",
                "Remove item " + input.identifier + " and all its batches?");
        if (!approved) {
            return;
        }

        RemoveItemResult result = input.byItem
                ? controller.removeItemByName(input.identifier)
                : controller.removeItemBySku(input.identifier);

        if (result.message() != null) {
            showWarning(result.message());
            return;
        }

        refreshCurrentView();
        showStatus("Removed item " + result.item().getDisplayName());
    }

    /** Opens and processes the update-SKU form. */
    private void showUpdateSkuDialog() {
        TextField identifierField = new TextField();
        TextField newSkuField = new TextField();
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton byItem = new RadioButton("By Item Name");
        RadioButton byCurrentSku = new RadioButton("By Current SKU");
        byItem.setToggleGroup(modeGroup);
        byCurrentSku.setToggleGroup(modeGroup);
        byItem.setSelected(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, byItem, byCurrentSku);
        form.addRow(1, new Label("Identifier"), identifierField);
        form.addRow(2, new Label("New SKU"), newSkuField);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update SKU");
        alert.setHeaderText("Update an item's SKU");
        alert.getDialogPane().setContent(form);
        alert.getDialogPane().setPrefWidth(470);

        Optional<ButtonType> selection = alert.showAndWait();
        if (selection.isEmpty() || selection.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        String identifier = identifierField.getText().trim();
        String newSku = newSkuField.getText().trim();
        if (identifier.isEmpty() || newSku.isEmpty()) {
            showWarning("Identifier and new SKU are required.");
            return;
        }

        UpdateSkuResult result = byItem.isSelected()
                ? controller.updateSkuByName(identifier, newSku)
                : controller.updateSkuByCurrentSku(identifier, newSku);

        if (result.message() != null) {
            showWarning(result.message());
            return;
        }

        refreshCurrentView();
        showStatus("Updated SKU for " + result.item().getDisplayName()
                + " from " + result.oldSku() + " to " + result.item().getSku());
    }

    /** Builds a generic dialog for operations that need identifier + optional extra input. */
    private IdentifierInput showIdentifierDialog(String title, String extraFieldLabel, boolean numericExtra) {
        TextField identifierField = new TextField();
        TextField extraField = new TextField();
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton byItem = new RadioButton("By Item Name");
        RadioButton bySku = new RadioButton("By SKU");
        byItem.setToggleGroup(modeGroup);
        bySku.setToggleGroup(modeGroup);
        byItem.setSelected(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, byItem, bySku);
        form.addRow(1, new Label("Identifier"), identifierField);
        if (extraFieldLabel != null) {
            form.addRow(2, new Label(extraFieldLabel), extraField);
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getDialogPane().setContent(form);
        alert.getDialogPane().setPrefWidth(470);

        Optional<ButtonType> selection = alert.showAndWait();
        if (selection.isEmpty() || selection.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return null;
        }

        String identifier = identifierField.getText().trim();
        String extraValue = extraFieldLabel == null ? "" : extraField.getText().trim();
        List<String> errors = new ArrayList<>();
        if (identifier.isEmpty() || (extraFieldLabel != null && extraValue.isEmpty())) {
            if (identifier.isEmpty()) {
                errors.add("Identifier is required.");
            }
            if (extraFieldLabel != null && extraValue.isEmpty()) {
                errors.add(extraFieldLabel + " is required.");
            }
        }

        int quantity = 0;
        if (numericExtra && !extraValue.isEmpty()) {
            Integer parsed = parseIntConditional(extraValue, value -> value > 0,
                    "Quantity must be a positive whole number.", errors);
            if (parsed != null) {
                quantity = parsed;
            }
        }

        if (!errors.isEmpty()) {
            showWarning(String.join("\n", errors));
            return null;
        }

        return new IdentifierInput(identifier, byItem.isSelected(), quantity, extraValue);
    }

    /** Converts a full inventory item into a table row with all batches. */
    private InventoryRow toInventoryRow(InventoryItem item) {
        List<Batch> orderedBatches = item.getBatches().values().stream()
                .sorted(Comparator.comparing(Batch::getInvoiceNumber, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<BatchRow> rows = orderedBatches.stream().map(this::toBatchRow).toList();
        return new InventoryRow(item.getDisplayName(), item.getSku(),
                item.getCategory().name().toLowerCase(Locale.ROOT),
                item.getTotalQuantity(), item.getTotalCost(), rows);
    }

    /** Converts one domain batch object into detail-table row data. */
    private BatchRow toBatchRow(Batch batch) {
        LocalDate expiry = batch instanceof PerishableBatch
                ? ((PerishableBatch) batch).getExpiryDate() : null;
        return new BatchRow(batch.getInvoiceNumber(), batch.getQuantity(), batch.getUnitPrice(),
                expiry, batch.getUpc());
    }

    /** Parses and validates positive integers used by quantity inputs. */
    private Integer parseIntConditional(String text, Predicate<Integer> validator,
            String message, List<String> errors) {
        try {
            int value = Integer.parseInt(text.trim());
            if (validator.test(value)) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Validation feedback is returned below.
        }
        errors.add(message);
        return null;
    }

    /** Parses and validates non-negative decimal values used by unit price inputs. */
    private BigDecimal parseNonNegativeDecimal(String text, String message, List<String> errors) {
        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.signum() >= 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Validation feedback is returned below.
        }
        errors.add(message);
        return null;
    }

    /** Formats monetary values with two decimal places. */
    private String formatPrice(BigDecimal price) {
        return FxFormatter.price(price);
    }

    /** Shows a warning dialog and keeps the user on the current screen. */
    private void showWarning(String message) {
        dialogs.showWarning(message, this::showStatus);
    }

    /** Shows a two-option confirmation dialog. */
    private boolean confirm(String title, String message) {
        return dialogs.confirm(title, message);
    }

    /** Updates transient status text under the top action row. */
    private void showStatus(String text) {
        statusLabel.setText(text == null ? "" : text.trim());
    }

    /** Stores parsed identifier form values for action dialogs. */
    private record IdentifierInput(String identifier, boolean byItem, int quantity, String extraFieldValue) { }

}
