package org.levimc.launcher.core.mods.memoryeditor;

import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FreezeManager {
    private static volatile FreezeManager instance;
    private final List<MemoryAddress> frozenAddresses = new CopyOnWriteArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private static final int FREEZE_INTERVAL = 50;

    private final Runnable freezeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            for (MemoryAddress addr : frozenAddresses) {
                if (addr.isFrozen() && !addr.getFrozenValue().isEmpty()) {
                    addr.writeValue(addr.getFrozenValue());
                }
            }
            handler.postDelayed(this, FREEZE_INTERVAL);
        }
    };

    private FreezeManager() {}

    public static FreezeManager getInstance() {
        if (instance == null) {
            synchronized (FreezeManager.class) {
                if (instance == null) {
                    instance = new FreezeManager();
                }
            }
        }
        return instance;
    }

    public void start() {
        if (!running) {
            running = true;
            handler.post(freezeRunnable);
        }
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(freezeRunnable);
    }

    public void addFrozenAddress(MemoryAddress address) {
        if (!frozenAddresses.contains(address)) {
            frozenAddresses.add(address);
        }
    }

    public void removeFrozenAddress(MemoryAddress address) {
        frozenAddresses.remove(address);
    }

    public void clearAll() {
        frozenAddresses.clear();
    }

    public boolean isRunning() {
        return running;
    }
}
