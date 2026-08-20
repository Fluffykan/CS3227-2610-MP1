package stockie.util;

import java.util.Locale;

/** Normalizes user-entered identifiers for case-insensitive matching. */
public final class TextNormalizer {
    private TextNormalizer() { }

    /** Returns a lower-case representation using a locale-independent rule. */
    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
