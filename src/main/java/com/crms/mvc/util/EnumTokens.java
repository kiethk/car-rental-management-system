package com.crms.mvc.util;

/**
 * Views and forms pass enum values around as lowercase text ("pending_payment", "sedan"), while entities and the
 * database use the enum itself. This is the one place that converts between the two.
 */
public final class EnumTokens {
    private EnumTokens() {}

    /** The lowercase view token of an enum value; null stays null. */
    public static String of(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase();
    }

    /** The enum name as it is stored (upper case); null stays null. */
    public static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    /** True when the token (any case) names the given enum value. */
    public static boolean is(Enum<?> value, String token) {
        return value != null && token != null && value.name().equalsIgnoreCase(token.trim());
    }
}
