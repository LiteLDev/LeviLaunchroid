package com.microsoft.applications.events;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class OfflineRoom implements AutoCloseable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    OfflineRoomDatabase m_db;
    long m_pageSize;
    StorageSettingDao m_settingDao;
    StorageRecordDao m_srDao;

    public static native void connectContext(Context context);

    class TrimTransaction implements Callable<Long> {
        long m_byteLimit;
        OfflineRoom m_room;

        public TrimTransaction(OfflineRoom offlineRoom, long j) {
            this.m_room = offlineRoom;
            this.m_byteLimit = j;
        }

        protected long vacuum(long j) {
            Cursor cursorQuery = this.m_room.m_db.query("VACUUM", null);
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            long j2 = this.m_room.totalSize();
            Log.i("MAE", String.format("Vacuum: %d before, %d after", Long.valueOf(j), Long.valueOf(j2)));
            return j2;
        }

        @Override
        public Long call() {
            OfflineRoom offlineRoom = this.m_room;
            if (offlineRoom == null || this.m_byteLimit == 0) {
                return null;
            }
            long jVacuum = offlineRoom.totalSize();
            if (jVacuum <= this.m_byteLimit) {
                return new Long(0L);
            }
            try {
                jVacuum = vacuum(jVacuum);
            } catch (Exception e) {
                Log.e("MAE", "Exception in VACUUM", e);
            }
            if (jVacuum <= this.m_byteLimit) {
                return new Long(0L);
            }
            long jCeil = (long) Math.ceil((this.m_byteLimit > this.m_room.m_pageSize ? Math.max(0.25d, 1.0d - (this.m_byteLimit / jVacuum)) : 0.25d) * this.m_room.m_srDao.totalRecordCount());
            if (jCeil <= 0) {
                return new Long(0L);
            }
            long jTrim = this.m_room.m_srDao.trim(jCeil);
            long jVacuum2 = this.m_room.totalSize();
            if (jVacuum2 > this.m_byteLimit) {
                jVacuum2 = vacuum(jVacuum2);
            }
            Log.i("MAE", String.format("Trim: dropped %d records, new size %d bytes", Long.valueOf(jTrim), Long.valueOf(jVacuum2)));
            return new Long(jTrim);
        }
    }

    public OfflineRoom(Context context, String str) {
        RoomDatabase.Builder builderDatabaseBuilder;
        this.m_db = null;
        this.m_srDao = null;
        this.m_settingDao = null;
        this.m_pageSize = 4096L;
        if (str.equals(":memory:")) {
            builderDatabaseBuilder = Room.inMemoryDatabaseBuilder(context, OfflineRoomDatabase.class);
        } else {
            builderDatabaseBuilder = Room.databaseBuilder(context, OfflineRoomDatabase.class, str);
        }
        builderDatabaseBuilder.fallbackToDestructiveMigration();
        builderDatabaseBuilder.setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING);
        OfflineRoomDatabase offlineRoomDatabase = (OfflineRoomDatabase) builderDatabaseBuilder.build();
        this.m_db = offlineRoomDatabase;
        this.m_srDao = offlineRoomDatabase.getStorageRecordDao();
        this.m_settingDao = this.m_db.getStorageSettingDao();
        Cursor cursorQuery = this.m_db.query("PRAGMA page_size", null);
        try {
            if (cursorQuery.getCount() == 1 && cursorQuery.getColumnCount() == 1) {
                cursorQuery.moveToFirst();
                this.m_pageSize = cursorQuery.getLong(0);
            } else {
                Log.e("MAE", String.format("Unexpected result from PRAGMA page_size: %d rows, %d columns", Integer.valueOf(cursorQuery.getCount()), Integer.valueOf(cursorQuery.getColumnCount())));
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            cursorQuery = this.m_db.query("PRAGMA page_count", null);
            try {
                if (cursorQuery.getCount() == 1 && cursorQuery.getColumnCount() == 1) {
                    cursorQuery.moveToFirst();
                    cursorQuery.getLong(0);
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } finally {
            }
        } finally {
        }
    }

    public long[] storeRecords(StorageRecord... storageRecordArr) {
        return this.m_srDao.insertRecords(storageRecordArr);
    }

    @Override
    public void close() {
        if (this.m_db.isOpen()) {
            this.m_db.close();
        }
        this.m_srDao = null;
        this.m_settingDao = null;
        this.m_db = null;
    }

    public void explain(String str) {
        Cursor cursorQuery = this.m_db.query("EXPLAIN QUERY PLAN " + str, null);
        try {
            int count = cursorQuery.getCount();
            int columnCount = cursorQuery.getColumnCount();
            cursorQuery.moveToFirst();
            String[] columnNames = cursorQuery.getColumnNames();
            for (int i = 0; i < columnNames.length; i++) {
                Log.i("MAE", String.format("e", columnNames[i], Integer.valueOf(i), Integer.valueOf(cursorQuery.getType(i))));
            }
            for (int i2 = 0; i2 < count; i2++) {
                if (!cursorQuery.moveToPosition(i2)) {
                    break;
                }
                for (int i3 = 0; i3 < columnCount; i3++) {
                    Log.i("MAE", String.format("%d %s: %s", Integer.valueOf(i2), columnNames[i3], cursorQuery.getString(i3)));
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th) {
            if (cursorQuery != null) {
                try {
                    cursorQuery.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public long[] storeFromBuffersIds(int i, int[] iArr, byte[] bArr, int[] iArr2, long[] jArr) {
        int i2;
        StorageRecord[] storageRecordArr = new StorageRecord[i];
        int i3 = 0;
        for (int i4 = 0; i4 < i; i4++) {
            int i5 = i4 * 3;
            long j = jArr[i5];
            int i6 = i4 * 2;
            String str = new String(bArr, i3, iArr[i6]);
            int i7 = i3 + iArr[i6];
            int i8 = i6 + 1;
            byte[] bArr2 = new byte[iArr[i8]];
            int i9 = 0;
            while (true) {
                i2 = iArr[i8];
                if (i9 < i2) {
                    bArr2[i9] = bArr[i9 + i7];
                    i9++;
                }
            }
        }
        return storeRecords(storageRecordArr);
    }

    public void storeFromBuffers(int i, int[] iArr, byte[] bArr, int[] iArr2, long[] jArr) {
        storeFromBuffersIds(i, iArr, bArr, iArr2, jArr);
    }

    public long getRecordCount(int i) {
        if (i == -1) {
            return this.m_srDao.totalRecordCount();
        }
        return this.m_srDao.recordCount(i);
    }

    public long trim(long j) {
        if (totalSize() <= j) {
            return 0L;
        }
        Log.i("MAE", "Start trim");
        Long l = (Long) this.m_db.runInTransaction(new TrimTransaction(this, j));
        if (l == null) {
            Log.e("MAE", "Null result from trim");
            return 0L;
        }
        Log.i("MAE", String.format("Dropped %d records in trim", l));
        return l.longValue();
    }

    public ByTenant[] releaseRecords(long[] jArr, boolean z, long j) {
        TreeMap<String, Long> treeMap = new TreeMap<>();
        this.m_srDao.releaseRecords(jArr, z, j, treeMap);
        ByTenant[] byTenantArr = new ByTenant[treeMap.size()];
        int i = 0;
        for (Map.Entry<String, Long> entryFirstEntry = treeMap.firstEntry(); entryFirstEntry != null; entryFirstEntry = treeMap.higherEntry(entryFirstEntry.getKey())) {
            byTenantArr[i] = new ByTenant(entryFirstEntry.getKey(), entryFirstEntry.getValue());
            i++;
        }
        return byTenantArr;
    }

    public long deleteAllRecords() {
        return this.m_srDao.deleteAllRecords();
    }

    public long storeSetting(String str, String str2) {
        return this.m_settingDao.setValue(str, str2);
    }

    public String getSetting(String str) {
        StorageSetting[] values = this.m_settingDao.getValues(str);
        if (values.length > 0) {
            return values[0].value;
        }
        return "";
    }

    public long totalSize() {
        Cursor cursorQuery = this.m_db.query("PRAGMA page_count", null);
        try {
            cursorQuery.moveToFirst();
            long j = cursorQuery.getLong(0) * this.m_pageSize;
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return j;
        } catch (Throwable th) {
            if (cursorQuery != null) {
                try {
                    cursorQuery.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public StorageRecord[] getRecords(boolean z, int i, long j) {
        return this.m_srDao.getRecords(z, i, j);
    }

    public long deleteById(long[] jArr) {
        return this.m_srDao.deleteById(jArr);
    }

    public long deleteByToken(String str) {
        return this.m_srDao.deleteRecordsByToken(str);
    }

    public StorageRecord[] getAndReserve(int i, long j, long j2, long j3) {
        return this.m_srDao.getAndReserve(i, j, j2, j3);
    }

    public void releaseUnconsumed(StorageRecord[] storageRecordArr, int i) {
        this.m_srDao.releaseUnconsumed(storageRecordArr, i);
    }

    public void deleteAllSettings() {
        this.m_settingDao.deleteAllSettings();
    }

    public void deleteSetting(String str) {
        this.m_settingDao.deleteSetting(str);
    }
}