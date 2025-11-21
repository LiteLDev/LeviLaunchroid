package org.levimc.launcher.core.content;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.levimc.launcher.core.versions.GameVersion;

import java.io.File;
import java.util.List;

public class ContentManager {
    private static ContentManager instance;
    
    private final Context context;
    private final WorldManager worldManager;
    private final ResourcePackManager resourcePackManager;
    
    private GameVersion currentVersion;
    private final MutableLiveData<List<WorldItem>> worldsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ResourcePackItem>> resourcePacksLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ResourcePackItem>> behaviorPacksLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ResourcePackItem>> skinPacksLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> statusLiveData = new MutableLiveData<>();

    private ContentManager(Context context) {
        this.context = context.getApplicationContext();
        this.worldManager = new WorldManager(this.context);
        this.resourcePackManager = new ResourcePackManager(this.context);
    }

    public static synchronized ContentManager getInstance(Context context) {
        if (instance == null) {
            instance = new ContentManager(context);
        }
        return instance;
    }

    public void setCurrentVersion(GameVersion version) {
        this.currentVersion = version;
        worldManager.setCurrentVersion(version);
        resourcePackManager.setCurrentVersion(version);
        refreshContent();
    }

    public void setStorageDirectories(File worldsDir, File resourcePacksDir, File behaviorPacksDir, File skinPacksDir) {
        worldManager.setWorldsDirectory(worldsDir);
        resourcePackManager.setPackDirectories(resourcePacksDir, behaviorPacksDir, skinPacksDir);
        refreshContent();
    }

    public void refreshContent() {
        refreshWorlds();
        refreshResourcePacks();
        refreshBehaviorPacks();
        refreshSkinPacks();
    }

    public void refreshWorlds() {
        List<WorldItem> worlds = worldManager.getWorlds();
        worldsLiveData.postValue(worlds);
    }

    public void refreshResourcePacks() {
        List<ResourcePackItem> resourcePacks = resourcePackManager.getResourcePacks();
        resourcePacksLiveData.postValue(resourcePacks);
    }

    public void refreshBehaviorPacks() {
        List<ResourcePackItem> behaviorPacks = resourcePackManager.getBehaviorPacks();
        behaviorPacksLiveData.postValue(behaviorPacks);
    }

    public void refreshSkinPacks() {
        List<ResourcePackItem> skinPacks = resourcePackManager.getSkinPacks();
        skinPacksLiveData.postValue(skinPacks);
    }

    public LiveData<List<WorldItem>> getWorldsLiveData() {
        return worldsLiveData;
    }

    public LiveData<List<ResourcePackItem>> getResourcePacksLiveData() {
        return resourcePacksLiveData;
    }

    public LiveData<List<ResourcePackItem>> getBehaviorPacksLiveData() {
        return behaviorPacksLiveData;
    }

    public LiveData<List<ResourcePackItem>> getSkinPacksLiveData() {
        return skinPacksLiveData;
    }

    public LiveData<String> getStatusLiveData() {
        return statusLiveData;
    }

    public void setStatus(String status) {
        statusLiveData.postValue(status);
    }

    public void shutdown() {
        worldManager.shutdown();
        resourcePackManager.shutdown();
    }
    
    public void importWorld(android.net.Uri worldUri, WorldManager.WorldOperationCallback callback) {
        setStatus("Importing world...");
        worldManager.importWorld(worldUri, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                refreshWorlds();
                setStatus(message);
                if (callback != null) callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                setStatus("Import failed: " + error);
                if (callback != null) callback.onError(error);
            }

            @Override
            public void onProgress(int progress) {
                setStatus("Importing world... " + progress + "%");
                if (callback != null) callback.onProgress(progress);
            }
        });
    }

    public void exportWorld(WorldItem world, android.net.Uri exportUri, WorldManager.WorldOperationCallback callback) {
        setStatus("Exporting world...");
        worldManager.exportWorld(world, exportUri, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                setStatus(message);
                if (callback != null) callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                setStatus("Export failed: " + error);
                if (callback != null) callback.onError(error);
            }

            @Override
            public void onProgress(int progress) {
                setStatus("Exporting world... " + progress + "%");
                if (callback != null) callback.onProgress(progress);
            }
        });
    }

    public void deleteWorld(WorldItem world, WorldManager.WorldOperationCallback callback) {
        setStatus("Deleting world...");
        worldManager.deleteWorld(world, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                refreshWorlds();
                setStatus(message);
                if (callback != null) callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                setStatus("Delete failed: " + error);
                if (callback != null) callback.onError(error);
            }

            @Override
            public void onProgress(int progress) {
                if (callback != null) callback.onProgress(progress);
            }
        });
    }

    public void backupWorld(WorldItem world, WorldManager.WorldOperationCallback callback) {
        setStatus("Creating backup...");
        worldManager.backupWorld(world, new WorldManager.WorldOperationCallback() {
            @Override
            public void onSuccess(String message) {
                setStatus(message);
                if (callback != null) callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                setStatus("Backup failed: " + error);
                if (callback != null) callback.onError(error);
            }

            @Override
            public void onProgress(int progress) {
                setStatus("Creating backup... " + progress + "%");
                if (callback != null) callback.onProgress(progress);
            }
        });
    }

    public void importResourcePack(android.net.Uri packUri, ResourcePackManager.PackOperationCallback callback) {
        setStatus("Importing resource pack...");
        resourcePackManager.importPack(packUri, new ResourcePackManager.PackOperationCallback() {
            @Override
            public void onSuccess(String message) {
                refreshResourcePacks();
                refreshBehaviorPacks();
                setStatus(message);
                if (callback != null) callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                setStatus("Import failed: " + error);
                if (callback != null) callback.onError(error);
            }

            @Override
            public void onProgress(int progress) {
                setStatus("Importing resource pack... " + progress + "%");
                if (callback != null) callback.onProgress(progress);
            }
        });
    }

    public void deleteResourcePack(ResourcePackItem pack, ResourcePackManager.PackOperationCallback callback) {
        setStatus("Deleting resource pack...");
        resourcePackManager.deletePack(pack, new ResourcePackManager.PackOperationCallback() {
            @Override
            public void onSuccess(String message) {
                refreshResourcePacks();
                refreshBehaviorPacks();
                setStatus(message);
                if (callback != null) callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                setStatus("Delete failed: " + error);
                if (callback != null) callback.onError(error);
            }

            @Override
            public void onProgress(int progress) {
                if (callback != null) callback.onProgress(progress);
            }
        });
    }
}