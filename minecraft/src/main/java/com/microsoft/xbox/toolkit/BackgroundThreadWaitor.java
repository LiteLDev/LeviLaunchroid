package com.microsoft.xbox.toolkit;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;


public class BackgroundThreadWaitor {
    private static BackgroundThreadWaitor instance = new BackgroundThreadWaitor();
    private final Hashtable<WaitType, WaitObject> blockingTable = new Hashtable<>();
    private final Ready waitReady = new Ready();
    private final ArrayList<Runnable> waitingRunnables = new ArrayList<>();
    private BackgroundThreadWaitorChangedCallback blockingChangedCallback = null;

    public static BackgroundThreadWaitor getInstance() {
        if (instance == null) {
            instance = new BackgroundThreadWaitor();
        }
        return instance;
    }

    public void waitForReady(int i) {
        XLEAssert.assertTrue(ThreadManager.UIThread != Thread.currentThread());
        ThreadManager.UIThreadPost(new Runnable() {
            public void run() {
                BackgroundThreadWaitor.this.updateWaitReady();
            }
        });
        this.waitReady.waitForReady(i);
    }

    public void setBlocking(WaitType waitType, int i) {
        XLEAssert.assertTrue(ThreadManager.UIThread == Thread.currentThread());
        this.blockingTable.put(waitType, new WaitObject(waitType, i));
        updateWaitReady();
    }

    public void clearBlocking(WaitType waitType) {
        XLEAssert.assertTrue(ThreadManager.UIThread == Thread.currentThread());
        this.blockingTable.remove(waitType);
        updateWaitReady();
    }

    public void setChangedCallback(BackgroundThreadWaitorChangedCallback backgroundThreadWaitorChangedCallback) {
        this.blockingChangedCallback = backgroundThreadWaitorChangedCallback;
    }

    public boolean isBlocking() {
        return !this.waitReady.getIsReady();
    }

    public void updateWaitReady() {
        boolean blocking;
        XLEAssert.assertTrue(ThreadManager.UIThread == Thread.currentThread());
        HashSet<WaitType> waitTypesToRemove = new HashSet<>();
        EnumSet<WaitType> blockingTypes = EnumSet.noneOf(WaitType.class);
        Enumeration<WaitObject> e = blockingTable.elements();
        while (e.hasMoreElements()) {
            WaitObject waitObject = e.nextElement();
            if (waitObject.isExpired()) {
                waitTypesToRemove.add(waitObject.type);
            } else {
                blockingTypes.add(waitObject.type);
            }
        }
        Iterator<WaitType> it = waitTypesToRemove.iterator();
        while (it.hasNext()) {
            blockingTable.remove(it.next());
        }
        if (blockingTable.size() == 0) {
            waitReady.setReady();
            drainWaitingRunnables();
            blocking = false;
        } else {
            waitReady.reset();
            blocking = true;
        }
        if (blockingChangedCallback != null) {
            blockingChangedCallback.run(blockingTypes, blocking);
        }
    }

    public void postRunnableAfterReady(Runnable runnable) {
        XLEAssert.assertTrue(ThreadManager.UIThread == Thread.currentThread());
        if (runnable != null) {
            if (!isBlocking()) {
                runnable.run();
            } else {
                this.waitingRunnables.add(runnable);
            }
        }
    }

    private void drainWaitingRunnables() {
        XLEAssert.assertTrue(ThreadManager.UIThread == Thread.currentThread());
        Iterator<Runnable> it = this.waitingRunnables.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
        this.waitingRunnables.clear();
    }

    public enum WaitType {
        Navigation,
        ApplicationBar,
        ListScroll,
        ListLayout,
        PivotScroll
    }

    public interface BackgroundThreadWaitorChangedCallback {
        void run(EnumSet<WaitType> enumSet, boolean z);
    }

    private class WaitObject {
        private final long expires;
        public WaitType type;

        public WaitObject(WaitType waitType, long j) {
            this.type = waitType;
            this.expires = SystemClock.uptimeMillis() + j;
        }

        public boolean isExpired() {
            return this.expires < SystemClock.uptimeMillis();
        }
    }
}
