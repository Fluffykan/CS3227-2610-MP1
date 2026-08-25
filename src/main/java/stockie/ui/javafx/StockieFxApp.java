package stockie.ui.javafx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import stockie.application.InventoryService;
import stockie.application.StockieController;
import stockie.application.request.AddBatchRequest;
import stockie.application.result.AddBatchResult;
import stockie.application.result.CommandResult;
import stockie.application.result.ExpiringBatchQueryResult;
import stockie.application.result.ExpiringItem;
import stockie.application.result.FindQueryResult;
import stockie.application.result.ListQueryResult;
import stockie.application.result.RecallBatchResult;
import stockie.application.result.RemoveItemResult;
import stockie.application.result.SellItemResult;
import stockie.application.result.SoldBatch;
import stockie.application.result.UpdateSkuResult;
import stockie.command.CommandManager;
import stockie.model.Batch;
import stockie.model.InventoryItem;
import stockie.model.PerishableBatch;
import stockie.storage.FileInventoryRepository;
import stockie.storage.InventoryRepository;

/**
 * JavaFX application UI for Stockie.
 *
 * <p>This class reuses the existing StockieController API and presents the same features
 * that were available in the console UI.</p>
 */
public final class StockieFxApp extends Application {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private final ObservableList<InventoryRow> inventoryRows = FXCollections.observableArrayList();
    private final ObservableList<BatchRow> detailRows = FXCollections.observableArrayList();

    private final Label selectedItemLabel = new Label("No item selected");
    private final Label selectedSkuLabel = new Label("-");
    private final Label selectedCategoryLabel = new Label("-");
    private final Label selectedQuantityLabel = new Label("-");
    private final Label selectedCostLabel = new Label("-");
    private final Label statusLabel = new Label("Ready");

    private final Label trackedItemsValue = new Label("0");
    private final Label totalStockValue = new Label("0");
    private final Label inventoryCostValue = new Label("0.00");
    private final Label expiringSoonValue = new Label("0");

    private TableView<InventoryRow> inventoryTable;
    private TableView<BatchRow> batchTable;

    private StockieController controller;
    private ViewMode currentViewMode = ViewMode.ALL;
    private int expiringInDays = 7;

