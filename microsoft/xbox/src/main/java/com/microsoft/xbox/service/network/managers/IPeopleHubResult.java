package com.microsoft.xbox.service.network.managers;

import com.microsoft.xbox.toolkit.JavaUtil;

import java.util.ArrayList;
import java.util.Date;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public interface IPeopleHubResult {

    enum RecommendationType {
        Unknown,
        Dummy,
        Follower,
        FacebookFriend,
        PhoneContact,
        FriendOfFriend,
        VIP;

        public static RecommendationType getRecommendationType(String str) {
            for (RecommendationType recommendationType : values()) {
                if (recommendationType.name().equalsIgnoreCase(str)) {
                    return recommendationType;
                }
            }
            return Unknown;
        }
    }

    class Follower {
        public Date followedDateTime;
        public String text;
    }

    class MultiplayerSummary {
        public int InMultiplayerSession;
        public int InParty;
    }

    class PeopleHubPeopleSummary {
        public ArrayList<PeopleHubPersonSummary> people;
        public RecommendationSummary recommendationSummary;
    }

    class PeopleHubPreferredColor {
        public String primaryColor;
        public String secondaryColor;
        public String tertiaryColor;
    }

    class PeopleHubTitleHistory {
        public Date LastTimePlayed;
        public long TitleId;
        public String TitleName;
    }

    class PeopleHubTitlePresence {
        public boolean IsCurrentlyPlaying;
        public String PresenceText;
        public String TitleId;
        public String TitleName;
    }

    class PeopleHubTitleSummary {
    }

    class RecentPlayer {
        public String text;
        public ArrayList<Title> titles;
    }

    class RecommendationSummary {
        public int VIP;
        public int facebookFriend;
        public int follower;
        public int friendOfFriend;
        public int phoneContact;
        public boolean promoteSuggestions;
    }

    class Title {
        public Date lastPlayedWithDateTime;
        public String titleName;
    }

    class PeopleHubPersonSummary {
        public String displayName;
        public String displayPicRaw;
        public Follower follower;
        public String gamerScore;
        public String gamertag;
        public boolean isFavorite;
        public boolean isFollowedByCaller;
        public boolean isFollowingCaller;
        public boolean isIdentityShared;
        public MultiplayerSummary multiplayerSummary;
        public PeopleHubPreferredColor preferredColor;
        public String presenceState;
        public String presenceText;
        public String realName;
        public RecentPlayer recentPlayer;
        public PeopleHubRecommendation recommendation;
        public PeopleHubTitleHistory titleHistory;
        public PeopleHubTitlePresence titlePresence;
        public ArrayList<PeopleHubTitleSummary> titleSummaries;
        public boolean useAvatar;
        public String xboxOneRep;
        public String xuid;

        public String getRealNameFromRecommendationOrDefault() {
            String realName2 = realName;
            if (!JavaUtil.isNullOrEmpty(realName2) || recommendation == null || recommendation.Reasons == null || recommendation.Reasons.size() <= 0) {
                return realName2;
            }
            return recommendation.Reasons.get(0);
        }
    }

    class PeopleHubRecommendation {
        public ArrayList<String> Reasons;
        public String Type;

        public RecommendationType getRecommendationType() {
            return RecommendationType.getRecommendationType(this.Type);
        }
    }
}
