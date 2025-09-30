package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public interface StorageSettingDao {
    int deleteAllSettings();

    int deleteSetting(String str);

    StorageSetting[] getValues(String str);

    long setValue(String str, String str2);

    long totalSettingCount();

    long totalSize();
}
