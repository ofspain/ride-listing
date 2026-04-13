package com.ridelist.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A simple, thread-safe in-memory cache implementation using ConcurrentHashMap.
 *
 * Features:
 * - Lazy loading via computeIfAbsent
 * - Optional TTL (time-to-live) support
 * - Manual eviction methods
 *
 * Intended for lightweight reference data (locations, categories).
 */
@Component
@Slf4j
public class InMemoryCache {

    private static final long DEFAULT_TTL_HOURS = 10;
    private static final long TTL_MILLIS = DEFAULT_TTL_HOURS * 60 * 60 * 1000;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Get a value from cache, computing it if absent or expired.
     *
     * @param key      the cache key
     * @param supplier the supplier to compute the value if not cached
     * @param <T>      the type of the cached value
     * @return the cached or computed value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> supplier) {
        CacheEntry entry = cache.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                log.debug("Cache miss for key: {}", key);
                T value = supplier.get();
                return new CacheEntry(value, Instant.now().toEpochMilli());
            }
            log.debug("Cache hit for key: {}", key);
            return existing;
        });
        return (T) entry.getValue();
    }

    /**
     * Evict a single entry from the cache.
     *
     * @param key the cache key to evict
     */
    public void evict(String key) {
        cache.remove(key);
        log.debug("Evicted cache key: {}", key);
    }

    /**
     * Evict all entries from the cache.
     */
    public void evictAll() {
        cache.clear();
        log.info("Evicted all cache entries");
    }

    /**
     * Get the current size of the cache.
     *
     * @return number of entries in cache
     */
    public int size() {
        return cache.size();
    }

    /**
     * Check if a key exists in the cache (and is not expired).
     *
     * @param key the cache key
     * @return true if the key exists and is not expired
     */
    public boolean containsKey(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    /**
     * Internal cache entry that holds the value and creation timestamp.
     */
    private static class CacheEntry {
        private final Object value;
        private final long createdAt;

        CacheEntry(Object value, long createdAt) {
            this.value = value;
            this.createdAt = createdAt;
        }

        Object getValue() {
            return value;
        }

        boolean isExpired() {
            return Instant.now().toEpochMilli() - createdAt > TTL_MILLIS;
        }
    }
}
