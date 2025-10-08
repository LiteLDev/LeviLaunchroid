package com.microsoft.applications.events;

import androidx.room.RoomDatabase;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public abstract class OfflineRoomDatabase extends RoomDatabase {
    public abstract StorageRecordDao getStorageRecordDao();

    public abstract StorageSettingDao getStorageSettingDao();
}
