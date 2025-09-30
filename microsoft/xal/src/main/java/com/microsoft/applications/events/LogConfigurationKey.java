package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public enum LogConfigurationKey {
    CFG_BOOL_ENABLE_ANALYTICS("enableLifecycleSession", Boolean.class),
    CFG_BOOL_ENABLE_MULTITENANT("multiTenantEnabled", Boolean.class),
    CFG_BOOL_ENABLE_CRC32("enableCRC32", Boolean.class),
    CFG_BOOL_ENABLE_HMAC("enableHMAC", Boolean.class),
    CFG_BOOL_ENABLE_DB_DROP_IF_FULL("enableDbDropIfFull", Boolean.class),
    CFG_BOOL_ENABLE_DB_COMPRESS("enableDBCompression", Boolean.class),
    CFG_BOOL_ENABLE_WAL_JOURNAL("enableWALJournal", Boolean.class),
    CFG_BOOL_ENABLE_NET_DETECT("enableNetworkDetector", Boolean.class),
    CFG_BOOL_TPM_CLOCK_SKEW_ENABLED("clockSkewEnabled", Boolean.class),
    CFG_BOOL_UTC_ENABLED("e", Boolean.class),
    CFG_BOOL_UTC_ACTIVE("active", Boolean.class),
    CFG_BOOL_UTC_LARGE_PAYLOADS("largePayloadsEnabled", Boolean.class),
    CFG_STR_COLLECTOR_URL("ee", String.class),
    CFG_STR_CACHE_FILE_PATH("cacheFilePath", String.class),
    CFG_INT_CACHE_FILE_SIZE("eee", Long.class),
    CFG_INT_RAM_QUEUE_SIZE("cacheMemorySizeLimitInBytes", Long.class),
    CFG_INT_RAM_QUEUE_BUFFERS("maxDBFlushQueues", Long.class),
    CFG_INT_TRACE_LEVEL_MASK("traceLevelMask", Long.class),
    CFG_INT_TRACE_LEVEL_MIN("minimumTraceLevel", Long.class),
    CFG_BOOL_ENABLE_TRACE("enableTrace", Boolean.class),
    CFG_STR_TRACE_FOLDER_PATH("traceFolderPath", String.class),
    CFG_INT_SDK_MODE("sdkmode", Long.class),
    CFG_MAP_UTC("utc", ILogConfiguration.class),
    CFG_STR_UTC("utc", ILogConfiguration.class),
    CFG_STR_PROVIDER_GROUP_ID("providerGroupId", String.class),
    CFG_INT_MAX_TEARDOWN_TIME("maxTeardownUploadTimeInSec", Long.class),
    CFG_INT_MAX_PENDING_REQ("maxPendingHTTPRequests", Long.class),
    CFG_INT_MAX_PKG_DROP_ON_FULL("maxPkgDropOnFull", Long.class),
    CFG_INT_STORAGE_FULL_PCT("cacheFileFullNotificationPercentage", Long.class),
    CFG_INT_STORAGE_FULL_CHECK_TIME("cacheFullNotificationIntervalTime", Long.class),
    CFG_INT_RAMCACHE_FULL_PCT("cacheMemoryFullNotificationPercentage", Long.class),
    CFG_STR_PRAGMA_JOURNAL_MODE("PRAGMA_journal_mode", String.class),
    CFG_STR_PRAGMA_SYNCHRONOUS("PRAGMA_synchronous", String.class),
    CFG_STR_PRIMARY_TOKEN("primaryToken", String.class),
    CFG_STR_START_PROFILE_NAME("startProfileName", String.class),
    CFG_STR_TRANSMIT_PROFILES("transmitProfiles", String.class),
    CFG_STR_FACTORY_NAME("name", String.class),
    CFG_MAP_FACTORY_CONFIG("config", ILogConfiguration.class),
    CFG_STR_FACTORY_HOST("host", String.class),
    CFG_STR_CONTEXT_SCOPE("scope", String.class),
    CFG_MAP_SAMPLE("sample", ILogConfiguration.class),
    CFG_INT_SAMPLE_RATE("rate", Long.class),
    CFG_MAP_METASTATS_CONFIG("stats", ILogConfiguration.class),
    CFG_INT_METASTATS_INTERVAL("interval", Long.class),
    CFG_BOOL_METASTATS_SPLIT("split", Boolean.class),
    CFG_STR_METASTATS_TOKEN_INT("tokenInt", String.class),
    CFG_STR_METASTATS_TOKEN_PROD("tokenProd", String.class),
    CFG_MAP_COMPAT("compat", ILogConfiguration.class),
    CFG_BOOL_COMPAT_DOTS("dotType", Boolean.class),
    CFG_BOOL_HOST_MODE("hostMode", Boolean.class),
    CFG_MAP_HTTP("http", ILogConfiguration.class),
    CFG_BOOL_HTTP_MS_ROOT_CHECK("msRootCheck", Boolean.class),
    CFG_BOOL_HTTP_COMPRESSION("compress", Boolean.class),
    CFG_STR_HTTP_CONTENT_ENCODING("contentEncoding", String.class),
    CFG_MAP_TPM("tpm", ILogConfiguration.class),
    CFG_INT_TPM_MAX_RETRY("maxRetryCount", Long.class),
    CFG_STR_TPM_BACKOFF("eeee", String.class),
    CFG_INT_TPM_MAX_BLOB_BYTES("maxBlobSize", Long.class),
    CFG_BOOL_SESSION_RESET_ENABLED("sessionResetEnabled", Boolean.class);

    private String key;
    private Class valueType;

    LogConfigurationKey(String str, Class cls) {
        this.key = str;
        this.valueType = cls;
    }

    public String getKey() {
        return this.key;
    }

    public Class getValueType() {
        return this.valueType;
    }
}
