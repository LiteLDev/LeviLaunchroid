package com.microsoft.xbox.service.model;

import com.microsoft.xbox.service.network.managers.IUserProfileResult;


public class ProfileData {
    private final IUserProfileResult.UserProfileResult profileResult;
    private final boolean shareRealName;
    private final String shareRealNameStatus;
    private boolean sharingRealNameTransitively;

    public ProfileData(IUserProfileResult.UserProfileResult userProfileResult, boolean z) {
        this.profileResult = userProfileResult;
        this.shareRealName = z;
        this.shareRealNameStatus = null;
    }

    public ProfileData(IUserProfileResult.UserProfileResult userProfileResult, boolean z, String str, boolean z2) {
        this.profileResult = userProfileResult;
        this.shareRealName = z;
        this.shareRealNameStatus = str;
        this.sharingRealNameTransitively = z2;
    }

    public IUserProfileResult.UserProfileResult getProfileResult() {
        return this.profileResult;
    }

    public boolean getShareRealName() {
        return this.shareRealName;
    }

    public String getShareRealNameStatus() {
        return this.shareRealNameStatus;
    }

    public boolean getSharingRealNameTransitively() {
        return this.sharingRealNameTransitively;
    }
}
