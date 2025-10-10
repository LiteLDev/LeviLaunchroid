package com.microsoft.xbox.xle.app;

import android.content.res.Configuration;
import android.util.Base64;
import android.util.DisplayMetrics;

import com.microsoft.xbox.service.model.ProfileModel;
import com.microsoft.xbox.toolkit.GsonUtil;
import com.microsoft.xbox.toolkit.IProjectSpecificDataProvider;
import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.network.XboxLiveEnvironment;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;


public class XleProjectSpecificDataProvider implements IProjectSpecificDataProvider {
    private static final String[][] displayLocales = {new String[]{"zh_SG", "zh", "CN"}, new String[]{"zh_CN", "zh", "CN"}, new String[]{"zh_HK", "zh", "TW"}, new String[]{"zh_TW", "zh", "TW"}, new String[]{"da", "da", "DK"}, new String[]{"nl", "nl", "NL"}, new String[]{"en", "en", "GB"}, new String[]{"en_US", "en", "US"}, new String[]{"fi", "fi", "FI"}, new String[]{"fr", "fr", "FR"}, new String[]{"de", "de", "DE"}, new String[]{"it", "it", "IT"}, new String[]{"ja", "ja", "JP"}, new String[]{"ko", "ko", "KR"}, new String[]{"nb", "nb", "NO"}, new String[]{"pl", "pl", "PL"}, new String[]{"pt_PT", "pt", "PT"}, new String[]{"pt", "pt", "BR"}, new String[]{"ru", "ru", "RU"}, new String[]{"es_ES", "es", "ES"}, new String[]{"es", "es", "MX"}, new String[]{"sv", "sv", "SE"}, new String[]{"tr", "tr", "TR"}};
    private static final XleProjectSpecificDataProvider instance = new XleProjectSpecificDataProvider();
    private final Set<String> blockFeaturedChild = new HashSet();
    private final Set<String> musicBlocked = new HashSet();
    private final Set<String> promotionalRestrictedRegions = new HashSet();
    private final Set<String> purchaseBlocked = new HashSet();
    private final Hashtable<String, String> serviceLocaleMapTable = new Hashtable<>();
    private final Set<String> videoBlocked = new HashSet();
    private String androidId;
    private boolean gotSettings;
    private boolean isMeAdult;
    private String meXuid;
    private String privileges;
    private String scdRpsTicket;
    private String[][] serviceLocales = {new String[]{"es_AR", "es-AR"}, new String[]{"AR", "es-AR"}, new String[]{"en_AU", "en-AU"}, new String[]{"AU", "en-AU"}, new String[]{"de_AT", "de-AT"}, new String[]{"AT", "de-AT"}, new String[]{"fr_BE", "fr-BE"}, new String[]{"nl_BE", "nl-BE"}, new String[]{"BE", "fr-BE"}, new String[]{"pt_BR", "pt-BR"}, new String[]{"BR", "pt-BR"}, new String[]{"en_CA", "en-CA"}, new String[]{"fr_CA", "fr-CA"}, new String[]{"CA", "en-CA"}, new String[]{"en_CZ", "en-CZ"}, new String[]{"CZ", "en-CZ"}, new String[]{"da_DK", "da-DK"}, new String[]{"DK", "da-DK"}, new String[]{"fi_FI", "fi-FI"}, new String[]{"FI", "fi-FI"}, new String[]{"fr_FR", "fr-FR"}, new String[]{"FR", "fr-FR"}, new String[]{"de_DE", "de-DE"}, new String[]{"DE", "de-DE"}, new String[]{"en_GR", "en-GR"}, new String[]{"GR", "en-GR"}, new String[]{"en_HK", "en-HK"}, new String[]{"zh_HK", "zh-HK"}, new String[]{"HK", "en-HK"}, new String[]{"en_HU", "en-HU"}, new String[]{"HU", "en-HU"}, new String[]{"en_IN", "en-IN"}, new String[]{"IN", "en-IN"}, new String[]{"en_GB", "en-GB"}, new String[]{"GB", "en-GB"}, new String[]{"en_IL", "en-IL"}, new String[]{"IL", "en-IL"}, new String[]{"it_IT", "it-IT"}, new String[]{"IT", "it-IT"}, new String[]{"ja_JP", "ja-JP"}, new String[]{"JP", "ja-JP"}, new String[]{"zh_CN", "zh-CN"}, new String[]{"CN", "zh-CN"}, new String[]{"es_MX", "es-MX"}, new String[]{"MX", "es-MX"}, new String[]{"es_CL", "es-CL"}, new String[]{"CL", "es-CL"}, new String[]{"es_CO", "es-CO"}, new String[]{"CO", "es-CO"}, new String[]{"nl_NL", "nl-NL"}, new String[]{"NL", "nl-NL"}, new String[]{"en_NZ", "en-NZ"}, new String[]{"NZ", "en-NZ"}, new String[]{"nb_NO", "nb-NO"}, new String[]{"NO", "nb-NO"}, new String[]{"pl_PL", "pl-PL"}, new String[]{"PL", "pl-PL"}, new String[]{"pt_PT", "pt-PT"}, new String[]{"PT", "pt-PT"}, new String[]{"ru_RU", "ru-RU"}, new String[]{"RU", "ru-RU"}, new String[]{"en_SA", "en-SA"}, new String[]{"SA", "en-SA"}, new String[]{"en_SG", "en-SG"}, new String[]{"zh_SG", "zh-SG"}, new String[]{"SG", "en-SG"}, new String[]{"en_SK", "en-SK"}, new String[]{"SK", "en-SK"}, new String[]{"en_ZA", "en-ZA"}, new String[]{"ZA", "en-ZA"}, new String[]{"ko_KR", "ko-KR"}, new String[]{"KR", "ko-KR"}, new String[]{"es_ES", "es-ES"}, new String[]{"es", "es-ES"}, new String[]{"de_CH", "de-CH"}, new String[]{"fr_CH", "fr-CH"}, new String[]{"CH", "fr-CH"}, new String[]{"zh_TW", "zh-TW"}, new String[]{"TW", "zh-TW"}, new String[]{"en_AE", "en-AE"}, new String[]{"AE", "en-AE"}, new String[]{"en_US", "en-US"}, new String[]{"US", "en-US"}, new String[]{"sv_SE", "sv-SE"}, new String[]{"SE", "sv-SE"}, new String[]{"tr_Tr", "tr-TR"}, new String[]{"Tr", "tr-TR"}, new String[]{"en_IE", "en-IE"}, new String[]{"IE", "en-IE"}};

