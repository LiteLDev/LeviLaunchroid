package com.microsoft.xbox.service.model.privacy;

import com.microsoft.xbox.toolkit.GsonUtil;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class PrivacySettingsResult {
    public ArrayList<PrivacySettings.PrivacySetting> settings;

    public PrivacySettingsResult() {
    }

    public PrivacySettingsResult(ArrayList<PrivacySettings.PrivacySetting> arrayList) {
        this.settings = new ArrayList<>(arrayList);
    }

    public static PrivacySettingsResult deserialize(String str) {
        return GsonUtil.deserializeJson(str, PrivacySettingsResult.class);
    }

    public static @Nullable String getPrivacySettingRequestBody(PrivacySettingsResult privacySettingsResult) {
        try {
            return GsonUtil.toJsonString(privacySettingsResult);
        } catch (Exception unused) {
            return null;
        }
    }

    public String getShareRealNameStatus() {
        Iterator<PrivacySettings.PrivacySetting> it = this.settings.iterator();
        while (it.hasNext()) {
            PrivacySettings.PrivacySetting next = it.next();
            if (next.getPrivacySettingId() == PrivacySettings.PrivacySettingId.ShareIdentity) {
                return next.value;
            }
        }
        return PrivacySettings.PrivacySettingValue.PeopleOnMyList.name();
    }

    public boolean getSharingRealNameTransitively() {
        Iterator<PrivacySettings.PrivacySetting> it = this.settings.iterator();
        while (it.hasNext()) {
            PrivacySettings.PrivacySetting next = it.next();
            if (next.getPrivacySettingId() == PrivacySettings.PrivacySettingId.ShareIdentityTransitively) {
                return next.value.equalsIgnoreCase(PrivacySettings.PrivacySettingValue.Everyone.name());
            }
        }
        return false;
    }
}
