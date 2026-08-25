package stockie.ui.javafx.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Provides formatting rules shared by JavaFX presentation components. */
public final class FxFormatter {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private FxFormatter() {
    }

    public static String price(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String date(LocalDate value) {
        return value == null ? "-" : DATE_FORMAT.format(value);
    }

    public static String optionalText(String value) {
        return value == null ? "-" : value;
    }
}
