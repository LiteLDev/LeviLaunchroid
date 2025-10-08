package com.microsoft.applications.events;

import android.annotation.SuppressLint;

import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public final class OfflineRoomDatabase_Impl extends OfflineRoomDatabase {
    private volatile StorageRecordDao _storageRecordDao;
    private volatile StorageSettingDao _storageSettingDao;

    @SuppressLint("RestrictedApi")
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration databaseConfiguration) {
        return databaseConfiguration.sqliteOpenHelperFactory.create(SupportSQLiteOpenHelper.Configuration.builder(databaseConfiguration.context).name(databaseConfiguration.name).callback(new RoomOpenHelper(databaseConfiguration, new RoomOpenHelper.Delegate(3) {
            public void onPostMigrate(SupportSQLiteDatabase supportSQLiteDatabase) {
            }

            public void createAllTables(SupportSQLiteDatabase supportSQLiteDatabase) {
                supportSQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `StorageRecord` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tenantToken` TEXT, `latency` INTEGER NOT NULL, `persistence` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL, `reservedUntil` INTEGER NOT NULL, `blob` BLOB)");
                supportSQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_StorageRecord_id` ON `StorageRecord` (`id`)");
                supportSQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_StorageRecord_latency` ON `StorageRecord` (`latency`)");
                supportSQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS `StorageSetting` (`name` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`name`))");
                supportSQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
                supportSQLiteDatabase.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c562644244e4b7e47787917e9f63a59e')");
            }

            public void dropAllTables(SupportSQLiteDatabase supportSQLiteDatabase) {
                supportSQLiteDatabase.execSQL("DROP TABLE IF EXISTS `StorageRecord`");
                supportSQLiteDatabase.execSQL("DROP TABLE IF EXISTS `StorageSetting`");
                if (OfflineRoomDatabase_Impl.this.mCallbacks != null) {
                    int size = OfflineRoomDatabase_Impl.this.mCallbacks.size();
                    for (int i = 0; i < size; i++) {
                        ((RoomDatabase.Callback) OfflineRoomDatabase_Impl.this.mCallbacks.get(i)).onDestructiveMigration(supportSQLiteDatabase);
                    }
                }
            }

            public void onCreate(SupportSQLiteDatabase supportSQLiteDatabase) {
                if (OfflineRoomDatabase_Impl.this.mCallbacks != null) {
                    int size = OfflineRoomDatabase_Impl.this.mCallbacks.size();
                    for (int i = 0; i < size; i++) {
                        ((RoomDatabase.Callback) OfflineRoomDatabase_Impl.this.mCallbacks.get(i)).onCreate(supportSQLiteDatabase);
                    }
                }
            }

            public void onOpen(SupportSQLiteDatabase supportSQLiteDatabase) {
                OfflineRoomDatabase_Impl.this.mDatabase = supportSQLiteDatabase;
                OfflineRoomDatabase_Impl.this.internalInitInvalidationTracker(supportSQLiteDatabase);
                if (OfflineRoomDatabase_Impl.this.mCallbacks != null) {
                    int size = OfflineRoomDatabase_Impl.this.mCallbacks.size();
                    for (int i = 0; i < size; i++) {
                        ((RoomDatabase.Callback) OfflineRoomDatabase_Impl.this.mCallbacks.get(i)).onOpen(supportSQLiteDatabase);
                    }
                }
            }

            public void onPreMigrate(SupportSQLiteDatabase supportSQLiteDatabase) {
                DBUtil.dropFtsSyncTriggers(supportSQLiteDatabase);
            }

            @SuppressLint("RestrictedApi")
            public RoomOpenHelper.ValidationResult onValidateSchema(SupportSQLiteDatabase supportSQLiteDatabase) {
                HashMap map = new HashMap(8);
                map.put("id", new TableInfo.Column("id", "INTEGER", true, 1, (String) null, 1));
                map.put("tenantToken", new TableInfo.Column("tenantToken", "TEXT", false, 0, (String) null, 1));
                map.put("latency", new TableInfo.Column("latency", "INTEGER", true, 0, (String) null, 1));
                map.put("persistence", new TableInfo.Column("persistence", "INTEGER", true, 0, (String) null, 1));
                map.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, (String) null, 1));
                map.put("retryCount", new TableInfo.Column("retryCount", "INTEGER", true, 0, (String) null, 1));
                map.put("reservedUntil", new TableInfo.Column("reservedUntil", "INTEGER", true, 0, (String) null, 1));
                map.put("blob", new TableInfo.Column("blob", "BLOB", false, 0, (String) null, 1));
                HashSet hashSet = new HashSet(0);
                HashSet hashSet2 = new HashSet(2);
                hashSet2.add(new TableInfo.Index("index_StorageRecord_id", true, Arrays.asList("id"), Arrays.asList("ASC")));
                hashSet2.add(new TableInfo.Index("index_StorageRecord_latency", false, Arrays.asList("latency"), Arrays.asList("ASC")));
                TableInfo tableInfo = new TableInfo("StorageRecord", map, hashSet, hashSet2);
                TableInfo tableInfo2 = TableInfo.read(supportSQLiteDatabase, "StorageRecord");
                if (!tableInfo.equals(tableInfo2)) {
                    return new RoomOpenHelper.ValidationResult(false, "StorageRecord(com.microsoft.applications.events.StorageRecord).\n Expected:\n" + tableInfo + "\n Found:\n" + tableInfo2);
                }
                HashMap map2 = new HashMap(2);
                map2.put("name", new TableInfo.Column("name", "TEXT", true, 1, (String) null, 1));
                map2.put("value", new TableInfo.Column("value", "TEXT", true, 0, (String) null, 1));
                HashSet hashSet3 = new HashSet(0);
                HashSet hashSet4 = new HashSet(0);
                String str = "StorageSetting";
                TableInfo tableInfo3 = new TableInfo(str, map2, hashSet3, hashSet4);
                TableInfo tableInfo4 = TableInfo.read(supportSQLiteDatabase, str);
                if (!tableInfo3.equals(tableInfo4)) {
                    return new RoomOpenHelper.ValidationResult(false, "StorageSetting(com.microsoft.applications.events.StorageSetting).\n Expected:\n" + tableInfo3 + "\n Found:\n" + tableInfo4);
                }
                return new RoomOpenHelper.ValidationResult(true, (String) null);
            }
        }, "c562644244e4b7e47787917e9f63a59e", "2b88cd37477c619d428f0b5e60adeaef")).build());
    }

    @SuppressLint("RestrictedApi")
    protected InvalidationTracker createInvalidationTracker() {
        return new InvalidationTracker(this, new HashMap(0), new HashMap(0), new String[]{"StorageRecord", "StorageSetting"});
    }

    @SuppressLint("RestrictedApi")
    public void clearAllTables() {
        super.assertNotMainThread();
        SupportSQLiteDatabase writableDatabase = super.getOpenHelper().getWritableDatabase();
        try {
            super.beginTransaction();
            writableDatabase.execSQL("DELETE FROM `StorageRecord`");
            writableDatabase.execSQL("DELETE FROM `StorageSetting`");
            super.setTransactionSuccessful();
        } finally {
            super.endTransaction();
            writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close();
            if (!writableDatabase.inTransaction()) {
                writableDatabase.execSQL("VACUUM");
            }
        }
    }

    @SuppressLint("RestrictedApi")
    protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
        HashMap map = new HashMap();
        map.put(StorageRecordDao.class, StorageRecordDao_Impl.getRequiredConverters());
        map.put(StorageSettingDao.class, StorageSettingDao_Impl.getRequiredConverters());
        return map;
    }

    @SuppressLint("RestrictedApi")
    public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
        return new HashSet();
    }

    @SuppressLint("RestrictedApi")
    public List<Migration> getAutoMigrations(Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> map) {
        return Arrays.asList(new Migration[0]);
    }

    @Override // com.microsoft.applications.events.OfflineRoomDatabase
    public StorageRecordDao getStorageRecordDao() {
        StorageRecordDao storageRecordDao;
        if (this._storageRecordDao != null) {
            return this._storageRecordDao;
        }
        synchronized (this) {
            if (this._storageRecordDao == null) {
                this._storageRecordDao = new StorageRecordDao_Impl(this);
            }
            storageRecordDao = this._storageRecordDao;
        }
        return storageRecordDao;
    }

    @Override // com.microsoft.applications.events.OfflineRoomDatabase
    public StorageSettingDao getStorageSettingDao() {
        StorageSettingDao storageSettingDao;
        if (this._storageSettingDao != null) {
            return this._storageSettingDao;
        }
        synchronized (this) {
            if (this._storageSettingDao == null) {
                this._storageSettingDao = new StorageSettingDao_Impl(this);
            }
            storageSettingDao = this._storageSettingDao;
        }
        return storageSettingDao;
    }
}
