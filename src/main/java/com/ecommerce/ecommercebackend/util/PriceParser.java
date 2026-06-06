package com.ecommerce.ecommercebackend.util;

/**
 * Parses the price strings produced by the TGDD crawler into plain VND integers.
 *
 * <p>Handles both formats seen in the data:
 * <ul>
 *   <li>Vietnamese grouped currency — {@code "16.390.000₫"} → {@code 16390000}</li>
 *   <li>Plain decimal numbers — {@code "16490000.0"} → {@code 16490000}</li>
 * </ul>
 * The distinction is made from the separators: multiple dots mean thousands
 * grouping, while a single dot followed by a non-3-digit fraction is a decimal.</p>
 */
public final class PriceParser {

    private PriceParser() {
    }

    /**
     * @param raw a formatted price; may be {@code null}/blank
     * @return the numeric VND value, or {@code null} when no number is present
     */
    public static Long parse(String raw) {
        if (raw == null) {
            return null;
        }
        // Keep only digits and the separators we need to interpret.
        String s = raw.replaceAll("[^0-9.,]", "");
        if (s.isEmpty()) {
            return null;
        }

        boolean hasDot = s.indexOf('.') >= 0;
        boolean hasComma = s.indexOf(',') >= 0;

        try {
            if (hasDot && hasComma) {
                // e.g. "1.234.567,89" → '.' = thousands, ',' = decimal
                s = s.replace(".", "").replace(",", ".");
                return (long) Double.parseDouble(s);
            }
            if (hasDot) {
                return interpretSingleSeparator(s, '.');
            }
            if (hasComma) {
                return interpretSingleSeparator(s, ',');
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Interprets a string that uses exactly one kind of separator {@code sep}.
     * Multiple occurrences ⇒ thousands grouping; a single occurrence with a
     * 3-digit fraction ⇒ also thousands ("1.000"); otherwise it is a decimal.
     */
    private static Long interpretSingleSeparator(String s, char sep) {
        long count = s.chars().filter(c -> c == sep).count();
        int idx = s.indexOf(sep);
        String fraction = s.substring(idx + 1);

        boolean thousands = count > 1 || fraction.length() == 3;
        if (thousands) {
            return Long.parseLong(s.replace(String.valueOf(sep), ""));
        }
        // Decimal: normalise the separator to '.' and truncate the fraction.
        return (long) Double.parseDouble(s.replace(sep, '.'));
    }
}
