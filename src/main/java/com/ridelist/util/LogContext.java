package com.ridelist.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing logging context (MDC).
 * Use this to add contextual information to logs within a request scope.
 */
public final class LogContext {

    public static final String USER_ID = "userId";
    public static final String LISTING_ID = "listingId";
    public static final String TRACE_ID = "traceId";

    private LogContext() {
        // Utility class
    }

    /**
     * Set the current user ID in the logging context.
     */
    public static void setUserId(UUID userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }

    /**
     * Set the current user ID in the logging context.
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.isBlank()) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Set the current listing ID in the logging context.
     */
    public static void setListingId(UUID listingId) {
        if (listingId != null) {
            MDC.put(LISTING_ID, listingId.toString());
        }
    }

    /**
     * Set the current listing ID in the logging context.
     */
    public static void setListingId(String listingId) {
        if (listingId != null && !listingId.isBlank()) {
            MDC.put(LISTING_ID, listingId);
        }
    }

    /**
     * Clear the user ID from the logging context.
     */
    public static void clearUserId() {
        MDC.remove(USER_ID);
    }

    /**
     * Clear the listing ID from the logging context.
     */
    public static void clearListingId() {
        MDC.remove(LISTING_ID);
    }

    /**
     * Clear all custom context keys (preserves traceId set by filter).
     */
    public static void clearAll() {
        MDC.remove(USER_ID);
        MDC.remove(LISTING_ID);
    }

    /**
     * Get the current trace ID.
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }
}
