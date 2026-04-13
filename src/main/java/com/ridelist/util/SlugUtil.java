package com.ridelist.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for generating and parsing URL-safe slugs.
 */
public final class SlugUtil {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    private SlugUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a string to a URL-safe slug.
     * <p>
     * - Converts to lowercase
     * - Normalizes unicode characters
     * - Removes special characters
     * - Replaces spaces with hyphens
     * - Removes consecutive hyphens
     * - Trims leading/trailing hyphens
     *
     * @param input the string to convert
     * @return URL-safe slug
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Normalize unicode characters (e.g., é -> e)
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convert to lowercase
        String lowercase = normalized.toLowerCase();

        // Remove special characters (keep alphanumeric, spaces, and hyphens)
        String cleaned = NON_ALPHANUMERIC.matcher(lowercase).replaceAll("");

        // Replace whitespace with hyphens
        String hyphenated = WHITESPACE.matcher(cleaned).replaceAll("-");

        // Remove consecutive hyphens
        String singleHyphens = MULTIPLE_HYPHENS.matcher(hyphenated).replaceAll("-");

        // Trim leading/trailing hyphens
        return singleHyphens.replaceAll("^-+|-+$", "");
    }

    /**
     * Converts a slug back to a readable string.
     * <p>
     * - Replaces hyphens with spaces
     * - Capitalizes each word
     *
     * @param slug the slug to convert
     * @return readable string with capitalized words
     */
    public static String fromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return "";
        }

        return Arrays.stream(slug.split("-"))
                .filter(word -> !word.isEmpty())
                .map(SlugUtil::capitalize)
                .collect(Collectors.joining(" "));
    }

    private static String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
}
