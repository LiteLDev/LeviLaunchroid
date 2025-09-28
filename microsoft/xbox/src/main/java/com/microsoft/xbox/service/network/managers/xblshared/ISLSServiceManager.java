package com.microsoft.xbox.service.network.managers.xblshared;

import com.microsoft.xbox.service.model.privacy.PrivacySettings;
import com.microsoft.xbox.service.model.privacy.PrivacySettingsResult;
import com.microsoft.xbox.service.network.managers.AddFollowingUserResponseContainer;
import com.microsoft.xbox.service.network.managers.FamilySettings;
import com.microsoft.xbox.service.network.managers.IPeopleHubResult;
import com.microsoft.xbox.service.network.managers.IUserProfileResult;
import com.microsoft.xbox.service.network.managers.MutedListResultContainer;
import com.microsoft.xbox.service.network.managers.NeverListResultContainer;
import com.microsoft.xbox.service.network.managers.ProfilePreferredColor;
import com.microsoft.xbox.service.network.managers.ProfileSummaryResultContainer;
import com.microsoft.xbox.toolkit.XLEException;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public interface ISLSServiceManager {
    IUserProfileResult.UserProfileResult SearchGamertag(String str) throws XLEException;

    boolean addFriendToShareIdentitySetting(String str, String str2) throws XLEException;

    boolean addUserToFavoriteList(String str) throws XLEException;

    AddFollowingUserResponseContainer.AddFollowingUserResponse addUserToFollowingList(String str) throws XLEException;

    boolean addUserToMutedList(String str, String str2) throws XLEException;

    boolean addUserToNeverList(String str, String str2) throws XLEException;

    FamilySettings getFamilySettings(String str) throws XLEException;

    MutedListResultContainer.MutedListResult getMutedListInfo(String str) throws XLEException;

    NeverListResultContainer.NeverListResult getNeverListInfo(String str) throws XLEException;

    IPeopleHubResult.PeopleHubPeopleSummary getPeopleHubRecommendations() throws XLEException;

    PrivacySettings.PrivacySetting getPrivacySetting(PrivacySettings.PrivacySettingId privacySettingId) throws XLEException;

    ProfilePreferredColor getProfilePreferredColor(String str) throws XLEException;

    ProfileSummaryResultContainer.ProfileSummaryResult getProfileSummaryInfo(String str) throws XLEException;

    IUserProfileResult.UserProfileResult getUserProfileInfo(String str) throws XLEException;

    PrivacySettingsResult getUserProfilePrivacySettings() throws XLEException;

    int[] getXTokenPrivileges() throws XLEException;

    boolean removeFriendFromShareIdentitySetting(String str, String str2) throws XLEException;

    boolean removeUserFromFavoriteList(String str) throws XLEException;

    boolean removeUserFromFollowingList(String str) throws XLEException;

    boolean removeUserFromMutedList(String str, String str2) throws XLEException;

    boolean removeUserFromNeverList(String str, String str2) throws XLEException;

    boolean setPrivacySettings(PrivacySettingsResult privacySettingsResult) throws XLEException;

    boolean submitFeedback(String str, String str2) throws XLEException;
}
