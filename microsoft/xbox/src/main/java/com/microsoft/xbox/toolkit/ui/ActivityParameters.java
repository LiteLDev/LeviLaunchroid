package com.microsoft.xbox.toolkit.ui;

import java.util.HashMap;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ActivityParameters extends HashMap<String, Object> {
    private static final String FF_INFO_TYPE = "InfoType";
    private static final String FORCE_RELOAD = "ForceReload";
    private static final String FROM_SCREEN = "FromScreen";
    private static final String ME_XUID = "MeXuid";
    private static final String ORIGINATING_PAGE = "OriginatingPage";
    private static final String PRIVILEGES = "Privileges";
    private static final String SELECTED_PROFILE = "SelectedProfile";

    public String getMeXuid() {
        return (String) get(ME_XUID);
    }

    public void putMeXuid(String str) {
        put(ME_XUID, str);
    }

    public String getSelectedProfile() {
        return (String) get(SELECTED_PROFILE);
    }

    public void putSelectedProfile(String str) {
        put(SELECTED_PROFILE, str);
    }

    public String getPrivileges() {
        return (String) get(PRIVILEGES);
    }

    public void putPrivileges(String str) {
        put(PRIVILEGES, str);
    }

    public void putFromScreen(ScreenLayout screenLayout) {
        put(FROM_SCREEN, screenLayout);
    }

    public boolean isForceReload() {
        if (containsKey(FORCE_RELOAD)) {
            return ((Boolean) get(FORCE_RELOAD)).booleanValue();
        }
        return false;
    }

    public void putSourcePage(String str) {
        put(ORIGINATING_PAGE, str);
    }
}
