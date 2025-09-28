package com.microsoft.xbox.service.network.managers;

import com.microsoft.xbox.service.model.sls.UserProfileSetting;
import com.microsoft.xbox.toolkit.GsonUtil;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public interface IUserProfileResult {

    class Settings {
        public String id;
        public String value;
    }

    class ProfileUser {
        private static final long FORCE_MATURITY_LEVEL_UPDATE_TIME = 10800000;
        public boolean canViewTVAdultContent;
        public ProfilePreferredColor colors;
        public String id;
        public ArrayList<Settings> settings;
        private int maturityLevel;
        private int[] privileges;
        private long updateMaturityLevelTimer = -1;

        private void fetchMaturityLevel() {
            try {
                FamilySettings familySettings = ServiceManagerFactory.getInstance().getSLSServiceManager().getFamilySettings(this.id);
                if (familySettings != null && familySettings.familyUsers != null) {
                    int i = 0;
                    while (true) {
                        if (i >= familySettings.familyUsers.size()) {
                            break;
                        } else if (familySettings.familyUsers.get(i).xuid.equalsIgnoreCase(this.id)) {
                            this.canViewTVAdultContent = familySettings.familyUsers.get(i).canViewTVAdultContent;
                            this.maturityLevel = familySettings.familyUsers.get(i).maturityLevel;
                            break;
                        } else {
                            i++;
                        }
                    }
                }
            } catch (Throwable unused) {
            }
            this.updateMaturityLevelTimer = System.currentTimeMillis();
        }

        public int getMaturityLevel() {
            if (this.updateMaturityLevelTimer < 0 || System.currentTimeMillis() - this.updateMaturityLevelTimer > FORCE_MATURITY_LEVEL_UPDATE_TIME) {
                fetchMaturityLevel();
            }
            return this.maturityLevel;
        }

        public void setmaturityLevel(int i) {
            this.maturityLevel = i;
            this.updateMaturityLevelTimer = System.currentTimeMillis();
        }

        public int[] getPrivileges() {
            return this.privileges;
        }

        public void setPrivilieges(int[] iArr) {
            this.privileges = iArr;
        }

        public String getSettingValue(UserProfileSetting userProfileSetting) {
            ArrayList<Settings> arrayList = this.settings;
            if (arrayList == null) {
                return null;
            }
            Iterator<Settings> it = arrayList.iterator();
            while (it.hasNext()) {
                Settings next = it.next();
                if (next.id != null && next.id.equals(userProfileSetting.toString())) {
                    return next.value;
                }
            }
            return null;
        }
    }

    class UserProfileResult {
        public ArrayList<ProfileUser> profileUsers;

        public static UserProfileResult deserialize(String str) {
            return GsonUtil.deserializeJson(str, UserProfileResult.class);
        }
    }
}
