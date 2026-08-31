package stockie.ui.javafx.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import stockie.ui.javafx.util.BatchRow;
import stockie.ui.javafx.util.InventoryRow;

class InventoryDetailsViewTest {
    @BeforeAll
    static void setUpJavaFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void showDisplaysItemDetailsFormattedBatchValuesAndColumns() throws InterruptedException {
        InventoryDetailsView view = FxTestSupport.call(InventoryDetailsView::new);
        BatchRow batch = new BatchRow("INV-1", 2, new BigDecimal("3.50"),
                LocalDate.of(2026, 2, 28), "UPC-1");
        InventoryRow row = new InventoryRow("Milk", "MILK", "perishable", 2,
                new BigDecimal("7.00"), List.of(batch));

        FxTestSupport.runAndWait(() -> view.show(row));

        VBox node = FxTestSupport.call(view::node);
        GridPane infoGrid = (GridPane) node.getChildren().get(1);
        assertEquals("Milk", ((Label) infoGrid.getChildren().get(1)).getText());
        assertEquals("MILK", ((Label) infoGrid.getChildren().get(3)).getText());
        assertEquals("perishable", ((Label) infoGrid.getChildren().get(5)).getText());
        assertEquals("2", ((Label) infoGrid.getChildren().get(7)).getText());
        assertEquals("7.00", ((Label) infoGrid.getChildren().get(9)).getText());

        TableView<BatchRow> table = tableFrom(node);
        assertEquals(1, table.getItems().size());
        assertEquals(List.of("Invoice", "Qty", "Unit Price", "Expiry", "UPC"),
                table.getColumns().stream().map(column -> column.getText()).toList());
        assertEquals("INV-1", table.getColumns().get(0).getCellObservableValue(batch).getValue());
        assertEquals(2, table.getColumns().get(1).getCellObservableValue(batch).getValue());
        assertEquals(new BigDecimal("3.50"), table.getColumns().get(2)
                .getCellObservableValue(batch).getValue());
        assertEquals("28-02-2026", table.getColumns().get(3).getCellObservableValue(batch).getValue());
        assertEquals("UPC-1", table.getColumns().get(4).getCellObservableValue(batch).getValue());
    }

    @Test
    void clearRestoresEmptyDetailsState() throws InterruptedException {
        InventoryDetailsView view = FxTestSupport.call(InventoryDetailsView::new);
        FxTestSupport.runAndWait(view::clear);

        VBox node = FxTestSupport.call(view::node);
        GridPane infoGrid = (GridPane) node.getChildren().get(1);
        assertEquals("No item selected", ((Label) infoGrid.getChildren().get(1)).getText());
        assertEquals("-", ((Label) infoGrid.getChildren().get(3)).getText());
        assertEquals("-", ((Label) infoGrid.getChildren().get(5)).getText());
        assertEquals("-", ((Label) infoGrid.getChildren().get(7)).getText());
        assertEquals("-", ((Label) infoGrid.getChildren().get(9)).getText());
        assertEquals(0, tableFrom(node).getItems().size());
    }

    @SuppressWarnings("unchecked")
    private static TableView<BatchRow> tableFrom(VBox node) {
        return (TableView<BatchRow>) node.getChildren().get(4);
    }
}
