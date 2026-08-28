package stockie.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {
    @Test
    void normalizeMixedCaseReturnsLowerCaseText() {
        assertEquals("sku-abc123", TextNormalizer.normalize("Sku-AbC123"));
    }

    @Test
    void normalizeWhitespaceAndPunctuationPreservesCharacters() {
        assertEquals("  item name!  ", TextNormalizer.normalize("  ITEM NAME!  "));
    }

    @Test
    void normalizeLocaleSensitiveTextUsesLocaleIndependentLowerCase() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals("i", TextNormalizer.normalize("I"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void normalizeNullValueThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> TextNormalizer.normalize(null));
    }
}
