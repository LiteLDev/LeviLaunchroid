package com.microsoft.xbox.toolkit.network;

import com.microsoft.xbox.toolkit.XLEAssert;

import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XboxLiveEnvironment {
    public static final String NEVER_LIST_CONTRACT_VERSION = "1";
    public static final String SHARE_IDENTITY_CONTRACT_VERSION = "4";
    public static final String SOCIAL_SERVICE_GENERAL_CONTRACT_VERSION = "1";
    public static final String USER_PROFILE_CONTRACT_VERSION = "3";
    public static final String USER_PROFILE_PRIVACY_SETTINGS_CONTRACT_VERSION = "4";
    private static final XboxLiveEnvironment instance = new XboxLiveEnvironment();
    private final boolean useProxy = false;
    private final Environment environment = Environment.PROD;

    public static XboxLiveEnvironment Instance() {
        return instance;
    }

    public String getFriendFinderSettingsUrl() {
        return "https://settings.xboxlive.com/settings/feature/friendfinder/settings";
    }

    public String getMutedServiceUrlFormat() {
        return "https://privacy.xboxlive.com/users/xuid(%s)/people/mute";
    }

    public String getPeopleHubFriendFinderStateUrlFormat() {
        return "https://peoplehub.xboxlive.com/users/me/friendfinder";
    }

    public String getPeopleHubRecommendationsUrlFormat() {
        return "https://peoplehub.xboxlive.com/users/me/people/recommendations";
    }

    public String getProfileSettingUrlFormat() {
        return "https://privacy.xboxlive.com/users/me/privacy/settings/%s";
    }

    public boolean getProxyEnabled() {
        return false;
    }

    public String getSetFriendFinderOptInStatusUrlFormat() {
        return "https://friendfinder.xboxlive.com/users/me/networks/%s/optin";
    }

    public String getShortCircuitProfileUrlFormat() {
        return "https://pf.directory.live.com/profile/mine/System.ShortCircuitProfile.json";
    }

    public String getSubmitFeedbackUrlFormat() {
        return "https://reputation.xboxlive.com/users/xuid(%s)/feedback";
    }

    public String getTenureWatermarkUrlFormat() {
        return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/tenure/%s.png";
    }

    public String getUpdateThirdPartyTokenUrlFormat() {
        return "https://thirdpartytokens.xboxlive.com/users/me/networks/%s/token";
    }

    public String getUploadingPhoneContactsUrlFormat() {
        return "https://people.directory.live.com/people/ExternalSCDLookup";
    }

    public String getUserProfileSettingUrlFormat() {
        return "https://privacy.xboxlive.com/users/me/privacy/settings";
    }

    public String getUserProfileInfoUrl() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://profile.dnet.xboxlive.com/users/batch/profile/settings";
            case PARTNERNET:
                return "https://profile.dnet.xboxlive.com/users/batch/profile/settings";
            case PROD:
                return "https://profile.xboxlive.com/users/batch/profile/settings";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getAddFriendsToShareIdentityUrlFormat() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://social.dnet.xboxlive.com/users/xuid(%s)/people/identityshared/xuids?method=add";
            case PROD:
                return "https://social.xboxlive.com/users/xuid(%s)/people/identityshared/xuids?method=add";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getRemoveUsersFromShareIdentityUrlFormat() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://social.dnet.xboxlive.com/users/xuid(%s)/people/identityshared/xuids?method=remove";
            case PROD:
                return "https://social.xboxlive.com/users/xuid(%s)/people/identityshared/xuids?method=remove";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getProfileNeverListUrlFormat() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://privacy.dnet.xboxlive.com/users/xuid(%s)/people/never";
            case PARTNERNET:
                return "https://privacy.dnet.xboxlive.com/users/xuid(%s)/people/never";
            case PROD:
                return "https://privacy.xboxlive.com/users/xuid(%s)/people/never";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getProfileFavoriteListUrl() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://social.dnet.xboxlive.com/users/me/people/favorites/xuids?method=%s";
            case PARTNERNET:
                return "https://social.dnet.xboxlive.com/users/me/people/favorites/xuids?method=%s";
            case PROD:
                return "https://social.xboxlive.com/users/me/people/favorites/xuids?method=%s";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String updateProfileFollowingListUrl() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://social.dnet.xboxlive.com/users/me/people/xuids?method=%s";
            case PARTNERNET:
                return "https://social.dnet.xboxlive.com/users/me/people/xuids?method=%s";
            case PROD:
                return "https://social.xboxlive.com/users/me/people/xuids?method=%s";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getProfileSummaryUrlFormat() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://social.dnet.xboxlive.com/users/xuid(%s)/summary";
            case PROD:
                return "https://social.xboxlive.com/users/xuid(%s)/summary";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public String getWatermarkUrl(@NotNull String watermark) {
        String lowerCase = watermark.toLowerCase();
        char c = 65535;
        switch (lowerCase.hashCode()) {
            case -1921480520:
                if (lowerCase.equals("nxeteam")) {
                    c = 4;
                    break;
                }
                break;
            case -69693424:
                if (lowerCase.equals("xboxoneteam")) {
                    c = 6;
                    break;
                }
                break;
            case 467871267:
                if (lowerCase.equals("kinectteam")) {
                    c = 5;
                    break;
                }
                break;
            case 547378320:
                if (lowerCase.equals("launchteam")) {
                    c = 3;
                    break;
                }
                break;
            case 742262976:
                if (lowerCase.equals("cheater")) {
                    c = 0;
                    break;
                }
                break;
            case 949652176:
                if (lowerCase.equals("xboxnxoeteam")) {
                    c = 7;
                    break;
                }
                break;
            case 1584505217:
                if (lowerCase.equals("xboxoriginalteam")) {
                    c = 1;
                    break;
                }
                break;
            case 2056113039:
                if (lowerCase.equals("xboxlivelaunchteam")) {
                    c = 2;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/cheater.png";
            case 1:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/launch/xboxoriginalteam.png";
            case 2:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/launch/xboxlivelaunchteam.png";
            case 3:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/launch/launchteam.png";
            case 4:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/launch/nxeteam.png";
            case 5:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/launch/kinectteam.png";
            case 6:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/launch/xboxoneteam.png";
            case 7:
                return "http://dlassets.xboxlive.com/public/content/ppl/watermarks/launch/xboxnxoeteam.png";
            default:
                XLEAssert.fail("Unsupported watermark value: " + watermark);
                return "";
        }
    }

    public String getGamertagSearchUrlFormat() {
        switch (environment) {
            case VINT:
            case DNET:
                return "https://profile.dnet.xboxlive.com/users/gt(%s)/profile/settings?settings=AppDisplayName,DisplayPic,Gamerscore,Gamertag,PublicGamerpic,XboxOneRep";
            case PARTNERNET:
                return "https://profile.dnet.xboxlive.com/users/gt(%s)/profile/settings?settings=AppDisplayName,DisplayPic,Gamerscore,Gamertag,PublicGamerpic,XboxOneRep";
            case PROD:
                return "https://profile.xboxlive.com/users/gt(%s)/profile/settings?settings=AppDisplayName,DisplayPic,Gamerscore,Gamertag,PublicGamerpic,XboxOneRep";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public enum Environment {
        STUB,
        VINT,
        CERTNET,
        PARTNERNET,
        PROD,
        DNET
    }

}
