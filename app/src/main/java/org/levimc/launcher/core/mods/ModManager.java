package org.levimc.launcher.core.mods;

import android.os.FileObserver;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;

import org.levimc.launcher.core.versions.GameVersion;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class ModManager {
    private static ModManager instance;
    private File modsDir;
    private File configFile;
    private Map<String, Boolean> enabledMap = new LinkedHashMap<>();
    private List<String> modOrder = new ArrayList<>();
    private FileObserver modDirObserver;
    private GameVersion currentVersion;
    private final MutableLiveData<Void> modsChangedLiveData = new MutableLiveData<>();
    private final Gson gson = new Gson();

    private ModManager() {
        if (currentVersion != null && currentVersion.modsDir != null) {
            modsDir = currentVersion.modsDir;
            configFile = new File(modsDir, "mods_config.json");
            if (!modsDir.exists()) modsDir.mkdirs();
            loadConfig();
            initFileObserver();
        } else {
            modsDir = null;
            configFile = null;
        }
    }

    public static synchronized ModManager getInstance() {
        if (instance == null) {
            instance = new ModManager();
        }
        return instance;
    }

    public synchronized void setCurrentVersion(GameVersion version) {
        if (Objects.equals(this.currentVersion, version)) return;
        stopFileObserver();

        this.currentVersion = version;
        if (currentVersion != null) {
            this.modsDir = currentVersion.modsDir;
            if (!modsDir.exists()) modsDir.mkdirs();
            this.configFile = new File(modsDir, "mods_config.json");
            loadConfig();
            initFileObserver();
        } else {
            modsDir = null;
            configFile = null;
            enabledMap = new LinkedHashMap<>();
            modOrder = new ArrayList<>();
        }
        postModChanged();
    }

    public GameVersion getCurrentVersion() {
        return currentVersion;
    }

    public List<Mod> getMods() {
        if (currentVersion == null || modsDir == null) return new ArrayList<>();
        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".so"));
        List<Mod> mods = new ArrayList<>();
        boolean changed = false;

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (!enabledMap.containsKey(fileName)) {
                    enabledMap.put(fileName, true);
                    modOrder.add(fileName);
                    changed = true;
                }
            }
        }

        List<String> toRemove = new ArrayList<>();
        for (String key : enabledMap.keySet()) {
            boolean found = false;
            if (files != null) {
                for (File file : files) {
                    if (file.getName().equals(key)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) toRemove.add(key);
        }
        for (String rm : toRemove) {
            enabledMap.remove(rm);
            modOrder.remove(rm);
            changed = true;
        }

        for (int i = 0; i < modOrder.size(); i++) {
            String fileName = modOrder.get(i);
            boolean enabled = Boolean.TRUE.equals(enabledMap.get(fileName));
            mods.add(new Mod(fileName, enabled, i));
        }

        if (changed) saveConfig();
        return mods;
    }

    public synchronized void setModEnabled(String fileName, boolean enabled) {
        if (currentVersion == null || modsDir == null) return;
        if (!fileName.endsWith(".so")) fileName += ".so";
        if (enabledMap.containsKey(fileName)) {
            enabledMap.put(fileName, enabled);
            saveConfig();
        }
    }

    private void loadConfig() {
        enabledMap = new LinkedHashMap<>();
        modOrder = new ArrayList<>();
        if (!configFile.exists()) {
            File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".so"));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    enabledMap.put(fileName, true);
                    modOrder.add(fileName);
                }
            }
            saveConfig();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> configList = gson.fromJson(reader, listType);
            if (configList != null) {
                for (Map<String, Object> item : configList) {
                    String name = (String) item.get("name");
                    Boolean enabled = (Boolean) item.get("enabled");
                    if (name != null && enabled != null) {
                        enabledMap.put(name, enabled);
                        modOrder.add(name);
                    }
                }
                return;
            }
        } catch (Exception e) {
        }

        try (FileReader reader = new FileReader(configFile)) {
            Type mapType = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> oldMap = gson.fromJson(reader, mapType);
            if (oldMap != null) {
                for (Map.Entry<String, Boolean> entry : oldMap.entrySet()) {
                    enabledMap.put(entry.getKey(), entry.getValue());
                    modOrder.add(entry.getKey());
                }
                saveConfig();
            }
        } catch (Exception ignored) {
        }
    }

    private void saveConfig() {
        if (configFile == null) return;
        try (FileWriter writer = new FileWriter(configFile)) {
            List<Map<String, Object>> configList = new ArrayList<>();
            for (int i = 0; i < modOrder.size(); i++) {
                String fileName = modOrder.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("name", fileName);
                item.put("enabled", enabledMap.get(fileName));
                item.put("order", i);
                configList.add(item);
            }
            gson.toJson(configList, writer);
        } catch (Exception ignored) {
        }
    }

    private synchronized void initFileObserver() {
        if (modsDir == null) return;
        modDirObserver = new FileObserver(modsDir.getAbsolutePath(), FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                postModChanged();
            }
        };
        modDirObserver.startWatching();
    }

    private void stopFileObserver() {
        if (modDirObserver != null) {
            try {
                modDirObserver.stopWatching();
            } catch (Exception ignored) {
            }
            modDirObserver = null;
        }
    }

    public synchronized void deleteMod(String fileName) {
        if (currentVersion == null || modsDir == null) return;
        if (!fileName.endsWith(".so")) fileName += ".so";
        File modFile = new File(modsDir, fileName);
        if (modFile.exists()) modFile.delete();
        enabledMap.remove(fileName);
        modOrder.remove(fileName);
        saveConfig();
        postModChanged();
    }

    public synchronized void reorderMods(List<Mod> reorderedMods) {
        if (currentVersion == null || modsDir == null) return;

        modOrder.clear();
        for (Mod mod : reorderedMods) {
            modOrder.add(mod.getFileName());
        }

        saveConfig();
        postModChanged();
    }

    private void postModChanged() {
        modsChangedLiveData.postValue(null);
    }

    public MutableLiveData<Void> getModsChangedLiveData() {
        return modsChangedLiveData;
    }

    public synchronized void refreshMods() {
        postModChanged();
    }
}
