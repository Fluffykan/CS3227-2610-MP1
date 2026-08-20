package stockie.application.result;

import java.util.List;

/** Result returned when querying items with batches that expire within a requested number of days. */
public record ExpiringBatchQueryResult(List<ExpiringItem> items, String message) { }
