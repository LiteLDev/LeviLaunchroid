package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public abstract class ILogConfiguration {
    public static native ILogConfiguration getDefaultConfiguration();

    public abstract Boolean getBoolean(LogConfigurationKey logConfigurationKey);

    public abstract ILogConfiguration getLogConfiguration(LogConfigurationKey logConfigurationKey);

    public abstract Long getLong(LogConfigurationKey logConfigurationKey);

    public abstract Object getObject(LogConfigurationKey logConfigurationKey);

    public abstract Object getObject(String str);

    public abstract String getString(LogConfigurationKey logConfigurationKey);

    public abstract ILogConfiguration roundTrip();

    public abstract void set(String str, Object obj);

    public abstract boolean set(LogConfigurationKey logConfigurationKey, ILogConfiguration iLogConfiguration);

    public abstract boolean set(LogConfigurationKey logConfigurationKey, Boolean bool);

    public abstract boolean set(LogConfigurationKey logConfigurationKey, Long l);

    public abstract boolean set(LogConfigurationKey logConfigurationKey, String str);
}
