package com.microsoft.xbox.idp.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class Profile {

    public static @NotNull GsonBuilder registerAdapters(@NotNull GsonBuilder gsonBuilder) {
        return gsonBuilder.registerTypeAdapter(new TypeToken<Map<SettingId, String>>() {
        }.getType(), new SettingsAdapter());
    }

    public enum SettingId {
        AppDisplayName,
        GameDisplayName,
        Gamertag,
        RealName,
        FirstName,
        LastName,
        AppDisplayPicRaw,
        GameDisplayPicRaw,
        AccountTier,
        TenureLevel,
        Gamerscore,
        PreferredColor,
        Watermarks,
        XboxOneRep,
        Background,
        PublicGamerpicType,
        ShowUserAsAvatar,
        TileTransparency
    }

    public static final class GamerpicChoiceList {
        public List<GamerpicListEntry> gamerpics;
    }

    public static final class GamerpicListEntry {
        public String id;
    }

    public static final class GamerpicUpdateResponse {
    }

    public static final class Response {
        public User[] profileUsers;
    }

    public static final class Setting {
        public SettingId id;
        public String value;
    }

    public static final class User {
        public String id;
        public boolean isSponsoredUser;
        public Map<SettingId, String> settings;
    }

    public static final class GamerpicChangeRequest {
        public UserSetting userSetting;

        public GamerpicChangeRequest(String str) {
            this.userSetting = new UserSetting("PublicGamerpic", str);
        }
    }

    public static final class UserSetting {
        public String id;
        public String value;

        public UserSetting(String str, String str2) {
            this.id = str;
            this.value = str2;
        }
    }

    private static class SettingsAdapter extends TypeAdapter<Map<SettingId, String>> {
        private SettingsAdapter() {
        }

        public void write(JsonWriter jsonWriter, @NotNull Map<SettingId, String> map) throws IOException {
            Setting[] settingArr = new Setting[map.size()];
            int i = -1;
            for (Map.Entry next : map.entrySet()) {
                Setting setting = new Setting();
                setting.id = (SettingId) next.getKey();
                setting.value = (String) next.getValue();
                i++;
                settingArr[i] = setting;
            }
            new Gson().toJson(settingArr, Setting[].class, jsonWriter);
        }

        public Map<SettingId, String> read(JsonReader jsonReader) throws IOException {
            Setting[] settingArr = new Gson().fromJson(jsonReader, Setting[].class);
            HashMap hashMap = new HashMap();
            for (Setting setting : settingArr) {
                hashMap.put(setting.id, setting.value);
            }
            return hashMap;
        }
    }
}
