package com.biswamit.cache.service;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * A service layer that provides a clean, business-oriented API for the cache.
 * It encapsulates the logic of creating and parsing composite keys, hiding
 * the implementation detail from the rest of the application.
 */
@Service
public class TimeExpiringCacheService {

    private final Cache<String, Object> cache;
    private static final char DELIMITER = ':';

    public TimeExpiringCacheService(Cache<String, Object> cache) {
        this.cache = cache;
    }

    /**
     * Puts a value into the cache under a group and a specific sub-key.
     *
     * @param groupKey The main key, e.g., "matrix.event.setting".
     * @param subKey The specific identifier, e.g., a tenant ID.
     * @param value The object to cache.
     */
    public void put(String groupKey, String subKey, Object value) {
        String compositeKey = groupKey + DELIMITER + subKey;
        cache.put(compositeKey, value);
    }

    /**
     * Puts a value into the cache that only has a group key.
     *
     * @param groupKey The main key, e.g., "some.simple.flag".
     * @param value The object to cache.
     */
    public void put(String groupKey, Object value) {
        cache.put(groupKey, value);
    }

    /**
     * Retrieves a value from the cache.
     *
     * @param groupKey The main key.
     * @param subKey The specific identifier.
     * @param type The class of the object to cast to.
     * @return An Optional containing the value if present and of the correct type, otherwise empty.
     */
    public <T> Optional<T> get(String groupKey, String subKey, Class<T> type) {
        String compositeKey = groupKey + DELIMITER + subKey;
        Object value = cache.getIfPresent(compositeKey);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Retrieves a value from the cache that only has a group key.
     */
    public <T> Optional<T> get(String groupKey, Class<T> type) {
        Object value = cache.getIfPresent(groupKey);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }
}