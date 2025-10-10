package com.microsoft.xbox.telemetry.helpers;

import com.microsoft.xbox.telemetry.utc.model.UTCNames;
import com.microsoft.xbox.toolkit.XLEAssert;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;


public class UTCChangeRelationship {
    public static CharSequence currentActivityTitle = "";
    public static String currentXUID = "";

    private static void verifyTrackedDefaults() {
        XLEAssert.assertFalse("Called trackPeopleHubView without set currentXUID", currentXUID.equals(""));
        XLEAssert.assertFalse("Called trackPeopleHubView without set activityTitle", currentActivityTitle.toString().equals(""));
    }

    public static @NotNull HashMap<String, Object> getAdditionalInfo(String str) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(UTCDeepLink.TARGET_XUID_KEY, "x:" + str);
        return hashMap;
    }

    public static void trackChangeRelationshipView(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> {
            CharSequence unused = UTCChangeRelationship.currentActivityTitle = charSequence;
            String unused2 = UTCChangeRelationship.currentXUID = str;
            UTCPageView.track(UTCNames.PageView.ChangeRelationship.ChangeRelationshipView, UTCChangeRelationship.currentActivityTitle, UTCChangeRelationship.getAdditionalInfo(UTCChangeRelationship.currentXUID));
        });
    }

    public static void trackChangeRelationshipAction(boolean z, boolean z2) {
        verifyTrackedDefaults();
        trackChangeRelationshipAction(currentActivityTitle, currentXUID, z, z2);
    }

    public static void trackChangeRelationshipAction(final CharSequence charSequence, final String str, final boolean z, final boolean z2) {
        UTCEventTracker.callTrackWrapper(() -> {
            HashMap access$200 = UTCChangeRelationship.getAdditionalInfo(str);
            access$200.put("relationship", Integer.valueOf((z ? Relationship.EXISTINGFRIEND : Relationship.ADDFRIEND).getValue()));
            UTCPageAction.track(UTCNames.PageAction.ChangeRelationship.Action, charSequence, access$200);
            if (z2) {
                UTCChangeRelationship.trackChangeRelationshipDone(charSequence, str, Relationship.ADDFRIEND, RealNameStatus.SHARINGON, FavoriteStatus.NOTFAVORITED, GamerType.FACEBOOK);
            }
        });
    }

    public static void trackChangeRelationshipRemoveFriend() {
        verifyTrackedDefaults();
        trackChangeRelationshipRemoveFriend(currentActivityTitle, currentXUID);
    }

    public static void trackChangeRelationshipRemoveFriend(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> {
            HashMap access$200 = UTCChangeRelationship.getAdditionalInfo(str);
            access$200.put("relationship", Relationship.REMOVEFRIEND);
            UTCPageAction.track(UTCNames.PageAction.ChangeRelationship.Action, charSequence, access$200);
        });
    }

    public static void trackChangeRelationshipDone(Relationship relationship, RealNameStatus realNameStatus, FavoriteStatus favoriteStatus, GamerType gamerType) {
        verifyTrackedDefaults();
        trackChangeRelationshipDone(currentActivityTitle, currentXUID, relationship, realNameStatus, favoriteStatus, gamerType);
    }

    public static void trackChangeRelationshipDone(CharSequence charSequence, String str, Relationship relationship, RealNameStatus realNameStatus, FavoriteStatus favoriteStatus, GamerType gamerType) {
        final String str2 = str;
        final Relationship relationship2 = relationship;
        final FavoriteStatus favoriteStatus2 = favoriteStatus;
        final RealNameStatus realNameStatus2 = realNameStatus;
        final GamerType gamerType2 = gamerType;
        final CharSequence charSequence2 = charSequence;
        UTCEventTracker.callTrackWrapper(() -> {
            HashMap access$200 = UTCChangeRelationship.getAdditionalInfo(str2);
            access$200.put("relationship", Integer.valueOf(relationship2.getValue()));
            access$200.put("favorite", Integer.valueOf(favoriteStatus2.getValue()));
            access$200.put("realname", Integer.valueOf(realNameStatus2.getValue()));
            access$200.put("gamertype", Integer.valueOf(gamerType2.getValue()));
            UTCPageAction.track(UTCNames.PageAction.ChangeRelationship.Done, charSequence2, access$200);
        });
    }

    public enum Relationship {
        UNKNOWN(0),
        ADDFRIEND(1),
        REMOVEFRIEND(2),
        EXISTINGFRIEND(3),
        NOTCHANGED(4);

        private final int value;

        Relationship(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum FavoriteStatus {
        UNKNOWN(0),
        FAVORITED(1),
        UNFAVORITED(2),
        NOTFAVORITED(3),
        EXISTINGFAVORITE(4),
        EXISTINGNOTFAVORITED(5);

        private final int value;

        FavoriteStatus(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum RealNameStatus {
        UNKNOWN(0),
        SHARINGON(1),
        SHARINGOFF(2),
        EXISTINGSHARED(3),
        EXISTINGNOTSHARED(4);

        private final int value;

        RealNameStatus(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum GamerType {
        UNKNOWN(0),
        NORMAL(1),
        FACEBOOK(2),
        SUGGESTED(3);

        private final int value;

        GamerType(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }
    }
}
