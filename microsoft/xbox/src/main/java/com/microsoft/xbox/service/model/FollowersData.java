package com.microsoft.xbox.service.model;

import com.microsoft.xbox.service.network.managers.IPeopleHubResult;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.xle.app.XLEUtil;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class FollowersData implements Serializable {
    private static final long serialVersionUID = 6714889261254600161L;
    public boolean isCurrentlyPlaying;
    public boolean isFavorite;
    public transient boolean isNew;
    public String presenceString;
    public UserStatus status;
    public long titleId;
    public UserProfileData userProfileData;
    public String xuid;
    protected boolean isDummy;
    protected DummyType itemDummyType;
    private String followerText;
    private Date lastPlayedWithDateTime;
    private IPeopleHubResult.PeopleHubPersonSummary personSummary;
    private String recentPlayerText;
    private SearchResultPerson searchResultPerson;
    private Date timeStamp;

    public FollowersData() {
        this.personSummary = null;
        this.isCurrentlyPlaying = false;
        this.isDummy = false;
        this.isNew = false;
    }

    public FollowersData(boolean z) {
        this(z, DummyType.NOT_SET);
    }

    public FollowersData(boolean z, DummyType dummyType) {
        this.personSummary = null;
        this.isCurrentlyPlaying = false;
        this.isDummy = false;
        this.isNew = false;
        this.isDummy = z;
        this.itemDummyType = dummyType;
    }

    public FollowersData(IPeopleHubResult.PeopleHubPersonSummary peopleHubPersonSummary) {
        this.personSummary = null;
        this.isCurrentlyPlaying = false;
        this.isDummy = false;
        this.isNew = false;
        XLEAssert.assertNotNull(peopleHubPersonSummary);
        this.personSummary = peopleHubPersonSummary;
        this.xuid = peopleHubPersonSummary.xuid;
        this.userProfileData = new UserProfileData(peopleHubPersonSummary);
        this.isFavorite = peopleHubPersonSummary.isFavorite;
        this.status = UserStatus.getStatusFromString(peopleHubPersonSummary.presenceState);
        this.presenceString = peopleHubPersonSummary.presenceText;
        if (peopleHubPersonSummary.titleHistory != null) {
            this.titleId = peopleHubPersonSummary.titleHistory.TitleId;
            this.timeStamp = peopleHubPersonSummary.titleHistory.LastTimePlayed;
        }
        if (peopleHubPersonSummary.recentPlayer != null) {
            this.recentPlayerText = peopleHubPersonSummary.recentPlayer.text;
            if (!XLEUtil.isNullOrEmpty(peopleHubPersonSummary.recentPlayer.titles)) {
                this.lastPlayedWithDateTime = peopleHubPersonSummary.recentPlayer.titles.get(0).lastPlayedWithDateTime;
            }
        }
        if (peopleHubPersonSummary.follower != null) {
            this.followerText = peopleHubPersonSummary.follower.text;
        }
        if (peopleHubPersonSummary.titlePresence != null) {
            this.isCurrentlyPlaying = peopleHubPersonSummary.titlePresence.IsCurrentlyPlaying;
            this.presenceString = peopleHubPersonSummary.titlePresence.PresenceText;
        }
    }

    public FollowersData(@NotNull FollowersData followersData) {
        this.personSummary = null;
        this.isCurrentlyPlaying = false;
        this.isDummy = false;
        this.isNew = false;
        this.xuid = followersData.xuid;
        this.isFavorite = followersData.isFavorite;
        this.status = followersData.status;
        this.presenceString = followersData.presenceString;
        this.titleId = followersData.titleId;
        this.userProfileData = followersData.userProfileData;
        this.isCurrentlyPlaying = followersData.isCurrentlyPlaying;
        this.timeStamp = followersData.timeStamp;
        this.isDummy = followersData.isDummy;
    }

    public DummyType getItemDummyType() {
        return this.itemDummyType;
    }

    public void setItemDummyType(DummyType dummyType) {
        this.isDummy = true;
        this.itemDummyType = dummyType;
    }

    public IPeopleHubResult.PeopleHubPersonSummary getPersonSummary() {
        return this.personSummary;
    }

    public int getGameScore() {
        UserProfileData userProfileData2 = this.userProfileData;
        if (userProfileData2 != null) {
            return Integer.parseInt(userProfileData2.gamerScore);
        }
        return 0;
    }

    public String getGamertag() {
        UserProfileData userProfileData2 = this.userProfileData;
        return userProfileData2 != null ? userProfileData2.gamerTag : "";
    }

    public String getGamerPicUrl() {
        UserProfileData userProfileData2 = this.userProfileData;
        if (userProfileData2 != null) {
            return userProfileData2.profileImageUrl;
        }
        return null;
    }

    public String getGamerName() {
        UserProfileData userProfileData2 = this.userProfileData;
        return userProfileData2 != null ? userProfileData2.appDisplayName : "";
    }

    public String getGamerRealName() {
        UserProfileData userProfileData2 = this.userProfileData;
        if (userProfileData2 == null) {
            return null;
        }
        return userProfileData2.gamerRealName;
    }

    public boolean getIsOnline() {
        return this.status == UserStatus.Online;
    }

    public Date getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(Date date) {
        this.timeStamp = date;
    }

    public boolean getIsDummy() {
        return this.isDummy;
    }

    public Date getLastPlayedWithDateTime() {
        return this.lastPlayedWithDateTime;
    }

    public String getRecentPlayerTitleText() {
        return this.recentPlayerText;
    }

    public String getFollowersTitleText() {
        return this.followerText;
    }

    public SearchResultPerson getSearchResultPerson() {
        return this.searchResultPerson;
    }

    public void setSearchResultPerson(SearchResultPerson searchResultPerson2) {
        this.searchResultPerson = searchResultPerson2;
    }

    public enum DummyType {
        NOT_SET,
        DUMMY_HEADER,
        DUMMY_FRIENDS_HEADER,
        DUMMY_LINK_TO_FACEBOOK,
        DUMMY_FRIENDS_WHO_PLAY,
        DUMMY_VIPS,
        DUMMY_ERROR,
        DUMMY_NO_DATA,
        DUMMY_LOADING
    }
}
