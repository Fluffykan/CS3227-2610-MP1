package stockie.ui.javafx.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import stockie.ui.javafx.util.InventoryRow;

class InventoryTableViewTest {
    @BeforeAll
    static void setUpJavaFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void nodeContainsExpectedHeaderAndColumns() throws InterruptedException {
        VBox node = FxTestSupport.call(() -> new InventoryTableView(row -> { }).node());

        assertEquals("Inventory", ((Label) node.getChildren().get(0)).getText());
        TableView<InventoryRow> table = tableFrom(node);
        assertEquals(List.of("Item", "SKU", "Category", "Total Qty", "Inventory Cost"),
                table.getColumns().stream().map(column -> column.getText()).toList());
    }

    @Test
    void setRowsReplacesRowsSelectsFirstRowAndClearsEmptyState() throws InterruptedException {
        AtomicReference<InventoryRow> selected = new AtomicReference<>();
        InventoryTableView view = FxTestSupport.call(() -> new InventoryTableView(selected::set));
        VBox node = FxTestSupport.call(view::node);
        TableView<InventoryRow> table = tableFrom(node);
        InventoryRow firstRow = row("Milk", "MILK", 3);
        InventoryRow replacementRow = row("Bread", "BREAD", 0);

        FxTestSupport.runAndWait(() -> view.setRows(List.of(firstRow)));

        assertEquals(1, table.getItems().size());
        assertSame(firstRow, table.getSelectionModel().getSelectedItem());
        assertSame(firstRow, selected.get());

        FxTestSupport.runAndWait(() -> view.setRows(List.of(replacementRow)));

        assertEquals(1, table.getItems().size());
        assertSame(replacementRow, table.getItems().get(0));
        assertSame(replacementRow, table.getSelectionModel().getSelectedItem());
        assertSame(replacementRow, selected.get());

        FxTestSupport.runAndWait(() -> view.setRows(List.of()));

        assertEquals(0, table.getItems().size());
        assertNull(table.getSelectionModel().getSelectedItem());
    }

    @Test
    void columnsExposeValuesFromInventoryRow() throws InterruptedException {
        InventoryTableView view = FxTestSupport.call(() -> new InventoryTableView(row -> { }));
        InventoryRow row = new InventoryRow("Milk", "MILK", "perishable", 3,
                new BigDecimal("10.50"), List.of());
        TableView<InventoryRow> table = FxTestSupport.call(() -> {
            view.setRows(List.of(row));
            return tableFrom(view.node());
        });

        assertEquals("Milk", table.getColumns().get(0).getCellObservableValue(row).getValue());
        assertEquals("MILK", table.getColumns().get(1).getCellObservableValue(row).getValue());
        assertEquals("perishable", table.getColumns().get(2).getCellObservableValue(row).getValue());
        assertEquals(3, table.getColumns().get(3).getCellObservableValue(row).getValue());
        assertEquals(new BigDecimal("10.50"), table.getColumns().get(4)
                .getCellObservableValue(row).getValue());
    }

    private static InventoryRow row(String itemName, String sku, int quantity) {
        return new InventoryRow(itemName, sku, "non_perishable", quantity,
                BigDecimal.TEN, List.of());
    }

    @SuppressWarnings("unchecked")
    private static TableView<InventoryRow> tableFrom(VBox node) {
        return (TableView<InventoryRow>) node.getChildren().get(1);
    }
}
