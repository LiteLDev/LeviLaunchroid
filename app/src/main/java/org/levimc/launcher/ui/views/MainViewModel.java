package org.levimc.launcher.ui.views;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.core.versions.GameVersion;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class MainViewModel extends ViewModel {
    private final ModManager modManager;
    private final MutableLiveData<List<Mod>> modsLiveData = new MutableLiveData<>();
    private final ExecutorService modsExecutor = Executors.newSingleThreadExecutor();
    private final AtomicLong refreshGeneration = new AtomicLong();
    private final Observer<Void> modsChangedObserver = trigger -> refreshMods();

    public MainViewModel(ModManager modManager) {
        this.modManager = modManager;
        modManager.getModsChangedLiveData().observeForever(modsChangedObserver);
        refreshMods();
    }

    public void refreshMods() {
        loadMods(false);
    }

    public void reloadMods() {
        loadMods(true);
    }

    private void loadMods(boolean invalidateCache) {
        long generation = refreshGeneration.incrementAndGet();
        modsExecutor.execute(() -> {
            List<Mod> mods = invalidateCache ? modManager.reloadMods() : modManager.getMods();
            if (generation == refreshGeneration.get()) {
                modsLiveData.postValue(mods);
            }
        });
    }

    public void removeMod(Mod mod) {
        modManager.deleteMod(mod.getId());
        refreshMods();
    }

    public void setCurrentVersion(GameVersion version) {
        modManager.setCurrentVersion(version);
        refreshMods();
    }

    public LiveData<List<Mod>> getModsLiveData() {
        return modsLiveData;
    }

    public void setModEnabled(String fileName, boolean enabled) {
        new Thread(() -> modManager.setModEnabled(fileName, enabled)).start();
    }

    public void reorderMods(List<Mod> reorderedMods) {
        new Thread(() -> {
            modManager.reorderMods(reorderedMods);
            refreshMods();
        }).start();
    }

    @Override
    protected void onCleared() {
        modManager.getModsChangedLiveData().removeObserver(modsChangedObserver);
        refreshGeneration.incrementAndGet();
        modsExecutor.shutdownNow();
        super.onCleared();
    }
}
