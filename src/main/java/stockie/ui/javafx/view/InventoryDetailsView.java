package stockie.ui.javafx.view;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import stockie.ui.javafx.util.BatchRow;
import stockie.ui.javafx.util.FxFormatter;
import stockie.ui.javafx.util.InventoryRow;

/** Displays the selected inventory item and its batches. */
public final class InventoryDetailsView {
    private final ObservableList<BatchRow> batches = FXCollections.observableArrayList();
    private final Label itemLabel = new Label("No item selected");
    private final Label skuLabel = new Label("-");
    private final Label categoryLabel = new Label("-");
    private final Label quantityLabel = new Label("-");
    private final Label costLabel = new Label("-");
    private final TableView<BatchRow> table = new TableView<>();

    public InventoryDetailsView() {
        table.setItems(batches);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<BatchRow, String> invoiceColumn = new TableColumn<>("Invoice");
        invoiceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().invoice()));
        TableColumn<BatchRow, Number> quantityColumn = new TableColumn<>("Qty");
        quantityColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().quantity()));
        TableColumn<BatchRow, Number> priceColumn = new TableColumn<>("Unit Price");
        priceColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitPrice()));
        TableColumn<BatchRow, String> expiryColumn = new TableColumn<>("Expiry");
        expiryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                FxFormatter.date(cell.getValue().expiry())));
        TableColumn<BatchRow, String> upcColumn = new TableColumn<>("UPC");
        upcColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                FxFormatter.optionalText(cell.getValue().upc())));
        table.getColumns().setAll(invoiceColumn, quantityColumn, priceColumn, expiryColumn, upcColumn);
    }

    public VBox node() {
        Label header = new Label("Item Details");
        header.getStyleClass().add("panel-header");
        GridPane infoGrid = new GridPane();
        infoGrid.setVgap(6);
        infoGrid.setHgap(10);
        infoGrid.addRow(0, new Label("Item"), itemLabel);
        infoGrid.addRow(1, new Label("SKU"), skuLabel);
        infoGrid.addRow(2, new Label("Category"), categoryLabel);
        infoGrid.addRow(3, new Label("Total Qty"), quantityLabel);
        infoGrid.addRow(4, new Label("Inventory Cost"), costLabel);
        Label batchesHeader = new Label("Batches");
        batchesHeader.getStyleClass().add("panel-header");
        VBox wrapper = new VBox(8, header, infoGrid, new Separator(), batchesHeader, table);
        wrapper.getStyleClass().add("content-panel");
        VBox.setVgrow(table, Priority.ALWAYS);
        return wrapper;
    }

    public void show(InventoryRow row) {
        itemLabel.setText(row.itemName());
        skuLabel.setText(row.sku());
        categoryLabel.setText(row.category());
        quantityLabel.setText(String.valueOf(row.totalQuantity()));
        costLabel.setText(FxFormatter.price(row.inventoryCost()));
        batches.setAll(row.batches());
    }

    public void clear() {
        itemLabel.setText("No item selected");
        skuLabel.setText("-");
        categoryLabel.setText("-");
        quantityLabel.setText("-");
        costLabel.setText("-");
        batches.clear();
    }
}
