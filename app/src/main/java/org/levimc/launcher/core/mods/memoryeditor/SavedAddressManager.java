package org.levimc.launcher.core.mods.memoryeditor;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SavedAddressManager {
    private static final String PREFS_NAME = "memory_editor_saved";
    private static final String KEY_ADDRESSES = "saved_addresses";
    private static volatile SavedAddressManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private List<MemoryAddress> savedAddresses = new ArrayList<>();

    private SavedAddressManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static SavedAddressManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SavedAddressManager.class) {
                if (instance == null) {
                    instance = new SavedAddressManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private void load() {
        String json = prefs.getString(KEY_ADDRESSES, "[]");
        Type type = new TypeToken<List<SavedEntry>>(){}.getType();
        List<SavedEntry> entries = gson.fromJson(json, type);
        savedAddresses.clear();
        if (entries != null) {
            for (SavedEntry e : entries) {
                MemoryAddress addr = new MemoryAddress(e.address, ValueType.fromId(e.typeId));
                addr.setLabel(e.label);
                addr.setFrozen(e.frozen);
                addr.setFrozenValue(e.frozenValue);
                addr.setOverlayEnabled(e.overlayEnabled);
                addr.setOverlayToggleable(e.overlayToggleable);
                addr.setOverlayOriginalValue(e.overlayOriginalValue != null ? e.overlayOriginalValue : "");
                addr.setOverlayNewValue(e.overlayNewValue != null ? e.overlayNewValue : "");
                addr.setOverlayName(e.overlayName != null ? e.overlayName : "");
                savedAddresses.add(addr);
            }
        }
    }

    private void save() {
        List<SavedEntry> entries = new ArrayList<>();
        for (MemoryAddress addr : savedAddresses) {
            SavedEntry e = new SavedEntry();
            e.address = addr.getAddress();
            e.typeId = addr.getType().getId();
            e.label = addr.getLabel();
            e.frozen = addr.isFrozen();
            e.frozenValue = addr.getFrozenValue();
            e.overlayEnabled = addr.isOverlayEnabled();
            e.overlayToggleable = addr.isOverlayToggleable();
            e.overlayOriginalValue = addr.getOverlayOriginalValue();
            e.overlayNewValue = addr.getOverlayNewValue();
            e.overlayName = addr.getOverlayName();
            entries.add(e);
        }
        prefs.edit().putString(KEY_ADDRESSES, gson.toJson(entries)).apply();
    }

    public List<MemoryAddress> getSavedAddresses() {
        return new ArrayList<>(savedAddresses);
    }

    public void addAddress(MemoryAddress address) {
        savedAddresses.add(address);
        save();
    }

    public void removeAddress(int index) {
        if (index >= 0 && index < savedAddresses.size()) {
            savedAddresses.remove(index);
            save();
        }
    }

    public void updateAddress(int index, MemoryAddress address) {
        if (index >= 0 && index < savedAddresses.size()) {
            savedAddresses.set(index, address);
            save();
        }
    }

    public void clearAll() {
        savedAddresses.clear();
        save();
    }

    public List<MemoryAddress> getOverlayEnabledAddresses() {
        List<MemoryAddress> result = new ArrayList<>();
        for (MemoryAddress addr : savedAddresses) {
            if (addr.isOverlayEnabled()) {
                result.add(addr);
            }
        }
        return result;
    }

    private static class SavedEntry {
        long address;
        int typeId;
        String label;
        boolean frozen;
        String frozenValue;
        boolean overlayEnabled;
        boolean overlayToggleable;
        String overlayOriginalValue;
        String overlayNewValue;
        String overlayName;
    }
}
