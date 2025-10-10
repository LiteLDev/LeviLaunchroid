package com.microsoft.xbox.service.model;

import com.microsoft.xbox.toolkit.JavaUtil;


public enum UserStatus {
    Offline,
    Online;

    public static UserStatus getStatusFromString(String str) {
        if (JavaUtil.stringsEqualCaseInsensitive(str, Online.toString())) {
            return Online;
        }
        return Offline;
    }
}
