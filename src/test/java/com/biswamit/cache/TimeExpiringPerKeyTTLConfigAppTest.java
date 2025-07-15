package com.biswamit.cache;

import com.biswamit.cache.config.CacheTtlProperties;
import com.biswamit.cache.config.PerKeyExpiryPolicy;
import com.biswamit.cache.model.TenantMapper;
import com.biswamit.cache.model.TenantEventSetting;
import com.biswamit.cache.service.TimeExpiringCacheService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the TimeExpiringCache.
 * This test class uses @SpringBootTest to load the full application context,
 * including the TTL properties from application.properties.
 *
 * It uses a TestConfiguration to override the default cache bean with one
 * that uses a FakeTicker, allowing for manual time advancement to test expiration.
 */
@SpringBootTest
class TimeExpiringPerKeyTTLConfigAppTest {

    /**
     * TestConfiguration to override the production cache bean.
     * We replace the default system ticker with a FakeTicker that we can control manually.
     */
    @TestConfiguration
    static class CacheTestConfig {

        // The FakeTicker allows us to control time in our tests.
        private final FakeTicker ticker = new FakeTicker();

        @Bean
        public FakeTicker fakeTicker() {
            return ticker;
        }

        @Bean
        @Primary // Ensures this bean is used instead of the production one
        public Cache<String, Object> testCache(CacheTtlProperties ttlProperties) {
            return Caffeine.newBuilder()
                    .expireAfter(new PerKeyExpiryPolicy(ttlProperties))
                    .ticker(ticker) // Use the fake ticker
                    .build();
        }
    }

    @Autowired
    private TimeExpiringCacheService cacheService;

    @Autowired
    private Cache<String, Object> cache;

    @Autowired
    private FakeTicker ticker;

    // Test constants based on application.properties
    private static final String OTEL_GROUP = "tracing";
    private static final String OTEL_SUBKEY = "matrix.otel";
    private static final String NEXUS_EVENT_SETTING_GROUP = "matrix.event.setting";
    private static final String SPECIFIC_TENANT_ID = "cbec5243-e668-467e-8b67-d236510181b1";
    private static final String OTHER_TENANT_ID = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String HELIX_TENANT_MAPPER_GROUP = "odyssey.tenant.mapper";
    private static final String DEFAULT_TTL_KEY = "some.other.group";


    @BeforeEach
    void setUp() {
        // Clear the cache before each test to ensure isolation
        cache.invalidateAll();
    }

    @Test
    @DisplayName("Should store and retrieve value with specific sub-key TTL before expiry")
    void testGet_WithSpecificSubKeyTtl_BeforeExpiry() {
        // Arrange
        cacheService.put(OTEL_GROUP, OTEL_SUBKEY, true);

        // Act: advance time, but less than the 3m TTL
        ticker.advance(Duration.ofMinutes(2).plus(Duration.ofSeconds(55)));
        Optional<Boolean> result = cacheService.get(OTEL_GROUP, OTEL_SUBKEY, Boolean.class);

        // Assert
        assertThat(result).isPresent().contains(true);
    }

