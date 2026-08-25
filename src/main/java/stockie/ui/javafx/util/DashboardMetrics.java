package stockie.ui.javafx.util;

import java.math.BigDecimal;

/** Values displayed by the inventory dashboard summary cards. */
public record DashboardMetrics(int trackedItems, int totalStock, BigDecimal inventoryCost,
                               int expiringSoon) {
}
