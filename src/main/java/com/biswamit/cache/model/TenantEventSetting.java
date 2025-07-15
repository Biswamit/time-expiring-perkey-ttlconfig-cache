package com.biswamit.cache.model;
import java.util.List;

/**
 * Example POJO to store tenant-specific event settings.
 * Using Java 17 record for a concise, immutable data carrier.
 */
public record TenantEventSetting(List<String> ensEventSetting, List<String> dlpEventSetting) {}
