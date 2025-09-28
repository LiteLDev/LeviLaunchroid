package com.microsoft.xbox.service.model;

import com.microsoft.xbox.service.network.managers.IPeopleHubResult;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UserProfileData {
    public String TenureLevel;
    public String accountTier;
    public String appDisplayName;
    public String gamerRealName;
    public String gamerScore;
    public String gamerTag;
    public String profileImageUrl;
    public String xuid;

    public UserProfileData() {
    }

    public UserProfileData(@NotNull IPeopleHubResult.PeopleHubPersonSummary peopleHubPersonSummary) {
        this.xuid = peopleHubPersonSummary.xuid;
        this.profileImageUrl = peopleHubPersonSummary.displayPicRaw;
        this.gamerTag = peopleHubPersonSummary.gamertag;
        this.appDisplayName = peopleHubPersonSummary.displayName;
        this.gamerRealName = peopleHubPersonSummary.realName;
        this.gamerScore = peopleHubPersonSummary.gamerScore;
        this.accountTier = peopleHubPersonSummary.xboxOneRep;
    }
}
