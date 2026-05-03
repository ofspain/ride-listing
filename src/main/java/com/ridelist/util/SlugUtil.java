package com.ridelist.util;

import com.ridelist.model.ListingType;
import com.ridelist.model.VehicleType;

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

    /**
     * Generates a URL-safe slug from a listing title.
     * Used for the slug portion of the listing URL only.
     * Uniqueness is guaranteed by listing_number prefix, not this slug.
     *
     * @param title the listing title
     * @return URL-safe slug, or "listing" if title is null/blank
     */
    public static String toListingSlug(String title) {
        if (title == null || title.isBlank()) {
            return "listing";
        }

        String slug = title
                .toLowerCase()
                .transform(s -> Normalizer.normalize(s, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", ""))
                .replace("&", "and")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > 80) {
            slug = slug.substring(0, 80).replaceAll("-[^-]*$", "");
        }

        return slug.isEmpty() ? "listing" : slug;
    }

    /**
     * Generates the full URL path segment for a listing.
     * Format: {listing-number}-{title-slug}
     *
     * @param listingNumber the listing number
     * @param title the listing title
     * @return URL path segment like "10247-honda-cb300r-tokunbo-2023"
     */
    public static String toListingRef(Integer listingNumber, String title) {
        return listingNumber + "-" + toListingSlug(title);
    }

    /**
     * Extracts listing number from a URL ref segment.
     *
     * @param ref the URL segment like "10247-honda-cb300r-tokunbo"
     * @return the listing number, or null if invalid
     */
    public static Integer extractListingNumber(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        String[] parts = ref.split("-", 2);
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Generates the category path segment from listing type and vehicle type.
     *
     * @param listingType the listing type (VEHICLE or PART)
     * @param vehicleType the vehicle type (MOTORCYCLE, TRICYCLE, BICYCLE, or null)
     * @return category path like "motorcycles", "tricycles", "bicycles", "vehicles", or "spare-parts"
     */
    public static String toCategoryPath(ListingType listingType, VehicleType vehicleType) {
        if (listingType == ListingType.PART) {
            return "spare-parts";
        }
        if (vehicleType == null) {
            return "vehicles";
        }
        return switch (vehicleType) {
            case MOTORCYCLE -> "motorcycles";
            case TRICYCLE -> "tricycles";
            case BICYCLE -> "bicycles";
        };
    }

    /**
     * Builds the full SEO URL path for a listing.
     *
     * @param listingType the listing type
     * @param vehicleType the vehicle type (nullable)
     * @param stateSlug the state slug (nullable)
     * @param axisSlug the axis slug (nullable)
     * @param areaSlug the area slug (nullable)
     * @param listingNumber the listing number
     * @param title the listing title
     * @return full URL path like "/motorcycles/lagos/mainland/ikeja/10247-honda-cb300r-tokunbo-2023"
     */
    public static String toListingUrl(
            ListingType listingType,
            VehicleType vehicleType,
            String stateSlug,
            String axisSlug,
            String areaSlug,
            Integer listingNumber,
            String title) {

        StringBuilder url = new StringBuilder();

        url.append("/").append(toCategoryPath(listingType, vehicleType));

        if (stateSlug != null && !stateSlug.isBlank()) {
            url.append("/").append(stateSlug);

            if (axisSlug != null && !axisSlug.isBlank()) {
                url.append("/").append(axisSlug);

                if (areaSlug != null && !areaSlug.isBlank()) {
                    url.append("/").append(areaSlug);
                }
            }
        }

        url.append("/").append(toListingRef(listingNumber, title));

        return url.toString();
    }

    /**
     * Resolution result for category path lookup.
     */
    public record CategoryResolution(
            ListingType listingType,
            VehicleType vehicleType
    ) {}

    /**
     * Resolves a category path slug to listing type and vehicle type.
     *
     * @param categoryPath the category path (e.g., "motorcycles", "spare-parts")
     * @return CategoryResolution with listingType and vehicleType, or null if invalid
     */
    public static CategoryResolution resolveCategoryPath(String categoryPath) {
        if (categoryPath == null || categoryPath.isBlank()) {
            return null;
        }
        return switch (categoryPath.toLowerCase()) {
            case "motorcycles" -> new CategoryResolution(ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            case "tricycles" -> new CategoryResolution(ListingType.VEHICLE, VehicleType.TRICYCLE);
            case "bicycles" -> new CategoryResolution(ListingType.VEHICLE, VehicleType.BICYCLE);
            case "vehicles" -> new CategoryResolution(ListingType.VEHICLE, null);
            case "spare-parts", "parts", "accessories" -> new CategoryResolution(ListingType.PART, null);
            default -> null;
        };
    }
}
