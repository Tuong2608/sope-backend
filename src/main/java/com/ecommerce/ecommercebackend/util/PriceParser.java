package com.ecommerce.ecommercebackend.util;

/**
 * Parses the formatted VND price strings produced by the TGDD crawler
 * (e.g. "7.890.000₫") into plain numeric values.
 */
public final class PriceParser {

    private PriceParser() {
    }

    /**
     * Strips every non-digit character and parses the remainder as VND.
     *
     * @param raw formatted price such as "7.890.000₫"; may be {@code null}/blank
     * @return the numeric price, or {@code null} when no digits are present
     */
    public static Long parse(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        return Long.parseLong(digits);
    }
}
