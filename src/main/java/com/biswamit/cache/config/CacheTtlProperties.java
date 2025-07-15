package com.biswamit.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps application.properties to a configuration object for cache TTLs.
 * This class holds the default TTL and a map of specific TTLs for cache keys.
 */
@ConfigurationProperties(prefix = "app.cache.ttl")
public class CacheTtlProperties {

    private Duration defaultTtl = Duration.ofMinutes(30);
    private Map<String, Duration> keys = new HashMap<>();

    // Standard Getters and Setters
    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, Duration> keys) {
        this.keys = keys;
    }
}