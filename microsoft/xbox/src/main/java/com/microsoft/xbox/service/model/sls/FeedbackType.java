package com.microsoft.xbox.service.model.sls;

import com.microsoft.xboxtcui.R;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public enum FeedbackType {
    Unknown,
    FairPlayKillsTeammates,
    FairPlayCheater,
    FairPlayTampering,
    FairPlayQuitter,
    FairPlayKicked,
    FairPlayBlock,
    FairPlayUnblock,
    FairPlayUserBanRequest,
    FairPlayConsoleBanRequest,
    FairplayUnsporting,
    FairplayIdler,
    CommsTextMessage,
    CommsVoiceMessage,
    CommsPictureMessage,
    CommsInappropriateVideo,
    CommsAbusiveVoice,
    CommsSpam,
    CommsPhishing,
    CommsMuted,
    CommsUnmuted,
    Comms911,
    UserContentActivityFeed,
    UserContentGameDVR,
    UserContentGamertag,
    UserContentRealName,
    UserContentGamerpic,
    UserContentPersonalInfo,
    UserContentInappropriateUGC,
    UserContentReviewRequest,
    UserContentScreenshot,
    PositiveSkilledPlayer,
    PositiveHelpfulPlayer,
    PositiveHighQualityUGC,
    InternalReputationUpdated,
    InternalAmbassadorScoreUpdated,
    InternalReputationReset,
    InternalEnforcementDataUpdated;


    @NotNull
    public String getTitle() {
        switch (this) {
            case UserContentPersonalInfo:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_BioLoc);
            case FairPlayCheater:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_Cheating);
            case UserContentRealName:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_PlayerName);
            case UserContentGamertag:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_PlayerName);
            case UserContentGamerpic:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_PlayerPic);
            case FairPlayQuitter:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_QuitEarly);
            case FairplayUnsporting:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_Unsporting);
            case CommsAbusiveVoice:
                return XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_VoiceComm);
            default:
                XLEAssert.fail("No title implementation.");
                return "";
        }
    }
}
