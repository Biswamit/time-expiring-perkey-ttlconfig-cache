package com.biswamit.cache;

import com.biswamit.cache.model.TenantMapper;
import com.biswamit.cache.model.TenantEventSetting;
import com.biswamit.cache.service.TimeExpiringCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An example component demonstrating how to use the TimeExpiringCacheService.
 */
@Component
public class CacheUsageRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CacheUsageRunner.class);
    private final TimeExpiringCacheService cacheService;

    public CacheUsageRunner(TimeExpiringCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("--- Cache Usage Example ---");

        // --- Use Case 1: Storing a simple feature flag (e.g., tracing -> matrix.otel) ---
        String otelGroup = "tracing";
        String otelSubKey = "matrix.otel";
        cacheService.put(otelGroup, otelSubKey, true);
        logger.info("Stored '{}' -> '{}' = true. This should have a 3m TTL from properties.", otelGroup, otelSubKey);


        // --- Use Case 2: Storing complex POJO for a specific tenant ---
        String eventGroup = "matrix.event.setting";
        String specificTenantId = "cbec5243-e668-467e-8b67-d236510181b1";
        var specificTenantSettings = new TenantEventSetting(List.of("*"), List.of("dlpEventId1", "dlpEventId2"));
        cacheService.put(eventGroup, specificTenantId, specificTenantSettings);
        logger.info("Stored settings for specific tenant '{}'. This should have a 5-minute TTL.", specificTenantId);

        // --- Use Case 3: Storing POJO for a tenant without a specific TTL ---
        String otherTenantId = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d";
        var otherTenantSettings = new TenantEventSetting(List.of("ensEvent1"), List.of());
        cacheService.put(eventGroup, otherTenantId, otherTenantSettings);
        logger.info("Stored settings for other tenant '{}'. This will fall back to the group TTL of 10-minute.", otherTenantId);

        // --- Use Case 4: Storing POJO for a tenant without a specific TTL ---
        String h4tTenantMapGroup = "odyssey.tenant.mapper";
        String cyborgTenantId = UUID.fromString("cbec5243-e668-467e-8b67-d236510181b1").toString();
        String odsyId = "odsyqb939";
        String helixIngestionGW = "https://odsyqb939.qa.ingest.apps.avenger.com/ingest/odsyqb939/events";
        var h4TenantMap = new TenantMapper(cyborgTenantId,odsyId,"us-west-2", helixIngestionGW,true);
        cacheService.put(h4tTenantMapGroup, cyborgTenantId, h4TenantMap);
        logger.info("Stored settings for cyborgTenantId '{}'. This should have a 10-minute TTL..", cyborgTenantId);

        // --- Use Case 4: Storing POJO for a tenant without a specific TTL ---
        String anotherTenantId = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d").toString();
        String anotherHexId = "odsyId123";
        String anotherHelixIngestionGW = "https://odsyId123.qa.ingest.apps.avenger.com/ingest/odsyId123/events";
        var anotherTenantMapper = new TenantMapper(cyborgTenantId,anotherHexId,"us-east-1", anotherHelixIngestionGW,true);
        cacheService.put(h4tTenantMapGroup, anotherTenantId, anotherTenantMapper);
        logger.info("Stored settings for other anotherTenantId '{}'. This will fall back to the group TTL of 12-minute.", anotherTenantId);

        // --- Retrieving data ---
        Optional<Boolean> otelFlag = cacheService.get(otelGroup, otelSubKey, Boolean.class);
        otelFlag.ifPresent(flag -> logger.info("Retrieved flag '{}': {}", otelSubKey, flag));

        Optional<TenantEventSetting> settings = cacheService.get(eventGroup, specificTenantId, TenantEventSetting.class);
        settings.ifPresent(s -> logger.info("Retrieved settings for tenant '{}': {}", specificTenantId, s));

        logger.info("--- Example Finished. Check logs for TTL assignments. ---");
    }
}

