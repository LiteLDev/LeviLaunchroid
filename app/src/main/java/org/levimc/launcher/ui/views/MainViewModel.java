package org.levimc.launcher.ui.views;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;

import java.util.List;

public class MainViewModel extends ViewModel {
    private final ModManager modManager;
    private final VersionManager versionManager;
    private final MutableLiveData<List<Mod>> modsLiveData = new MutableLiveData<>();

    public MainViewModel(ModManager modManager, VersionManager versionManager) {
        this.modManager = modManager;
        this.versionManager = versionManager;
        modManager.getModsChangedLiveData().observeForever(trigger -> refreshMods());
        refreshMods();
    }

    public void refreshMods() {
        new Thread(() -> {
            List<Mod> mods = modManager.getMods();
            modsLiveData.postValue(mods);
        }).start();
    }

    public void removeMod(Mod mod) {
        modManager.deleteMod(mod.getFileName());
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
}