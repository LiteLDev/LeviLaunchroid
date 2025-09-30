package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class PrivacyGuardInitConfig {
    public CommonDataContext DataContext;
    public ILogger LoggerInstance;
    public String NotificationEventName;
    public String SemanticContextNotificationEventName;
    public String SummaryEventName;
    public boolean UseEventFieldPrefix = false;
    public boolean ScanForUrls = true;

    public PrivacyGuardInitConfig(ILogger iLogger, CommonDataContext commonDataContext) {
        if (iLogger == null) {
            throw new IllegalArgumentException("logger cannot be null");
        }
        if (commonDataContext == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        this.LoggerInstance = iLogger;
        this.DataContext = commonDataContext;
    }
}
