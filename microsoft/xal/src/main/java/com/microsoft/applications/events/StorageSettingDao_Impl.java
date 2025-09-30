package com.microsoft.applications.events;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public final class StorageSettingDao_Impl implements StorageSettingDao {
    private final RoomDatabase __db;
    @SuppressLint("RestrictedApi")
    private final SharedSQLiteStatement __preparedStmtOfDeleteAllSettings;
    @SuppressLint("RestrictedApi")
    private final SharedSQLiteStatement __preparedStmtOfDeleteSetting;
    @SuppressLint("RestrictedApi")
    private final SharedSQLiteStatement __preparedStmtOfSetValue;

    @SuppressLint("RestrictedApi")
    public StorageSettingDao_Impl(RoomDatabase roomDatabase) {
        this.__db = roomDatabase;
        this.__preparedStmtOfSetValue = new SharedSQLiteStatement(roomDatabase) {
            public String createQuery() {
                return "INSERT OR REPLACE INTO StorageSetting (name, value) VALUES (?, ?)";
            }
        };
        this.__preparedStmtOfDeleteAllSettings = new SharedSQLiteStatement(roomDatabase) {
            public String createQuery() {
                return "DELETE FROM StorageSetting";
            }
        };
        this.__preparedStmtOfDeleteSetting = new SharedSQLiteStatement(roomDatabase) {
            public String createQuery() {
                return "DELETE FROM StorageSetting WHERE name = ?";
            }
        };
    }

    @SuppressLint("RestrictedApi")
    @Override
    public long setValue(String str, String str2) {
        this.__db.assertNotSuspendingTransaction();
        SupportSQLiteStatement supportSQLiteStatementAcquire = this.__preparedStmtOfSetValue.acquire();
        if (str == null) {
            supportSQLiteStatementAcquire.bindNull(1);
        } else {
            supportSQLiteStatementAcquire.bindString(1, str);
        }
        if (str2 == null) {
            supportSQLiteStatementAcquire.bindNull(2);
        } else {
            supportSQLiteStatementAcquire.bindString(2, str2);
        }
        this.__db.beginTransaction();
        try {
            long jExecuteInsert = supportSQLiteStatementAcquire.executeInsert();
            this.__db.setTransactionSuccessful();
            return jExecuteInsert;
        } finally {
            this.__db.endTransaction();
            this.__preparedStmtOfSetValue.release(supportSQLiteStatementAcquire);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int deleteAllSettings() {
        this.__db.assertNotSuspendingTransaction();
        SupportSQLiteStatement supportSQLiteStatementAcquire = this.__preparedStmtOfDeleteAllSettings.acquire();
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementAcquire.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
            this.__preparedStmtOfDeleteAllSettings.release(supportSQLiteStatementAcquire);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int deleteSetting(String str) {
        this.__db.assertNotSuspendingTransaction();
        SupportSQLiteStatement supportSQLiteStatementAcquire = this.__preparedStmtOfDeleteSetting.acquire();
        if (str == null) {
            supportSQLiteStatementAcquire.bindNull(1);
        } else {
            supportSQLiteStatementAcquire.bindString(1, str);
        }
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementAcquire.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
            this.__preparedStmtOfDeleteSetting.release(supportSQLiteStatementAcquire);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public StorageSetting[] getValues(String str) {
        @SuppressLint("RestrictedApi") RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT * FROM StorageSetting WHERE name = ?", 1);
        if (str == null) {
            roomSQLiteQueryAcquire.bindNull(1);
        } else {
            roomSQLiteQueryAcquire.bindString(1, str);
        }
        this.__db.assertNotSuspendingTransaction();
        int i = 0;
        Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
        try {
            int columnIndexOrThrow = CursorUtil.getColumnIndexOrThrow(cursorQuery, "name");
            int columnIndexOrThrow2 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "value");
            StorageSetting[] storageSettingArr = new StorageSetting[cursorQuery.getCount()];
            while (cursorQuery.moveToNext()) {
                storageSettingArr[i] = new StorageSetting(cursorQuery.isNull(columnIndexOrThrow) ? null : cursorQuery.getString(columnIndexOrThrow), cursorQuery.isNull(columnIndexOrThrow2) ? null : cursorQuery.getString(columnIndexOrThrow2));
                i++;
            }
            return storageSettingArr;
        } finally {
            cursorQuery.close();
            roomSQLiteQueryAcquire.release();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public long totalSize() {
        @SuppressLint("RestrictedApi") RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT sum(length(name) + length(value)) FROM StorageSetting", 0);
        this.__db.assertNotSuspendingTransaction();
        Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
        try {
            return cursorQuery.moveToFirst() ? cursorQuery.getLong(0) : 0L;
        } finally {
            cursorQuery.close();
            roomSQLiteQueryAcquire.release();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public long totalSettingCount() {
        @SuppressLint("RestrictedApi") RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT count(*) FROM StorageSetting", 0);
        this.__db.assertNotSuspendingTransaction();
        Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
        try {
            return cursorQuery.moveToFirst() ? cursorQuery.getLong(0) : 0L;
        } finally {
            cursorQuery.close();
            roomSQLiteQueryAcquire.release();
        }
    }

    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}