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
import java.util.Map;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class Privacy {

    public static @NotNull GsonBuilder registerAdapters(@NotNull GsonBuilder gsonBuilder) {
        return gsonBuilder.registerTypeAdapter(new TypeToken<Map<Key, Value>>() {
        }.getType(), new SettingsAdapter());
    }

    public enum Key {
        None,
        ShareFriendList,
        ShareGameHistory,
        CommunicateUsingTextAndVoice,
        SharePresence,
        ShareProfile,
        ShareVideoAndMusicStatus,
        CommunicateUsingVideo,
        CollectVoiceData,
        ShareXboxMusicActivity,
        ShareExerciseInfo,
        ShareIdentity,
        ShareRecordedGameSessions,
        ShareIdentityTransitively,
        CanShareIdentity
    }

    public enum Value {
        NotSet,
        Everyone,
        PeopleOnMyList,
        FriendCategoryShareIdentity,
        Blocked
    }

    public static class Setting {
        public Key setting;
        public Value value;
    }

    public static class Settings {
        public Map<Key, Value> settings;

        public static @NotNull Settings newWithMap() {
            Settings settings2 = new Settings();
            settings2.settings = new HashMap<>();
            return settings2;
        }

        public boolean isSettingSet(Key key) {
            Value value;
            return settings != null && (value = settings.get(key)) != null && value != Value.NotSet;
        }
    }

    private static class SettingsAdapter extends TypeAdapter<Map<Key, Value>> {
        private SettingsAdapter() {
        }

        public void write(JsonWriter jsonWriter, @NotNull Map<Key, Value> map) throws IOException {
            Setting[] settingArr = new Setting[map.size()];
            int i = -1;
            for (Map.Entry<Key, Value> next : map.entrySet()) {
                Setting setting = new Setting();
                setting.setting = next.getKey();
                setting.value = next.getValue();
                i++;
                settingArr[i] = setting;
            }
            new Gson().toJson(settingArr, Setting[].class, jsonWriter);
        }

        @NotNull
        public Map<Key, Value> read(JsonReader jsonReader) throws IOException {
            Setting[] settingArr = new Gson().fromJson(jsonReader, Setting[].class);
            HashMap<Key, Value> hashMap = new HashMap<>();
            for (Setting setting : settingArr) {
                if (!(setting.setting == null || setting.value == null)) {
                    hashMap.put(setting.setting, setting.value);
                }
            }
            return hashMap;
        }
    }
}
