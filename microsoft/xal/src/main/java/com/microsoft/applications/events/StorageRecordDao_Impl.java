package com.microsoft.applications.events;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public final class StorageRecordDao_Impl extends StorageRecordDao {
    private final RoomDatabase __db;
    @SuppressLint("RestrictedApi")
    private final EntityDeletionOrUpdateAdapter<StorageRecord> __deletionAdapterOfStorageRecord;
    @SuppressLint("RestrictedApi")
    private final EntityInsertionAdapter<StorageRecord> __insertionAdapterOfStorageRecord;
    @SuppressLint("RestrictedApi")
    private final SharedSQLiteStatement __preparedStmtOfDeleteAllRecords;
    @SuppressLint("RestrictedApi")
    private final SharedSQLiteStatement __preparedStmtOfDeleteRecordsByToken;
    @SuppressLint("RestrictedApi")
    private final SharedSQLiteStatement __preparedStmtOfReleaseExpired;
    @SuppressLint("RestrictedApi")
    private final SharedSQLiteStatement __preparedStmtOfTrim;

    @SuppressLint("RestrictedApi")
    public StorageRecordDao_Impl(RoomDatabase roomDatabase) {
        this.__db = roomDatabase;
        this.__insertionAdapterOfStorageRecord = new EntityInsertionAdapter<StorageRecord>(roomDatabase) {
            public String createQuery() {
                return "INSERT OR REPLACE INTO `StorageRecord` (`id`,`tenantToken`,`latency`,`persistence`,`timestamp`,`retryCount`,`reservedUntil`,`blob`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
            }

            public void bind(SupportSQLiteStatement supportSQLiteStatement, StorageRecord storageRecord) {
                supportSQLiteStatement.bindLong(1, storageRecord.id);
                if (storageRecord.tenantToken == null) {
                    supportSQLiteStatement.bindNull(2);
                } else {
                    supportSQLiteStatement.bindString(2, storageRecord.tenantToken);
                }
                supportSQLiteStatement.bindLong(3, storageRecord.latency);
                supportSQLiteStatement.bindLong(4, storageRecord.persistence);
                supportSQLiteStatement.bindLong(5, storageRecord.timestamp);
                supportSQLiteStatement.bindLong(6, storageRecord.retryCount);
                supportSQLiteStatement.bindLong(7, storageRecord.reservedUntil);
                if (storageRecord.blob == null) {
                    supportSQLiteStatement.bindNull(8);
                } else {
                    supportSQLiteStatement.bindBlob(8, storageRecord.blob);
                }
            }
        };
        this.__deletionAdapterOfStorageRecord = new EntityDeletionOrUpdateAdapter<StorageRecord>(roomDatabase) {
            public String createQuery() {
                return "DELETE FROM `StorageRecord` WHERE `id` = ?";
            }

            public void bind(SupportSQLiteStatement supportSQLiteStatement, StorageRecord storageRecord) {
                supportSQLiteStatement.bindLong(1, storageRecord.id);
            }
        };
        this.__preparedStmtOfDeleteAllRecords = new SharedSQLiteStatement(roomDatabase) {
            public String createQuery() {
                return "DELETE FROM StorageRecord";
            }
        };
        this.__preparedStmtOfTrim = new SharedSQLiteStatement(roomDatabase) {
            public String createQuery() {
                return "DELETE FROM StorageRecord WHERE id IN (SELECT id FROM StorageRecord ORDER BY persistence ASC, timestamp ASC LIMIT ?)";
            }
        };
        this.__preparedStmtOfReleaseExpired = new SharedSQLiteStatement(roomDatabase) {
            public String createQuery() {
                return "UPDATE StorageRecord SET reservedUntil = 0 WHERE reservedUntil > 0 AND reservedUntil < ?";
            }
        };
        this.__preparedStmtOfDeleteRecordsByToken = new SharedSQLiteStatement(roomDatabase) {
            public String createQuery() {
                return "DELETE FROM StorageRecord WHERE tenantToken = ?";
            }
        };
    }

    @SuppressLint("RestrictedApi")
    @Override
    public long[] insertRecords(StorageRecord... storageRecordArr) {
        this.__db.assertNotSuspendingTransaction();
        this.__db.beginTransaction();
        try {
            long[] jArrInsertAndReturnIdsArray = this.__insertionAdapterOfStorageRecord.insertAndReturnIdsArray(storageRecordArr);
            this.__db.setTransactionSuccessful();
            return jArrInsertAndReturnIdsArray;
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int deleteRecordInner(StorageRecord[] storageRecordArr) {
        this.__db.assertNotSuspendingTransaction();
        this.__db.beginTransaction();
        try {
            int iHandleMultiple = this.__deletionAdapterOfStorageRecord.handleMultiple(storageRecordArr);
            this.__db.setTransactionSuccessful();
            return iHandleMultiple;
        } finally {
            this.__db.endTransaction();
        }
    }

    @Override
    public StorageRecord[] getRecords(boolean z, int i, long j) {
        this.__db.beginTransaction();
        try {
            StorageRecord[] records = super.getRecords(z, i, j);
            this.__db.setTransactionSuccessful();
            return records;
        } finally {
            this.__db.endTransaction();
        }
    }

    @Override
    public int deleteById(long[] jArr) {
        this.__db.beginTransaction();
        try {
            int iDeleteById = super.deleteById(jArr);
            this.__db.setTransactionSuccessful();
            return iDeleteById;
        } finally {
            this.__db.endTransaction();
        }
    }

    @Override
    public StorageRecord[] getAndReserve(int i, long j, long j2, long j3) {
        this.__db.beginTransaction();
        try {
            StorageRecord[] andReserve = super.getAndReserve(i, j, j2, j3);
            this.__db.setTransactionSuccessful();
            return andReserve;
        } finally {
            this.__db.endTransaction();
        }
    }

    @Override
    public void setReserved(long[] jArr, long j) {
        this.__db.beginTransaction();
        try {
            super.setReserved(jArr, j);
            this.__db.setTransactionSuccessful();
        } finally {
            this.__db.endTransaction();
        }
    }

    @Override
    public long releaseRecords(long[] jArr, boolean z, long j, TreeMap<String, Long> treeMap) {
        this.__db.beginTransaction();
        try {
            long jReleaseRecords = super.releaseRecords(jArr, z, j, treeMap);
            this.__db.setTransactionSuccessful();
            return jReleaseRecords;
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int deleteAllRecords() {
        this.__db.assertNotSuspendingTransaction();
        SupportSQLiteStatement supportSQLiteStatementAcquire = this.__preparedStmtOfDeleteAllRecords.acquire();
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementAcquire.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
            this.__preparedStmtOfDeleteAllRecords.release(supportSQLiteStatementAcquire);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int trim(long j) {
        this.__db.assertNotSuspendingTransaction();
        SupportSQLiteStatement supportSQLiteStatementAcquire = this.__preparedStmtOfTrim.acquire();
        supportSQLiteStatementAcquire.bindLong(1, j);
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementAcquire.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
            this.__preparedStmtOfTrim.release(supportSQLiteStatementAcquire);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int releaseExpired(long j) {
        this.__db.assertNotSuspendingTransaction();
        SupportSQLiteStatement supportSQLiteStatementAcquire = this.__preparedStmtOfReleaseExpired.acquire();
        supportSQLiteStatementAcquire.bindLong(1, j);
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementAcquire.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
            this.__preparedStmtOfReleaseExpired.release(supportSQLiteStatementAcquire);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int deleteRecordsByToken(String str) {
        this.__db.assertNotSuspendingTransaction();
        SupportSQLiteStatement supportSQLiteStatementAcquire = this.__preparedStmtOfDeleteRecordsByToken.acquire();
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
            this.__preparedStmtOfDeleteRecordsByToken.release(supportSQLiteStatementAcquire);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public long recordCount(int i) {
        RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT count(*) from StorageRecord WHERE latency = ?", 1);
        roomSQLiteQueryAcquire.bindLong(1, i);
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
    public long totalRecordCount() {
        RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT count(*) from StorageRecord", 0);
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
    public long totalSize() {
        @SuppressLint("RestrictedApi") RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT sum(length(id)) + sum(length(tenantToken)) + sum(length(blob)) + 40*count(*) from StorageRecord;", 0);
        this.__db.assertNotSuspendingTransaction();
        @SuppressLint("RestrictedApi") Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
        try {
            return cursorQuery.moveToFirst() ? cursorQuery.getLong(0) : 0L;
        } finally {
            cursorQuery.close();
            roomSQLiteQueryAcquire.release();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public StorageRecord[] getRecords(int i, long j) {
        RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT * FROM StorageRecord WHERE latency >= ? ORDER BY latency DESC, persistence DESC, timestamp ASC LIMIT ?", 2);
        roomSQLiteQueryAcquire.bindLong(1, i);
        roomSQLiteQueryAcquire.bindLong(2, j);
        this.__db.assertNotSuspendingTransaction();
        this.__db.beginTransaction();
        try {
            int i2 = 0;
            @SuppressLint("RestrictedApi") Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
            try {
                @SuppressLint("RestrictedApi") int columnIndexOrThrow = CursorUtil.getColumnIndexOrThrow(cursorQuery, "id");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow2 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "tenantToken");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow3 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "latency");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow4 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "persistence");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow5 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "timestamp");
                int columnIndexOrThrow6 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "retryCount");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow7 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "reservedUntil");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow8 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "blob");
                StorageRecord[] storageRecordArr = new StorageRecord[cursorQuery.getCount()];
                while (cursorQuery.moveToNext()) {
                    storageRecordArr[i2] = new StorageRecord(cursorQuery.getLong(columnIndexOrThrow), cursorQuery.isNull(columnIndexOrThrow2) ? null : cursorQuery.getString(columnIndexOrThrow2), cursorQuery.getInt(columnIndexOrThrow3), cursorQuery.getInt(columnIndexOrThrow4), cursorQuery.getLong(columnIndexOrThrow5), cursorQuery.getInt(columnIndexOrThrow6), cursorQuery.getLong(columnIndexOrThrow7), cursorQuery.isNull(columnIndexOrThrow8) ? null : cursorQuery.getBlob(columnIndexOrThrow8));
                    i2++;
                }
                this.__db.setTransactionSuccessful();
                return storageRecordArr;
            } finally {
                cursorQuery.close();
                roomSQLiteQueryAcquire.release();
            }
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public StorageRecord[] getUnreservedRecords(int i, long j) {
        @SuppressLint("RestrictedApi") RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT * FROM StorageRecord WHERE latency >= ? AND reservedUntil = 0 ORDER BY latency DESC, persistence DESC, timestamp ASC LIMIT ?", 2);
        roomSQLiteQueryAcquire.bindLong(1, i);
        roomSQLiteQueryAcquire.bindLong(2, j);
        this.__db.assertNotSuspendingTransaction();
        this.__db.beginTransaction();
        try {
            int i2 = 0;
            @SuppressLint("RestrictedApi") Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
            try {
                @SuppressLint("RestrictedApi") int columnIndexOrThrow = CursorUtil.getColumnIndexOrThrow(cursorQuery, "id");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow2 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "tenantToken");
                @SuppressLint("RestrictedApi") int  columnIndexOrThrow3 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "latency");
                @SuppressLint("RestrictedApi") int columnIndexOrThrow4 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "persistence");
                int columnIndexOrThrow5 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "timestamp");
                int columnIndexOrThrow6 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "retryCount");
                int columnIndexOrThrow7 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "reservedUntil");
                int columnIndexOrThrow8 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "blob");
                StorageRecord[] storageRecordArr = new StorageRecord[cursorQuery.getCount()];
                while (cursorQuery.moveToNext()) {
                    storageRecordArr[i2] = new StorageRecord(cursorQuery.getLong(columnIndexOrThrow), cursorQuery.isNull(columnIndexOrThrow2) ? null : cursorQuery.getString(columnIndexOrThrow2), cursorQuery.getInt(columnIndexOrThrow3), cursorQuery.getInt(columnIndexOrThrow4), cursorQuery.getLong(columnIndexOrThrow5), cursorQuery.getInt(columnIndexOrThrow6), cursorQuery.getLong(columnIndexOrThrow7), cursorQuery.isNull(columnIndexOrThrow8) ? null : cursorQuery.getBlob(columnIndexOrThrow8));
                    i2++;
                }
                this.__db.setTransactionSuccessful();
                return storageRecordArr;
            } finally {
                cursorQuery.close();
                roomSQLiteQueryAcquire.release();
            }
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public StorageRecord[] getRetryExpired(long[] jArr, long j) {
        StringBuilder sbNewStringBuilder = StringUtil.newStringBuilder();
        sbNewStringBuilder.append("SELECT * FROM StorageRecord WHERE id IN (");
        int length = jArr.length;
        StringUtil.appendPlaceholders(sbNewStringBuilder, length);
        sbNewStringBuilder.append(") AND retryCount >= ");
        sbNewStringBuilder.append("?");
        int i = length + 1;
        RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire(sbNewStringBuilder.toString(), i);
        int i2 = 0;
        int i3 = 1;
        for (long j2 : jArr) {
            roomSQLiteQueryAcquire.bindLong(i3, j2);
            i3++;
        }
        roomSQLiteQueryAcquire.bindLong(i, j);
        this.__db.assertNotSuspendingTransaction();
        this.__db.beginTransaction();
        try {
            Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
            try {
                int columnIndexOrThrow = CursorUtil.getColumnIndexOrThrow(cursorQuery, "id");
                int columnIndexOrThrow2 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "tenantToken");
                int columnIndexOrThrow3 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "latency");
                int columnIndexOrThrow4 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "persistence");
                int columnIndexOrThrow5 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "timestamp");
                int columnIndexOrThrow6 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "retryCount");
                int columnIndexOrThrow7 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "reservedUntil");
                int columnIndexOrThrow8 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "blob");
                StorageRecord[] storageRecordArr = new StorageRecord[cursorQuery.getCount()];
                while (cursorQuery.moveToNext()) {
                    storageRecordArr[i2] = new StorageRecord(cursorQuery.getLong(columnIndexOrThrow), cursorQuery.isNull(columnIndexOrThrow2) ? null : cursorQuery.getString(columnIndexOrThrow2), cursorQuery.getInt(columnIndexOrThrow3), cursorQuery.getInt(columnIndexOrThrow4), cursorQuery.getLong(columnIndexOrThrow5), cursorQuery.getInt(columnIndexOrThrow6), cursorQuery.getLong(columnIndexOrThrow7), cursorQuery.isNull(columnIndexOrThrow8) ? null : cursorQuery.getBlob(columnIndexOrThrow8));
                    i2++;
                }
                this.__db.setTransactionSuccessful();
                return storageRecordArr;
            } finally {
                cursorQuery.close();
                roomSQLiteQueryAcquire.release();
            }
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public Long getMinLatency(long j) {
        @SuppressLint("RestrictedApi") RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT min(latency) FROM StorageRecord WHERE latency >= ? AND reservedUntil = 0", 1);
        roomSQLiteQueryAcquire.bindLong(1, j);
        this.__db.assertNotSuspendingTransaction();
        Long lValueOf = null;
        Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
        try {
            if (cursorQuery.moveToFirst() && !cursorQuery.isNull(0)) {
                lValueOf = Long.valueOf(cursorQuery.getLong(0));
            }
            return lValueOf;
        } finally {
            cursorQuery.close();
            roomSQLiteQueryAcquire.release();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public StorageRecord[] getUnreservedByLatency(long j, long j2) {
        RoomSQLiteQuery roomSQLiteQueryAcquire = RoomSQLiteQuery.acquire("SELECT * FROM StorageRecord WHERE latency = ? AND reservedUntil = 0 ORDER BY persistence DESC, timestamp ASC LIMIT ?", 2);
        roomSQLiteQueryAcquire.bindLong(1, j);
        roomSQLiteQueryAcquire.bindLong(2, j2);
        this.__db.assertNotSuspendingTransaction();
        this.__db.beginTransaction();
        try {
            int i = 0;
            Cursor cursorQuery = DBUtil.query(this.__db, roomSQLiteQueryAcquire, false, (CancellationSignal) null);
            try {
                int columnIndexOrThrow = CursorUtil.getColumnIndexOrThrow(cursorQuery, "id");
                int columnIndexOrThrow2 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "tenantToken");
                int columnIndexOrThrow3 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "latency");
                int columnIndexOrThrow4 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "persistence");
                int columnIndexOrThrow5 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "timestamp");
                int columnIndexOrThrow6 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "retryCount");
                int columnIndexOrThrow7 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "reservedUntil");
                int columnIndexOrThrow8 = CursorUtil.getColumnIndexOrThrow(cursorQuery, "blob");
                StorageRecord[] storageRecordArr = new StorageRecord[cursorQuery.getCount()];
                while (cursorQuery.moveToNext()) {
                    storageRecordArr[i] = new StorageRecord(cursorQuery.getLong(columnIndexOrThrow), cursorQuery.isNull(columnIndexOrThrow2) ? null : cursorQuery.getString(columnIndexOrThrow2), cursorQuery.getInt(columnIndexOrThrow3), cursorQuery.getInt(columnIndexOrThrow4), cursorQuery.getLong(columnIndexOrThrow5), cursorQuery.getInt(columnIndexOrThrow6), cursorQuery.getLong(columnIndexOrThrow7), cursorQuery.isNull(columnIndexOrThrow8) ? null : cursorQuery.getBlob(columnIndexOrThrow8));
                    i++;
                }
                this.__db.setTransactionSuccessful();
                return storageRecordArr;
            } finally {
                cursorQuery.close();
                roomSQLiteQueryAcquire.release();
            }
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int deleteByIdBlock(long[] jArr) {
        this.__db.assertNotSuspendingTransaction();
        StringBuilder sbNewStringBuilder = StringUtil.newStringBuilder();
        sbNewStringBuilder.append("DELETE FROM StorageRecord WHERE id IN (");
        StringUtil.appendPlaceholders(sbNewStringBuilder, jArr.length);
        sbNewStringBuilder.append(")");
        SupportSQLiteStatement supportSQLiteStatementCompileStatement = this.__db.compileStatement(sbNewStringBuilder.toString());
        int i = 1;
        for (long j : jArr) {
            supportSQLiteStatementCompileStatement.bindLong(i, j);
            i++;
        }
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementCompileStatement.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int setReservedBlock(long[] jArr, long j) {
        this.__db.assertNotSuspendingTransaction();
        StringBuilder sbNewStringBuilder = StringUtil.newStringBuilder();
        sbNewStringBuilder.append("UPDATE StorageRecord SET reservedUntil = ");
        sbNewStringBuilder.append("?");
        sbNewStringBuilder.append(" WHERE id IN (");
        StringUtil.appendPlaceholders(sbNewStringBuilder, jArr.length);
        sbNewStringBuilder.append(")");
        SupportSQLiteStatement supportSQLiteStatementCompileStatement = this.__db.compileStatement(sbNewStringBuilder.toString());
        supportSQLiteStatementCompileStatement.bindLong(1, j);
        int i = 2;
        for (long j2 : jArr) {
            supportSQLiteStatementCompileStatement.bindLong(i, j2);
            i++;
        }
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementCompileStatement.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int releaseAndIncrementRetryCounts(long[] jArr) {
        this.__db.assertNotSuspendingTransaction();
        StringBuilder sbNewStringBuilder = StringUtil.newStringBuilder();
        sbNewStringBuilder.append("UPDATE StorageRecord SET reservedUntil = 0, retryCount = retryCount + 1 WHERE id IN (");
        StringUtil.appendPlaceholders(sbNewStringBuilder, jArr.length);
        sbNewStringBuilder.append(")");
        SupportSQLiteStatement supportSQLiteStatementCompileStatement = this.__db.compileStatement(sbNewStringBuilder.toString());
        int i = 1;
        for (long j : jArr) {
            supportSQLiteStatementCompileStatement.bindLong(i, j);
            i++;
        }
        this.__db.beginTransaction();
        try {
            int iExecuteUpdateDelete = supportSQLiteStatementCompileStatement.executeUpdateDelete();
            this.__db.setTransactionSuccessful();
            return iExecuteUpdateDelete;
        } finally {
            this.__db.endTransaction();
        }
    }

    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}