    private XleProjectSpecificDataProvider() {
        int i = 0;
        while (true) {
            String[][] strArr = this.serviceLocales;
            if (i < strArr.length) {
                this.serviceLocaleMapTable.put(strArr[i][0], strArr[i][1]);
                i++;
            } else {
                this.serviceLocales = null;
                return;
            }
        }
    }

    public static XleProjectSpecificDataProvider getInstance() {
        return instance;
    }

    public boolean getAllowExplicitContent() {
        return true;
    }

    public String getAutoSuggestdDataSource() {
        return "bbxall2";
    }

    public String getCombinedContentRating() {
        return "";
    }

    public String getCurrentSandboxID() {
        return "PROD";
    }

    public boolean getIsForXboxOne() {
        return true;
    }

    public boolean getIsFreeAccount() {
        return false;
    }

    public boolean getIsXboxMusicSupported() {
        return true;
    }

    public int getVersionCode() {
        return 1;
    }

    public boolean isMusicBlocked() {
        return true;
    }

    public boolean isVideoBlocked() {
        return true;
    }

    public void ensureDisplayLocale() {
        Locale locale;
        Locale locale2 = Locale.getDefault();
        String locale3 = locale2.toString();
        String language = locale2.getLanguage();
        String country = locale2.getCountry();
        int i = 0;
        while (true) {
            String[][] strArr = displayLocales;
            if (i >= strArr.length) {
                locale = null;
                break;
            } else if (!strArr[i][0].equals(locale3)) {
                i++;
            } else if (!displayLocales[i][1].equals(language) || !displayLocales[i][2].equals(country)) {
                String[][] strArr2 = displayLocales;
                locale = new Locale(strArr2[i][1], strArr2[i][2]);
            } else {
                return;
            }
        }
        if (locale == null) {
            int i2 = 0;
            while (true) {
                String[][] strArr3 = displayLocales;
                if (i2 >= strArr3.length) {
                    break;
                } else if (strArr3[i2][0].equals(language)) {
                    String[][] strArr4 = displayLocales;
                    locale = new Locale(strArr4[i2][1], strArr4[i2][2]);
                    break;
                } else {
                    i2++;
                }
            }
        }
        if (locale != null) {
            DisplayMetrics displayMetrics = XboxTcuiSdk.getResources().getDisplayMetrics();
            Configuration configuration = XboxTcuiSdk.getResources().getConfiguration();
            configuration.locale = locale;
            XboxTcuiSdk.getResources().updateConfiguration(configuration, displayMetrics);
        }
    }

    private void addRegions(String str, Set<String> set) {
        if (!JavaUtil.isNullOrEmpty(str)) {
            String[] split = str.split("[|]");
            if (!XLEUtil.isNullOrEmpty(split)) {
                set.clear();
                for (String str2 : split) {
                    if (!JavaUtil.isNullOrEmpty(str2)) {
                        set.add(str2);
                    }
                }
            }
        }
    }

    public void processContentBlockedList(@NotNull SmartglassSettings smartglassSettings) {
        addRegions(smartglassSettings.VIDEO_BLOCKED, this.videoBlocked);
        addRegions(smartglassSettings.MUSIC_BLOCKED, this.musicBlocked);
        addRegions(smartglassSettings.PURCHASE_BLOCKED, this.purchaseBlocked);
        addRegions(smartglassSettings.BLOCK_FEATURED_CHILD, this.blockFeaturedChild);
        addRegions(smartglassSettings.PROMOTIONAL_CONTENT_RESTRICTED_REGIONS, this.promotionalRestrictedRegions);
        this.gotSettings = true;
    }