    /** Starts the application and loads persisted inventory data. */
    @Override
    public void start(Stage stage) {
        this.controller = createController();
        try {
            controller.load();
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
            Integer days = parseIntConditional(text, value -> value > 0, "Days must be a positive whole number less than " + Integer.MAX_VALUE + ".");
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
        SplitPane splitPane = new SplitPane(buildInventoryTablePane(), buildDetailsPane());
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

    /** Builds the inventory master table. */
    private VBox buildInventoryTablePane() {
        Label header = new Label("Inventory");
        header.getStyleClass().add("panel-header");

        inventoryTable = new TableView<>();
        inventoryTable.setItems(inventoryRows);
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<InventoryRow, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().itemName()));

        TableColumn<InventoryRow, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().sku()));

        TableColumn<InventoryRow, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category()));

        TableColumn<InventoryRow, Number> quantityCol = new TableColumn<>("Total Qty");
        quantityCol.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().totalQuantity()));

        TableColumn<InventoryRow, Number> costCol = new TableColumn<>("Inventory Cost");
        costCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().inventoryCost()));
        costCol.setComparator(Comparator.comparingDouble(Number::doubleValue));

        inventoryTable.getColumns().setAll(itemCol, skuCol, categoryCol, quantityCol, costCol);
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                bindDetailPanel(newItem);
            }
        });

        VBox wrapper = new VBox(8, header, inventoryTable);
        wrapper.getStyleClass().add("content-panel");
        VBox.setVgrow(inventoryTable, Priority.ALWAYS);
        return wrapper;
    }

    /** Builds the detail panel and related batch table. */
    private VBox buildDetailsPane() {
        Label header = new Label("Item Details");
        header.getStyleClass().add("panel-header");

        GridPane infoGrid = new GridPane();
        infoGrid.setVgap(6);
        infoGrid.setHgap(10);
        infoGrid.addRow(0, new Label("Item"), selectedItemLabel);
        infoGrid.addRow(1, new Label("SKU"), selectedSkuLabel);
        infoGrid.addRow(2, new Label("Category"), selectedCategoryLabel);
        infoGrid.addRow(3, new Label("Total Qty"), selectedQuantityLabel);
        infoGrid.addRow(4, new Label("Inventory Cost"), selectedCostLabel);

        Label batchesHeader = new Label("Batches");
        batchesHeader.getStyleClass().add("panel-header");

        batchTable = new TableView<>();
        batchTable.setItems(detailRows);
        batchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<BatchRow, String> invoiceCol = new TableColumn<>("Invoice");
        invoiceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().invoice()));

        TableColumn<BatchRow, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().quantity()));

        TableColumn<BatchRow, Number> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitPrice()));

        TableColumn<BatchRow, String> expiryCol = new TableColumn<>("Expiry");
        expiryCol.setCellValueFactory(cell -> {
            LocalDate expiry = cell.getValue().expiry();
            String formatted = expiry == null ? "-" : DATE_FORMAT.format(expiry);
            return new ReadOnlyStringWrapper(formatted);
        });

        TableColumn<BatchRow, String> upcCol = new TableColumn<>("UPC");
        upcCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().upc() == null
                ? "-" : cell.getValue().upc()));

        batchTable.getColumns().setAll(invoiceCol, qtyCol, priceCol, expiryCol, upcCol);

        VBox wrapper = new VBox(8, header, infoGrid, new Separator(), batchesHeader, batchTable);
        wrapper.getStyleClass().add("content-panel");
        VBox.setVgrow(batchTable, Priority.ALWAYS);
        return wrapper;
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
                executeCliCommand(command, cliOutput);
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

    /** Executes the same named-argument command style as the console UI. */
    private void executeCliCommand(String input, TextArea output) {
        int separator = input.indexOf(' ');
        String command = (separator < 0 ? input : input.substring(0, separator)).toLowerCase(Locale.ROOT);
        String arguments = separator < 0 ? "" : input.substring(separator + 1).trim();

        switch (command) {
        case "help":
            output.appendText("add, recall, remove, sell, update-sku, list, find, undo, redo, bye\n");
            output.appendText("Use --item, --sku, --quantity, --invoice, --price, --expiry, and --upc.\n");
            break;
        case "bye":
            output.appendText("CLI ready. The window remains open.\n");
            break;
        case "undo":
            appendCommandResult(output, controller.undo(), true);
            break;
        case "redo":
            appendCommandResult(output, controller.redo(), false);
            break;
        case "list":
            executeCliList(arguments, output);
            break;
        case "find":
            executeCliFind(arguments, output);
            break;
        case "add":
            executeCliAdd(arguments, output);
            break;
        case "sell":
            executeCliSell(arguments, output);
            break;
        case "recall":
            executeCliRecall(arguments, output);
            break;
        case "remove":
            executeCliRemove(arguments, output);
            break;
        case "update-sku":
            executeCliUpdateSku(arguments, output);
            break;
        default:
            output.appendText("Unknown command. Type help for available commands.\n");
            break;
        }
    }

    /** Runs list commands from the inline CLI. */
    private void executeCliList(String arguments, TextArea output) {
        String option = arguments.trim().toLowerCase(Locale.ROOT);
        if (option.isEmpty()) {
            appendItems(output, controller.listItems(false).items());
        } else if (option.equals("depleted")) {
            appendItems(output, controller.listItems(true).items());
        } else if (option.equals("expired")) {
            appendExpiringItems(output, controller.listExpiredBatches().items());
        } else if (option.startsWith("expiring-in ")) {
            Integer days = parseCliInteger(option.substring("expiring-in ".length()), output);
            if (days != null && days > 0) {
                appendExpiringItems(output, controller.listExpiringBatches(days).items());
            }
        } else {
            output.appendText("Usage: list [depleted | expired | expiring-in <days>]\n");
            return;
        }
        refreshCurrentView();
    }

    /** Runs find commands from the inline CLI. */
    private void executeCliFind(String arguments, TextArea output) {
        Map<String, String> fields = parseCliFields(arguments, List.of("item", "sku"), output);
        if (fields == null || fields.size() != 1) {
            output.appendText("Usage: find --item <name> | --sku <sku>\n");
            return;
        }
        FindQueryResult result = fields.containsKey("item")
                ? controller.findByName(fields.get("item")) : controller.findBySku(fields.get("sku"));
        if (result.message() != null) {
            output.appendText(result.message().trim() + "\n");
            return;
        }
        InventoryRow row = toInventoryRow(result.item());
        inventoryRows.setAll(List.of(row));
        inventoryTable.getSelectionModel().selectFirst();
        bindDetailPanel(row);
        output.appendText(result.item().getDisplayName() + " | SKU " + result.item().getSku()
                + " | Qty " + result.item().getTotalQuantity() + "\n");
    }

    /** Runs add commands from the inline CLI. */
    private void executeCliAdd(String arguments, TextArea output) {
        Map<String, String> fields = parseCliFields(arguments,
                List.of("item", "sku", "invoice", "quantity", "price", "expiry", "upc"), output);
        if (fields == null || !fields.keySet().containsAll(List.of("item", "sku", "invoice", "quantity", "price"))) {
            output.appendText("Usage: add --item <name> --sku <sku> --invoice <invoice> --quantity <quantity> --price <price> [--expiry <dd-MM-yyyy>] [--upc <upc>]\n");
            return;
        }
        Integer quantity = parseCliInteger(fields.get("quantity"), output);
        BigDecimal price = parseCliDecimal(fields.get("price"), output);
        LocalDate expiry = parseCliDate(fields.get("expiry"), output);
        if (quantity == null || quantity <= 0 || price == null || price.signum() < 0
                || (fields.containsKey("expiry") && expiry == null)) {
            return;
        }
        AddBatchResult result = controller.addBatch(new AddBatchRequest(fields.get("item"), fields.get("sku"),
                fields.get("invoice"), quantity, price, expiry, fields.get("upc")));
        if (result.message() != null) {
            output.appendText(result.message().trim() + "\n");
            return;
        }
        output.appendText("Added batch for " + result.item().getDisplayName() + "\n");
        refreshCurrentView();
    }

    /** Runs sell commands from the inline CLI. */
    private void executeCliSell(String arguments, TextArea output) {
        Map<String, String> fields = parseCliFields(arguments, List.of("item", "sku", "quantity"), output);
        if (fields == null || (!fields.containsKey("item") && !fields.containsKey("sku")) || !fields.containsKey("quantity")) {
            output.appendText("Usage: sell (--item <name> | --sku <sku>) --quantity <quantity>\n");
            return;
        }
        Integer quantity = parseCliInteger(fields.get("quantity"), output);
        if (quantity == null || quantity <= 0) return;
        SellItemResult result = fields.containsKey("item")
                ? controller.sellItemByName(fields.get("item"), quantity)
                : controller.sellItemBySku(fields.get("sku"), quantity);
        appendMutationResult(output, result.message(), result.message() == null
                ? "Sold " + quantity + " of " + result.item().getDisplayName() : null);
    }

    /** Runs recall, remove, and SKU update commands from the inline CLI. */
    private void executeCliRecall(String arguments, TextArea output) {
        Map<String, String> fields = parseCliFields(arguments, List.of("item", "sku", "invoice"), output);
        if (fields == null || !fields.containsKey("invoice") || (!fields.containsKey("item") && !fields.containsKey("sku"))) {
            output.appendText("Usage: recall (--item <name> | --sku <sku>) --invoice <invoice>\n");
            return;
        }
        RecallBatchResult result = fields.containsKey("item")
                ? controller.recallBatchByName(fields.get("item"), fields.get("invoice"))
                : controller.recallBatchBySku(fields.get("sku"), fields.get("invoice"));
        appendMutationResult(output, result.message(), result.message() == null ? "Batch recalled." : null);
    }

    private void executeCliRemove(String arguments, TextArea output) {
        Map<String, String> fields = parseCliFields(arguments, List.of("item", "sku"), output);
        if (fields == null || (!fields.containsKey("item") && !fields.containsKey("sku"))) {
            output.appendText("Usage: remove (--item <name> | --sku <sku>)\n");
            return;
        }
        RemoveItemResult result = fields.containsKey("item")
                ? controller.removeItemByName(fields.get("item")) : controller.removeItemBySku(fields.get("sku"));
        appendMutationResult(output, result.message(), result.message() == null ? "Item removed." : null);
    }

    private void executeCliUpdateSku(String arguments, TextArea output) {
        Map<String, String> fields = parseCliFields(arguments, List.of("item", "current-sku", "sku"), output);
        if (fields == null || !fields.containsKey("sku") || (!fields.containsKey("item") && !fields.containsKey("current-sku"))) {
            output.appendText("Usage: update-sku (--item <name> | --current-sku <old sku>) --sku <new sku>\n");
            return;
        }
        UpdateSkuResult result = fields.containsKey("item")
                ? controller.updateSkuByName(fields.get("item"), fields.get("sku"))
                : controller.updateSkuByCurrentSku(fields.get("current-sku"), fields.get("sku"));
        appendMutationResult(output, result.message(), result.message() == null ? "SKU updated." : null);
    }

    /** Parses the console-style {@code --field value} arguments for one command. */
    private Map<String, String> parseCliFields(String arguments, List<String> supported, TextArea output) {
        String[] tokens = arguments.trim().isEmpty() ? new String[0] : arguments.trim().split("\\s+");
        Map<String, String> fields = new HashMap<>();
        String currentKey = null;
        StringBuilder value = new StringBuilder();
        for (String token : tokens) {
            if (token.startsWith("--")) {
                if (currentKey != null && !storeCliField(fields, currentKey, value.toString(), output)) return null;
                currentKey = token.substring(2).toLowerCase(Locale.ROOT);
                if (!supported.contains(currentKey)) {
                    output.appendText("Unknown field: --" + currentKey + "\n");
                    return null;
                }
                value.setLength(0);
            } else if (currentKey == null) {
                output.appendText("Values must follow a named field such as --item.\n");
                return null;
            } else {
                if (value.length() > 0) value.append(' ');
                value.append(token);
            }
        }
        if (currentKey != null && !storeCliField(fields, currentKey, value.toString(), output)) return null;
        return fields;
    }

    private boolean storeCliField(Map<String, String> fields, String key, String value, TextArea output) {
        if (value.trim().isEmpty()) {
            output.appendText("Field --" + key + " requires a value.\n");
            return false;
        }
        if (fields.putIfAbsent(key, value.trim()) != null) {
            output.appendText("Duplicate field: --" + key + "\n");
            return false;
        }
        return true;
    }

    private Integer parseCliInteger(String text, TextArea output) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            output.appendText("Expected a whole number.\n");
            return null;
        }
    }

    private BigDecimal parseCliDecimal(String text, TextArea output) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException exception) {
            output.appendText("Expected a valid price.\n");
            return null;
        }
    }

    private LocalDate parseCliDate(String text, TextArea output) {
        if (text == null) return null;
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (RuntimeException exception) {
            output.appendText("Expiry must use dd-MM-yyyy.\n");
            return null;
        }
    }

    private void appendCommandResult(TextArea output, CommandResult result, boolean undo) {
        output.appendText(result.message() == null
                ? (undo ? "Undo" : "Redo") + " applied.\n"
                : result.message().trim() + "\n");
        if (result.message() == null) refreshCurrentView();
    }

    private void appendMutationResult(TextArea output, String message, String successMessage) {
        output.appendText((message == null ? successMessage : message.trim()) + "\n");
        if (message == null) refreshCurrentView();
    }

    private void appendItems(TextArea output, List<InventoryItem> items) {
        if (items.isEmpty()) {
            output.appendText("No matching items.\n");
            return;
        }
        for (InventoryItem item : items) {
            output.appendText(item.getDisplayName() + " | SKU " + item.getSku()
                    + " | Qty " + item.getTotalQuantity() + "\n");
        }
    }

    private void appendExpiringItems(TextArea output, List<ExpiringItem> items) {
        if (items.isEmpty()) {
            output.appendText("No matching batches.\n");
            return;
        }
        for (ExpiringItem item : items) {
            output.appendText(item.item().getDisplayName() + " | batches " + item.batches().size() + "\n");
        }
    }

    /** Refreshes card metrics and table data according to the current quick filter. */
    private void refreshCurrentView() {
        refreshMetrics();

        switch (currentViewMode) {
        case ALL:
            applyListResult(controller.listItems(false));
            showStatus("Showing all items");
            break;
        case DEPLETED:
            applyListResult(controller.listItems(true));
            showStatus("Showing depleted items");
            break;
        case EXPIRED:
            applyExpiringResult(controller.listExpiredBatches(), "Showing expired batches");
            break;
        case EXPIRING:
            applyExpiringResult(controller.listExpiringBatches(expiringInDays),
                    "Showing batches expiring in " + expiringInDays + " days");
            break;
        default:
            break;
        }
    }

    /** Refreshes dashboard metrics from the full inventory snapshot. */
    private void refreshMetrics() {
        ListQueryResult allItemsResult = controller.listItems(false);
        List<InventoryItem> items = allItemsResult.items();

        int totalItems = items.size();
        int totalQuantity = items.stream().mapToInt(InventoryItem::getTotalQuantity).sum();
        BigDecimal totalCost = items.stream().map(InventoryItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ExpiringBatchQueryResult expiring = controller.listExpiringBatches(7);
        int expiringCount = expiring.items().stream().mapToInt(item -> item.batches().size()).sum();

        trackedItemsValue.setText(String.valueOf(totalItems));
        totalStockValue.setText(String.valueOf(totalQuantity));
        inventoryCostValue.setText("$" + formatPrice(totalCost));
        expiringSoonValue.setText(String.valueOf(expiringCount));
    }

    /** Applies list query results to the center table and details panel. */
    private void applyListResult(ListQueryResult result) {
        if (result.message() != null) {
            inventoryRows.clear();
            clearDetails();
            showStatus(result.message().trim());
            return;
        }
        List<InventoryRow> rows = result.items().stream().map(this::toInventoryRow).toList();
        replaceInventoryRows(rows);
    }

    /** Applies expiring/expired results to the center table and details panel. */
    private void applyExpiringResult(ExpiringBatchQueryResult result, String successStatus) {
        if (result.message() != null) {
            inventoryRows.clear();
            clearDetails();
            showStatus(result.message().trim());
            return;
        }
        List<InventoryRow> rows = result.items().stream().map(this::toInventoryRow).toList();
        replaceInventoryRows(rows);
        showStatus(successStatus);
    }

    /** Updates table content while preserving a sensible initial selection. */
    private void replaceInventoryRows(List<InventoryRow> rows) {
        inventoryRows.setAll(rows);
        if (rows.isEmpty()) {
            clearDetails();
            return;
        }
        inventoryTable.getSelectionModel().selectFirst();
        bindDetailPanel(rows.get(0));
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
        inventoryRows.setAll(List.of(row));
        inventoryTable.getSelectionModel().selectFirst();
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
        selectedItemLabel.setText(row.itemName());
        selectedSkuLabel.setText(row.sku());
        selectedCategoryLabel.setText(row.category());
        selectedQuantityLabel.setText(String.valueOf(row.totalQuantity()));
        selectedCostLabel.setText(formatPrice(row.inventoryCost()));
        detailRows.setAll(row.batches());
    }

    /** Clears the details panel when there is no selected row. */
    private void clearDetails() {
        selectedItemLabel.setText("No item selected");
        selectedSkuLabel.setText("-");
        selectedCategoryLabel.setText("-");
        selectedQuantityLabel.setText("-");
        selectedCostLabel.setText("-");
        detailRows.clear();
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
        Integer quantity = parseIntConditional(quantityField.getText(), value -> value > 0, "Quantity must be a positive whole number.");
        BigDecimal unitPrice = parseNonNegativeDecimal(priceField.getText(), "Unit price must be a non-negative number.");
        LocalDate expiry = expiryPicker.getValue();
        String upc = upcField.getText().trim().isEmpty() ? null : upcField.getText().trim();

        if (itemName.isEmpty() || sku.isEmpty() || invoice.isEmpty() || quantity == null || unitPrice == null) {
            showWarning("Item, SKU, invoice, quantity, and unit price are required.");
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
        showSaleBreakdown(result);
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
        if (identifier.isEmpty() || (extraFieldLabel != null && extraValue.isEmpty())) {
            showWarning("Required fields cannot be empty.");
            return null;
        }

        int quantity = 0;
        if (numericExtra) {
            Integer parsed = parseIntConditional(extraValue, value -> value > 0, "Quantity must be a positive whole number.");
            if (parsed == null) {
                return null;
            }
            quantity = parsed;
        }

        return new IdentifierInput(identifier, byItem.isSelected(), quantity, extraValue);
    }

    /** Shows sold-batch details for successful sell actions. */
    private void showSaleBreakdown(SellItemResult result) {
        StringBuilder content = new StringBuilder();
        for (SoldBatch soldBatch : result.soldBatches()) {
            content.append("Invoice ")
                    .append(soldBatch.invoiceNumber())
                    .append(": quantity ")
                    .append(soldBatch.quantity())
                    .append("\n");
        }
        if (content.length() == 0) {
            content.append("No batch lines were returned.");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sale Complete");
        alert.setHeaderText("Sold " + result.item().getDisplayName());
        TextArea details = new TextArea(content.toString());
        details.setWrapText(true);
        details.setEditable(false);
        details.setPrefRowCount(6);
        alert.getDialogPane().setContent(details);
        alert.showAndWait();
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

    /** Converts an expiring-item view into a table row with only relevant batches. */
    private InventoryRow toInventoryRow(ExpiringItem expiringItem) {
        InventoryItem item = expiringItem.item();
        List<BatchRow> rows = expiringItem.batches().stream().map(this::toBatchRow).toList();

        int listedQuantity = rows.stream().mapToInt(BatchRow::quantity).sum();
        BigDecimal listedCost = rows.stream().map(BatchRow::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryRow(item.getDisplayName(), item.getSku(),
                item.getCategory().name().toLowerCase(Locale.ROOT),
                listedQuantity, listedCost, rows);
    }

    /** Converts one domain batch object into detail-table row data. */
    private BatchRow toBatchRow(Batch batch) {
        LocalDate expiry = batch instanceof PerishableBatch
                ? ((PerishableBatch) batch).getExpiryDate() : null;
        return new BatchRow(batch.getInvoiceNumber(), batch.getQuantity(), batch.getUnitPrice(), expiry, batch.getUpc());
    }

    /** Parses and validates positive integers used by quantity inputs. */
    private Integer parseIntConditional(String text, Predicate<Integer> validator, String message) {
        try {
            int value = Integer.parseInt(text.trim());
            if (validator.test(value)) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Validation feedback is returned below.
        }
        showWarning(message);
        return null;
    }

    /** Parses and validates non-negative decimal values used by unit price inputs. */
    private BigDecimal parseNonNegativeDecimal(String text, String message) {
        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.signum() >= 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Validation feedback is returned below.
        }
        showWarning(message);
        return null;
    }

    /** Formats monetary values with two decimal places. */
    private String formatPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Shows a warning dialog and keeps the user on the current screen. */
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Stockie");
        alert.setHeaderText("Action could not be completed");
        alert.setContentText(message);
        alert.showAndWait();
        showStatus(message);
    }

    /** Shows a two-option confirmation dialog. */
    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    /** Updates transient status text under the top action row. */
    private void showStatus(String text) {
        statusLabel.setText(text == null ? "" : text.trim());
    }

    /** Supported table filter modes. */
    private enum ViewMode {
        ALL,
        DEPLETED,
        EXPIRED,
        EXPIRING
    }

    /** Stores parsed identifier form values for action dialogs. */
    private record IdentifierInput(String identifier, boolean byItem, int quantity, String extraFieldValue) { }

    /** Represents one row in the inventory master table. */
    private record InventoryRow(String itemName, String sku, String category, int totalQuantity,
                                BigDecimal inventoryCost, List<BatchRow> batches) { }

    /** Represents one batch line in the detail table. */
    private record BatchRow(String invoice, int quantity, BigDecimal unitPrice,
                            LocalDate expiry, String upc) {
        private BigDecimal totalCost() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
