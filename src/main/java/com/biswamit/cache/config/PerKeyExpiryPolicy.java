package com.biswamit.cache.config;

import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Custom Expiry policy for Caffeine that implements hierarchical TTL resolution.
 * It resolves TTL based on:
 * 1. A highly specific key (e.g., "group.subKey").
 * 2. A group-level key (e.g., "group").
 * 3. A global default.
 */
public class PerKeyExpiryPolicy implements Expiry<String, Object> {

    private static final Logger logger = LoggerFactory.getLogger(PerKeyExpiryPolicy.class);
    private final CacheTtlProperties ttlProperties;
    private static final char DELIMITER = ':';

    public PerKeyExpiryPolicy(CacheTtlProperties ttlProperties) {
        this.ttlProperties = ttlProperties;
    }

    @Override
    public long expireAfterCreate(@NonNull String compositeKey, @NonNull Object value, long currentTime) {
        Duration duration = resolveDuration(compositeKey);
        logger.debug("Cache entry created for key '{}' with TTL: {}", compositeKey, duration);
        return duration.toNanos();
    }

    @Override
    public long expireAfterUpdate(@NonNull String compositeKey, @NonNull Object value, long currentTime, long currentDuration) {
        // On update, re-calculate the duration as properties might have changed
        Duration duration = resolveDuration(compositeKey);
        logger.debug("Cache entry updated for key '{}'. New TTL: {}", compositeKey, duration);
        return duration.toNanos();
    }

    @Override
    public long expireAfterRead(@NonNull String key, @NonNull Object value, long currentTime, long currentDuration) {
        // Do not change expiration on read access
        return currentDuration;
    }

    private Duration resolveDuration(String compositeKey) {
        // 1. Check for the most specific key TTL (e.g., "group:subKey" -> "group.subKey" in properties)
        String specificPropertyKey = compositeKey.replace(DELIMITER, '.');
        Duration specificTtl = ttlProperties.getKeys().get(specificPropertyKey);
        if (specificTtl != null) {
            return specificTtl;
        }

        // 2. Check for the group-level key TTL
        int delimiterIndex = compositeKey.indexOf(DELIMITER);
        if (delimiterIndex > 0) {
            String groupPropertyKey = compositeKey.substring(0, delimiterIndex);
            Duration groupTtl = ttlProperties.getKeys().get(groupPropertyKey);
            if (groupTtl != null) {
                return groupTtl;
            }
        }

        // 3. Fallback to the global default TTL
        return ttlProperties.getDefaultTtl();
    }
}
