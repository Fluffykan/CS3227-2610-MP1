package stockie.application.exception;

/** Signals that an inventory operation refers to an unknown item key. */
public final class ItemNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ItemNotFoundException(String itemKey) {
        super("Item not found: " + itemKey);
    }
}
