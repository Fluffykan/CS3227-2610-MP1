package stockie.ui.javafx.view;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import stockie.ui.javafx.util.InventoryRow;

/** Displays inventory rows and reports selection changes to the screen coordinator. */
public final class InventoryTableView {
    private final ObservableList<InventoryRow> rows = FXCollections.observableArrayList();
    private final TableView<InventoryRow> table = new TableView<>();

    /** Creates the inventory table and connects its selection callback. */
    public InventoryTableView(Consumer<InventoryRow> selectionConsumer) {
        table.setItems(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<InventoryRow, String> itemColumn = new TableColumn<>("Item");
        itemColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().itemName()));
        TableColumn<InventoryRow, String> skuColumn = new TableColumn<>("SKU");
        skuColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().sku()));
        TableColumn<InventoryRow, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category()));
        TableColumn<InventoryRow, Number> quantityColumn = new TableColumn<>("Total Qty");
        quantityColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().totalQuantity()));
        TableColumn<InventoryRow, Number> costColumn = new TableColumn<>("Inventory Cost");
        costColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().inventoryCost()));
        costColumn.setComparator(Comparator.comparingDouble(Number::doubleValue));
        table.getColumns().setAll(itemColumn, skuColumn, categoryColumn, quantityColumn, costColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow != null) {
                selectionConsumer.accept(newRow);
            }
        });
    }

    /** Returns the JavaFX node containing the inventory table. */
    public VBox node() {
        Label header = new Label("Inventory");
        header.getStyleClass().add("panel-header");
        VBox wrapper = new VBox(8, header, table);
        wrapper.getStyleClass().add("content-panel");
        VBox.setVgrow(table, Priority.ALWAYS);
        return wrapper;
    }

    /** Replaces the table rows and selects the first row when available. */
    public void setRows(List<InventoryRow> newRows) {
        rows.setAll(newRows);
        if (!newRows.isEmpty()) {
            table.getSelectionModel().selectFirst();
        }
    }

    /** Selects the first row in the table, if one exists. */
    public void selectFirst() {
        table.getSelectionModel().selectFirst();
    }
}