    @Test
    @DisplayName("Should return empty when value with specific sub-key TTL expires")
    void testGet_WithSpecificSubKeyTtl_AfterExpiry() {
        // Arrange
        cacheService.put(OTEL_GROUP, OTEL_SUBKEY, true);

        // Act: advance time beyond the 3m TTL
        ticker.advance(Duration.ofMinutes(3).plus(Duration.ofSeconds(1)));
        Optional<Boolean> result = cacheService.get(OTEL_GROUP, OTEL_SUBKEY, Boolean.class);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should store and retrieve value with group-level TTL before expiry")
    void testGet_NES_WithGroupTtl_BeforeExpiry() {
        // Arrange
        var settings = new TenantEventSetting(List.of("ensEvent1"), List.of());
        cacheService.put(NEXUS_EVENT_SETTING_GROUP, OTHER_TENANT_ID, settings);

        // Act: advance time, but less than the 10m group TTL
        ticker.advance(Duration.ofMinutes(9).plus(Duration.ofSeconds(55)));
        Optional<TenantEventSetting> result = cacheService.get(NEXUS_EVENT_SETTING_GROUP, OTHER_TENANT_ID, TenantEventSetting.class);

        // Assert
        assertThat(result).isPresent().contains(settings);
    }

    @Test
    @DisplayName("Should return empty when value with group-level TTL expires")
    void testGet_NES_WithGroupTtl_AfterExpiry() {
        // Arrange
        var settings = new TenantEventSetting(List.of("ensEvent1"), List.of());
        cacheService.put(NEXUS_EVENT_SETTING_GROUP, OTHER_TENANT_ID, settings);

        // Act: advance time beyond the 10m group TTL
        ticker.advance(Duration.ofMinutes(10).plus(Duration.ofSeconds(2)));
        Optional<TenantEventSetting> result = cacheService.get(NEXUS_EVENT_SETTING_GROUP, OTHER_TENANT_ID, TenantEventSetting.class);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should use specific tenant TTL (5m) instead of group TTL (10m)")
    void testGet_NES_SpecificTenantTtlOverridesGroupTtl() {
        // Arrange
        var settings = new TenantEventSetting(List.of("*"), List.of("dlpEventId1"));
        cacheService.put(NEXUS_EVENT_SETTING_GROUP, SPECIFIC_TENANT_ID, settings);

        // Act: advance time past the 5m specific TTL but before the 10m group TTL
        ticker.advance(Duration.ofMinutes(5).plus(Duration.ofSeconds(1)));
        Optional<TenantEventSetting> result = cacheService.get(NEXUS_EVENT_SETTING_GROUP, SPECIFIC_TENANT_ID, TenantEventSetting.class);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should store and retrieve value with group-level TTL before expiry")
    void testGet_TTM_WithGroupTtl_BeforeExpiry() {
        // Arrange
        var h4TTenantMap = new TenantMapper("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d","odsyId123","us-west-2", "https://odsyId123.qa.ingest.apps.avenger.com/ingest/odsyId123/events",true);
        cacheService.put(HELIX_TENANT_MAPPER_GROUP, OTHER_TENANT_ID, h4TTenantMap);

        // Act: advance time, but less than the 12m group TTL
        ticker.advance(11, TimeUnit.MINUTES);
        Optional<TenantMapper> result = cacheService.get(HELIX_TENANT_MAPPER_GROUP, OTHER_TENANT_ID, TenantMapper.class);

        // Assert
        assertThat(result).isPresent().contains(h4TTenantMap);
    }

    @Test
    @DisplayName("Should return empty when value with group-level TTL expires")
    void testGet_TTM_WithGroupTtl_AfterExpiry() {
        // Arrange
        var h4TTenantMap = new TenantMapper("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d","odsyId123","us-east-1", "https://odsyId123.qa.ingest.apps.avenger.com/ingest/odsyId123/events",true);
        cacheService.put(HELIX_TENANT_MAPPER_GROUP, OTHER_TENANT_ID, h4TTenantMap);

        // Act: advance time beyond the 12m group TTL
        ticker.advance(Duration.ofHours(12).plus(Duration.ofSeconds(1)));
        Optional<TenantMapper> result = cacheService.get(NEXUS_EVENT_SETTING_GROUP, OTHER_TENANT_ID, TenantMapper.class);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should use specific tenant TTL (10m) instead of group TTL (12m)")
    void testGet_TTM_SpecificTenantTtlOverridesGroupTtl() {
        // Arrange
        var h4TTenantMap = new TenantMapper("cbec5243-e668-467e-8b67-d236510181b1","odsyqb939","us-east-1", "https://odsyqb939.qa.ingest.apps.avenger.com/ingest/odsyqb939/events",true);
        cacheService.put(HELIX_TENANT_MAPPER_GROUP, SPECIFIC_TENANT_ID, h4TTenantMap);

        // Act: advance time past the 11m specific TTL but before the 12m group TTL
        ticker.advance(Duration.ofMinutes(11).plus(Duration.ofSeconds(55)));
        Optional<TenantMapper> result = cacheService.get(HELIX_TENANT_MAPPER_GROUP, SPECIFIC_TENANT_ID, TenantMapper.class);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should use default TTL when no specific or group TTL is configured")
    void testGet_WithDefaultTtl_AfterExpiry() {
        // Arrange
        cacheService.put(DEFAULT_TTL_KEY, "some-value");

        // Act: advance time beyond the 30m default TTL
        ticker.advance(Duration.ofMinutes(30).plus(Duration.ofSeconds(1)));
        Optional<String> result = cacheService.get(DEFAULT_TTL_KEY, String.class);

        // Assert
        assertThat(result).isEmpty();
    }
}
