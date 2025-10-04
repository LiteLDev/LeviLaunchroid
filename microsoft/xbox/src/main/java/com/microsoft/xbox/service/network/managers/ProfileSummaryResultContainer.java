package com.microsoft.xbox.service.network.managers;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ProfileSummaryResultContainer {

    public static class ProfileSummaryResult {
        public boolean hasCallerMarkedTargetAsFavorite;
        public boolean hasCallerMarkedTargetAsIdentityShared;
        public boolean hasCallerMarkedTargetAsKnown;
        public boolean isCallerFollowingTarget;
        public boolean isTargetFollowingCaller;
        public String legacyFriendStatus;
        public int recentChangeCount;
        public int targetFollowerCount;
        public int targetFollowingCount;
    }
}