    public boolean gotSettings() {
        return this.gotSettings;
    }

    public void setIsMeAdult(boolean z) {
        this.isMeAdult = z;
    }

    public boolean isMeAdult() {
        return this.isMeAdult;
    }

    public int getMeMaturityLevel() {
        ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
        if (meProfileModel != null) {
            return meProfileModel.getMaturityLevel();
        }
        return 0;
    }

    public String getRegion() {
        return Locale.getDefault().getCountry();
    }

    public boolean isPurchaseBlocked() {
        return this.purchaseBlocked.contains(getRegion());
    }

    public boolean isFeaturedBlocked() {
        return !isMeAdult() && this.blockFeaturedChild.contains(getRegion());
    }

    public boolean isPromotionalRestricted() {
        return !isMeAdult() && this.promotionalRestrictedRegions.contains(getRegion());
    }

    public String getXuidString() {
        return this.meXuid;
    }

    public void setXuidString(String str) {
        this.meXuid = str;
    }

    public String getPrivileges() {
        return this.privileges;
    }

    public void setPrivileges(String str) {
        this.privileges = str;
    }

    public String getSCDRpsTicket() {
        return this.scdRpsTicket;
    }

    public void setSCDRpsTicket(String str) {
        this.scdRpsTicket = str;
    }

    public String getLegalLocale() {
        return getConnectedLocale();
    }

    public String getMembershipLevel() {
        if (ProfileModel.getMeProfileModel().getAccountTier() == null) {
            return "Gold";
        }
        return ProfileModel.getMeProfileModel().getAccountTier();
    }

    public boolean getInitializeComplete() {
        return getXuidString() != null;
    }

    public String getWindowsLiveClientId() {
        switch (XboxLiveEnvironment.Instance().getEnvironment()) {
            case PROD:
                return "0000000048093EE3";
            case VINT:
                return "0000000068036303";
            case DNET:
                return "0000000068036303";
            case PARTNERNET:
                return "0000000068036303";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getVersionCheckUrl() {
        switch (XboxLiveEnvironment.Instance().getEnvironment()) {
            case PROD:
                return "http://www.xbox.com/en-US/Platform/Android/XboxLIVE/sgversion";
            case PARTNERNET:
                return "http://www.rtm.vint.xbox.com/en-US/Platform/Android/XboxLIVE/sgversion";
            case VINT:
                return "http://www.rtm.vint.xbox.com/en-US/Platform/Android/XboxLIVE/sgversion";
            case DNET:
                return "http://www.xbox.com/en-US/Platform/Android/XboxLIVE/sgversion";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void resetModels(boolean z) {
        ProfileModel.reset();
    }

    private String getDeviceLocale() {
        Locale locale = Locale.getDefault();
        String locale2 = locale.toString();
        if (this.serviceLocaleMapTable.containsKey(locale2)) {
            return this.serviceLocaleMapTable.get(locale2);
        }
        String country = locale.getCountry();
        return (JavaUtil.isNullOrEmpty(country) || !this.serviceLocaleMapTable.containsKey(country)) ? "en-US" : this.serviceLocaleMapTable.get(country);
    }

    public boolean isDeviceLocaleKnown() {
        Locale locale = Locale.getDefault();
        if (this.serviceLocaleMapTable.containsKey(locale.toString())) {
            return true;
        }
        String country = locale.getCountry();
        return !JavaUtil.isNullOrEmpty(country) && this.serviceLocaleMapTable.containsKey(country);
    }

    public String getConnectedLocale() {
        return getDeviceLocale();
    }

    public String getConnectedLocale(boolean z) {
        return getConnectedLocale();
    }

    public String getContentRestrictions() {
        String region = getRegion();
        int meMaturityLevel = getMeMaturityLevel();
        if (JavaUtil.isNullOrEmpty(region) || meMaturityLevel == 255) {
            return null;
        }
        String jsonString = GsonUtil.toJsonString(new ContentRestrictions(region, meMaturityLevel, isPromotionalRestricted()));
        if (!JavaUtil.isNullOrEmpty(jsonString)) {
            return Base64.encodeToString(jsonString.getBytes(), 2);
        }
        return null;
    }

    private class ContentRestrictions {
        public Data data;
        public int version = 2;

        public ContentRestrictions(String str, int i, boolean z) {
            Data data2 = new Data();
            this.data = data2;
            data2.geographicRegion = str;
            Data data3 = this.data;
            data3.preferredAgeRating = i;
            data3.maxAgeRating = i;
            this.data.restrictPromotionalContent = z;
        }

        public class Data {
            public String geographicRegion;
            public int maxAgeRating;
            public int preferredAgeRating;
            public boolean restrictPromotionalContent;

            public Data() {
            }
        }
    }
}
