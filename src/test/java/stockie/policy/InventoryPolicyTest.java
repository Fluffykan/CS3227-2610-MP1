package stockie.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InventoryPolicyTest {
    @Test
    void maxItems_isPositiveAndMatchesConfiguredLimit() {
        assertEquals(200_000, InventoryPolicy.MAX_ITEMS);
    }
}
