package stockie.ui.javafx;

import java.util.Optional;
import java.util.function.Consumer;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import stockie.application.result.SellItemResult;
import stockie.application.result.SoldBatch;

/** Provides shared JavaFX dialogs used by inventory actions. */
public final class FxDialogs {
    /** Shows an action warning and updates the transient status message. */
    public void showWarning(String message, Consumer<String> statusConsumer) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Stockie");
        alert.setHeaderText("Action could not be completed");
        alert.setContentText(message);
        alert.showAndWait();
        statusConsumer.accept(message);
    }

    /** Shows a confirmation dialog and returns whether the user accepted it. */
    public boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    /** Shows the batches affected by a successful sale. */
    public void showSaleBreakdown(SellItemResult result) {
        StringBuilder content = new StringBuilder();
        for (SoldBatch soldBatch : result.soldBatches()) {
            content.append("Invoice ").append(soldBatch.invoiceNumber())
                    .append(": quantity ").append(soldBatch.quantity()).append("\n");
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
}
