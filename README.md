Time-Expiring Cache with Hierarchical TTL
This document describes a flexible, time-expiring in-memory cache solution built using Java 17, Spring Boot, and the high-performance Caffeine library.

The core feature of this implementation is a hierarchical, multi-level Time-To-Live (TTL) configuration, allowing for global, group-level, and highly specific per-entry expiration times.

Features
High-Performance Caching: Leverages Caffeine for near-optimal, in-memory caching performance.

Hierarchical TTL Configuration: Provides three levels of TTL configuration with a clear fallback mechanism.

Clean Abstraction: A simple TimeExpiringCacheService hides the complexity of key management and TTL resolution.

Type-Safe Retrieval: Generic methods for safe casting of cached objects.

Dynamic Configuration: TTLs are managed externally in application.properties and can be updated without code changes.

Configuration
The cache's expiration behavior is controlled through application.properties. The TTL for a given cache entry is resolved using the following priority order:

Specific Sub-Key TTL (Highest Priority): A TTL defined for a unique entry within a group.

Group-Level TTL (Medium Priority): A default TTL for all entries belonging to a specific group.

Global Default TTL (Lowest Priority): A fallback TTL for any entry that does not have a specific or group-level configuration.

Example application.properties
# 1. Global Default TTL (lowest priority)
# Fallback for any key that doesn't match a more specific rule.
app.cache.ttl.default=15m

# 2. Group/Main Key TTLs (medium priority)
# Default TTL for all entries belonging to a "group".
# Note: Use bracket notation for keys with dots to avoid issues with Spring parsing.
app.cache.ttl.keys[matrix.event.setting]=10m
app.cache.ttl.keys[odyssey.tenant.mapper]=12m
app.cache.ttl.keys[tracing]=5m


# 3. Specific Sub-Key TTLs (highest priority)
# A specific TTL for a tenant within the "matrix.event.setting" group.
app.cache.ttl.keys[matrix.event.setting.cbec5243-e668-467e-8b67-d236510181b1]=5m
# A specific TTL for a tenant within the "odyssey.tenant.mapper" group.
app.cache.ttl.keys[odyssey.tenant.mapper.cbec5243-e668-467e-8b67-d236510181b1]=10m
# A specific TTL for the "matrix.otel" flag within the "tracing" group.
app.cache.ttl.keys[tracing.matrix.otel]=3m

How It Works
The system is composed of three main parts: the service, the expiry policy, and the configuration.

1. TimeExpiringCacheService
   This service is the public API for all cache interactions. It abstracts away the underlying implementation details. To support hierarchical TTLs, it uses a composite key strategy. When you store an item, you provide a groupKey and a subKey. The service combines these into a single internal key (e.g., "matrix.event.setting:cbec5243-...").

2. PerKeyExpiryPolicy
   This is a custom implementation of Caffeine's Expiry interface and is the core of the TTL logic. When an item is added to the cache, this policy is invoked to determine its lifespan. It inspects the composite key and queries the CacheTtlProperties in the defined hierarchical order to find the appropriate duration.

3. CacheConfig & CacheTtlProperties
   CacheTtlProperties is a @ConfigurationProperties class that loads all app.cache.ttl.* values from application.properties into a structured Java object.

CacheConfig is a @Configuration class that builds the Caffeine Cache bean, wiring it up with our custom PerKeyExpiryPolicy.

Usage
Inject the TimeExpiringCacheService into any Spring component and use its simple put and get methods.

Storing Data
@Service
public class MyService {

    private final TimeExpiringCacheService cacheService;

    public MyService(TimeExpiringCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void setupCacheData() {
        // --- Use Case 1: Specific Sub-Key TTL ---
        // This will use the 3m TTL from 'tracing.matrix.otel'
        cacheService.put("tracing", "matrix.otel", true);

        // --- Use Case 2: Group-Level TTL ---
        // This tenant doesn't have a specific TTL, so it falls back to the
        // 10m group TTL from 'matrix.event.setting'.
        String tenantId = "some-other-tenant-uuid";
        //TenantEventSettings(List<String> ensEventSetting, List<String> dlpEventSetting)
        var tenantData = new TenantEventSettings(List.of("*"), List.of("dlpEventId1"));;
        cacheService.put("matrix.event.setting", tenantId, tenantData);

        // --- Use Case 3: Group-Level TTL ---
        // This tenant doesn't have a specific TTL, so it falls back to the
        // 12m group TTL from 'odyssey.tenant.mapper'.
        //TenantMapper(String cyborgTenantId, String odsyId, String region, String helixIngestionGW, Boolean isActive)
        String tenantId = "some-other-tenant-uuid";
        var tenantData = new TenantMapper("some-other-tenant-uuid","odsyId123","us-east-1", "https://odsyId123.qa.ingest.apps.avenger.com/ingest/odsyId123/events",true);
        cacheService.put("odyssey.tenant.mapper", tenantId, tenantData);
        
        // --- Use Case 4: Global Default TTL ---
        // This group has no configuration, so it falls back to the 15m default.
        cacheService.put("unconfigured.group", "some.key", "some-value");
    }
}

Retrieving Data
The get methods are type-safe. You provide the key parts and the Class of the object you expect to retrieve.

// Retrieve the boolean flag
Optional<Boolean> otelFlag = cacheService.get("tracing", "matrix.otel", Boolean.class);
otelFlag.ifPresent(flag -> System.out.println("Otel Flag is: " + flag));

// Retrieve the TenantEventSettings data POJO
Optional<TenantEventSettings> tenantData = cacheService.get("matrix.event.setting", tenantId, TenantEventSettings.class);
tenantData.ifPresent(data -> System.out.println("Tenant Data: " + data));

// Retrieve the TenantMapper data POJO
Optional<TenantMapper> tenantData = cacheService.get("odyssey.tenant.mapper", tenantId, TenantMapper.class);
tenantData.ifPresent(data -> System.out.println("Tenant Data: " + data));

// Returns Optional.empty() if the item is not found or has expired.

Testing
To test time-based expiration without Thread.sleep(), the test suite uses a custom Ticker implementation. This allows tests to manually advance time and verify that cache entries expire precisely when their configured TTL is up. Refer to TimeExpiringCacheServiceTest.java for a detailed example.