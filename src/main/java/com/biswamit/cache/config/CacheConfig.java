package com.biswamit.cache.config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration to set up the Caffeine Cache bean.
 * The cache is configured with a flattened key structure (String) and stores generic Objects.
 */
@Configuration
@EnableConfigurationProperties(CacheTtlProperties.class)
public class CacheConfig {

    @Bean
    public Cache<String, Object> timeExpiringCache(CacheTtlProperties ttlProperties) {
        return Caffeine.newBuilder()
                .expireAfter(new PerKeyExpiryPolicy(ttlProperties))
                .build();
    }
}