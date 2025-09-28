package com.microsoft.xbox.service.network.managers.xblshared;

import android.util.Log;
import android.util.Pair;
import com.microsoft.xbox.idp.util.HttpCall;
import com.microsoft.xbox.idp.util.HttpUtil;
import com.microsoft.xbox.service.model.privacy.PrivacySettings;
import com.microsoft.xbox.service.model.privacy.PrivacySettingsResult;
import com.microsoft.xbox.service.network.managers.*;
import com.microsoft.xbox.toolkit.*;
import com.microsoft.xbox.toolkit.network.XboxLiveEnvironment;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class SLSXsapiServiceManager implements ISLSServiceManager {
    private static final String TAG = SLSXsapiServiceManager.class.getSimpleName();

    public FamilySettings getFamilySettings(String str) throws XLEException {
        return null;
    }

    public int[] getXTokenPrivileges() throws XLEException {
        return new int[0];
    }

    public boolean removeFriendFromShareIdentitySetting(String str, String str2) throws XLEException {
        Log.i(TAG, "removeFriendFromShareIdentitySetting");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPost.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getRemoveUsersFromShareIdentityUrlFormat(), str), ""), "4");
        appendCommonParameters.setRequestBody(str2);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, List.of(HttpStatus.SC_NO_CONTENT));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public boolean addFriendToShareIdentitySetting(String str, String str2) throws XLEException {
        Log.i(TAG, "addFriendToShareIdentitySetting");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPost.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getAddFriendsToShareIdentityUrlFormat(), str), ""), "4");
        appendCommonParameters.setRequestBody(str2);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, List.of(HttpStatus.SC_NO_CONTENT));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public boolean addUserToFavoriteList(String str) throws XLEException {
        Log.i(TAG, "addUserToFavoriteList");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPost.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getProfileFavoriteListUrl(), "add"), ""), "1");
        appendCommonParameters.setRequestBody(str);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, List.of(HttpStatus.SC_NO_CONTENT));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public boolean removeUserFromFavoriteList(String str) throws XLEException {
        Log.i(TAG, "removeUserFromFavoriteList");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPost.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getProfileFavoriteListUrl(), "remove"), ""), "1");
        appendCommonParameters.setRequestBody(str);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, List.of(HttpStatus.SC_NO_CONTENT));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public AddFollowingUserResponseContainer.AddFollowingUserResponse addUserToFollowingList(String postBody) throws XLEException {
        boolean z;
        Log.i(TAG, "addUserToFollowingList");
        z = Thread.currentThread() != ThreadManager.UIThread;
        XLEAssert.assertTrue(z);
        HttpCall httpCall = HttpUtil.appendCommonParameters(new HttpCall(HttpPost.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().updateProfileFollowingListUrl(), "add"), ""), "1");
        httpCall.setRequestBody(postBody);
        final AddFollowingUserResponseContainer.AddFollowingUserResponse result = new AddFollowingUserResponseContainer.AddFollowingUserResponse();
        final AtomicReference<Pair<Boolean, AddFollowingUserResponseContainer.AddFollowingUserResponse>> notifier = new AtomicReference<>();
        notifier.set(new Pair<>(false, null));
        httpCall.getResponseAsync((httpStatus, stream, headers) -> {
            synchronized (notifier) {
                if (httpStatus >= 200 || httpStatus <= 299) {
                    result.setAddFollowingRequestStatus(true);
                    notifier.set(new Pair<>(true, result));
                } else {
                    notifier.set(new Pair<>(true, GsonUtil.deserializeJson(stream, AddFollowingUserResponseContainer.AddFollowingUserResponse.class)));
                }
                notifier.notify();
            }
        });
        synchronized (notifier) {
            while (!notifier.get().first) {
                try {
                    notifier.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        TcuiHttpUtil.throwIfNullOrFalse(notifier.get().second);
        return notifier.get().second;
    }

    public ProfileSummaryResultContainer.ProfileSummaryResult getProfileSummaryInfo(String str) throws XLEException {
        Log.i(TAG, "getProfileSummaryInfo");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        XLEAssert.assertTrue(!JavaUtil.isNullOrEmpty(str));
        ProfileSummaryResultContainer.ProfileSummaryResult profileSummaryResult = TcuiHttpUtil.getResponseSync(HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getProfileSummaryUrlFormat(), str), ""), "2"), ProfileSummaryResultContainer.ProfileSummaryResult.class);
        TcuiHttpUtil.throwIfNullOrFalse(profileSummaryResult);
        return profileSummaryResult;
    }

    public boolean removeUserFromFollowingList(String str) throws XLEException {
        Log.i(TAG, "removeUserFromFollowingList");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPost.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().updateProfileFollowingListUrl(), "remove"), ""), "1");
        appendCommonParameters.setRequestBody(str);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, List.of(HttpStatus.SC_NO_CONTENT));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public IUserProfileResult.UserProfileResult getUserProfileInfo(String str) throws XLEException {
        Log.i(TAG, "getUserProfileInfo");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall httpCall = new HttpCall(HttpPost.METHOD_NAME, XboxLiveEnvironment.Instance().getUserProfileInfoUrl(), "");
        HttpUtil.appendCommonParameters(httpCall, XboxLiveEnvironment.USER_PROFILE_CONTRACT_VERSION);
        httpCall.setRequestBody(str);
        IUserProfileResult.UserProfileResult userProfileResult = TcuiHttpUtil.getResponseSync(httpCall, IUserProfileResult.UserProfileResult.class);
        TcuiHttpUtil.throwIfNullOrFalse(userProfileResult);
        return userProfileResult;
    }

    public ProfilePreferredColor getProfilePreferredColor(String str) throws XLEException {
        Log.i(TAG, "getProfilePreferredColor");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        ProfilePreferredColor profilePreferredColor = TcuiHttpUtil.getResponseSync(HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, str, ""), "2"), ProfilePreferredColor.class);
        TcuiHttpUtil.throwIfNullOrFalse(profilePreferredColor);
        return profilePreferredColor;
    }

    public PrivacySettingsResult getUserProfilePrivacySettings() throws XLEException {
        Log.i(TAG, "getUserProfilePrivacySettings");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        PrivacySettingsResult privacySettingsResult = TcuiHttpUtil.getResponseSync(HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, XboxLiveEnvironment.Instance().getUserProfileSettingUrlFormat(), ""), "4"), PrivacySettingsResult.class);
        TcuiHttpUtil.throwIfNullOrFalse(privacySettingsResult);
        return privacySettingsResult;
    }

    public NeverListResultContainer.NeverListResult getNeverListInfo(String str) throws XLEException {
        Log.i(TAG, "getNeverListInfo");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        XLEAssert.assertTrue(!JavaUtil.isNullOrEmpty(str));
        NeverListResultContainer.NeverListResult neverListResult = TcuiHttpUtil.getResponseSync(HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getProfileNeverListUrlFormat(), str), ""), "1"), NeverListResultContainer.NeverListResult.class);
        TcuiHttpUtil.throwIfNullOrFalse(neverListResult);
        return neverListResult;
    }

    public boolean addUserToNeverList(String str, String str2) throws XLEException {
        Log.i(TAG, "addUserToNeverList");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPut.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getProfileNeverListUrlFormat(), str), ""), "1");
        appendCommonParameters.setRequestBody(str2);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, new ArrayList<>(0));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public boolean removeUserFromNeverList(String str, String str2) throws XLEException {
        Log.i(TAG, "removeUserFromNeverList");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpDelete.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getProfileNeverListUrlFormat(), str), ""), "1");
        appendCommonParameters.setRequestBody(str2);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, new ArrayList<>(0));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public MutedListResultContainer.MutedListResult getMutedListInfo(String str) throws XLEException {
        Log.i(TAG, "getMutedListInfo");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        XLEAssert.assertTrue(!JavaUtil.isNullOrEmpty(str));
        MutedListResultContainer.MutedListResult mutedListResult = TcuiHttpUtil.getResponseSync(HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getMutedServiceUrlFormat(), str), ""), "1"), MutedListResultContainer.MutedListResult.class);
        TcuiHttpUtil.throwIfNullOrFalse(mutedListResult);
        return mutedListResult;
    }

    public boolean addUserToMutedList(String str, String str2) throws XLEException {
        Log.i(TAG, "addUserToMutedList");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPut.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getMutedServiceUrlFormat(), str), ""), "1");
        appendCommonParameters.setRequestBody(str2);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, new ArrayList<>(0));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public boolean removeUserFromMutedList(String str, String str2) throws XLEException {
        Log.i(TAG, "removeUserFromMutedList");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpDelete.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getMutedServiceUrlFormat(), str), ""), "1");
        appendCommonParameters.setRequestBody(str2);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, new ArrayList<>(0));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public boolean submitFeedback(String str, String str2) throws XLEException {
        Log.i(TAG, "submitFeedback");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPost.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getSubmitFeedbackUrlFormat(), str), ""), "101");
        appendCommonParameters.setRequestBody(str2);
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, new ArrayList<>(HttpStatus.SC_ACCEPTED));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public PrivacySettings.PrivacySetting getPrivacySetting(@NotNull PrivacySettings.PrivacySettingId privacySettingId) throws XLEException {
        Log.i(TAG, "getPrivacySetting");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        PrivacySettings.PrivacySetting privacySetting = TcuiHttpUtil.getResponseSync(HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getProfileSettingUrlFormat(), privacySettingId.name()), ""), "4"), PrivacySettings.PrivacySetting.class);
        TcuiHttpUtil.throwIfNullOrFalse(privacySetting);
        return privacySetting;
    }

    public boolean setPrivacySettings(PrivacySettingsResult privacySettingsResult) throws XLEException {
        Log.i(TAG, "setPrivacySettings");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpPut.METHOD_NAME, XboxLiveEnvironment.Instance().getUserProfileSettingUrlFormat(), ""), "4");
        appendCommonParameters.setRequestBody(PrivacySettingsResult.getPrivacySettingRequestBody(privacySettingsResult));
        boolean responseSyncSucceeded = TcuiHttpUtil.getResponseSyncSucceeded(appendCommonParameters, List.of(HttpStatus.SC_CREATED));
        TcuiHttpUtil.throwIfNullOrFalse(responseSyncSucceeded);
        return responseSyncSucceeded;
    }

    public IPeopleHubResult.PeopleHubPeopleSummary getPeopleHubRecommendations() throws XLEException {
        Log.i(TAG, "getPeopleHubRecommendations");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        HttpCall appendCommonParameters = HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, XboxLiveEnvironment.Instance().getPeopleHubRecommendationsUrlFormat(), ""), "1");
        appendCommonParameters.setCustomHeader("Accept-Language", ProjectSpecificDataProvider.getInstance().getLegalLocale());
        appendCommonParameters.setCustomHeader("X-XBL-Contract-Version", "1");
        appendCommonParameters.setCustomHeader("X-XBL-Market", ProjectSpecificDataProvider.getInstance().getRegion());
        IPeopleHubResult.PeopleHubPeopleSummary peopleHubPeopleSummary = TcuiHttpUtil.getResponseSync(appendCommonParameters, IPeopleHubResult.PeopleHubPeopleSummary.class);
        TcuiHttpUtil.throwIfNullOrFalse(peopleHubPeopleSummary);
        return peopleHubPeopleSummary;
    }

    public IUserProfileResult.UserProfileResult SearchGamertag(@NotNull String str) throws XLEException {
        Log.i(TAG, "SearchGamertag");
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        try {
            IUserProfileResult.UserProfileResult userProfileResult = TcuiHttpUtil.getResponseSync(HttpUtil.appendCommonParameters(new HttpCall(HttpGet.METHOD_NAME, String.format(XboxLiveEnvironment.Instance().getGamertagSearchUrlFormat(), URLEncoder.encode(str.toLowerCase(), "utf-8")), ""), XboxLiveEnvironment.USER_PROFILE_CONTRACT_VERSION), IUserProfileResult.UserProfileResult.class);
            TcuiHttpUtil.throwIfNullOrFalse(userProfileResult);
            return userProfileResult;
        } catch (UnsupportedEncodingException e) {
            throw new XLEException(15, e);
        }
    }
}
