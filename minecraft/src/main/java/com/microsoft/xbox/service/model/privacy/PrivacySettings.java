package com.microsoft.xbox.service.model.privacy;

import org.jetbrains.annotations.NotNull;


public class PrivacySettings {

    public enum PrivacySettingId {
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
        CanShareIdentity;

        public static PrivacySettingId getPrivacySettingId(String str) {
            for (PrivacySettingId privacySettingId : values()) {
                if (privacySettingId.name().equalsIgnoreCase(str)) {
                    return privacySettingId;
                }
            }
            return None;
        }
    }

    public enum PrivacySettingValue {
        NotSet,
        Everyone,
        PeopleOnMyList,
        FriendCategoryShareIdentity,
        Blocked;

        public static PrivacySettingValue getPrivacySettingValue(String str) {
            for (PrivacySettingValue privacySettingValue : values()) {
                if (privacySettingValue.name().equalsIgnoreCase(str)) {
                    return privacySettingValue;
                }
            }
            return NotSet;
        }
    }

    public static class PrivacySetting {
        public String setting;
        public String value;
        private PrivacySettingId settingId;
        private PrivacySettingValue settingValue;

        public PrivacySetting() {
        }

        public PrivacySetting(@NotNull PrivacySettingId privacySettingId, @NotNull PrivacySettingValue privacySettingValue) {
            this.setting = privacySettingId.name();
            this.value = privacySettingValue.name();
        }

        public PrivacySettingId getPrivacySettingId() {
            PrivacySettingId privacySettingId = PrivacySettingId.getPrivacySettingId(this.setting);
            this.settingId = privacySettingId;
            return privacySettingId;
        }

        public void setPrivacySettingId(@NotNull PrivacySettingId privacySettingId) {
            this.setting = privacySettingId.name();
            this.settingId = privacySettingId;
        }

        public PrivacySettingValue getPrivacySettingValue() {
            PrivacySettingValue privacySettingValue = PrivacySettingValue.getPrivacySettingValue(this.value);
            this.settingValue = privacySettingValue;
            return privacySettingValue;
        }
    }
}
