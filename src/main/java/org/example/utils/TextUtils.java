package org.example.utils;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {

    private TextUtils() {
    }

    /**
     * Removes characters not allowed in file names (Windows / Unix).
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}

