package com.mojang.minecraftpe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class SessionInfo implements Serializable {
    private static final String NOT_YET_CONFIGURED = "Not yet configured";
    private static final String SEP = ";";
    public int appVersion;
    public String branchId;
    public String buildId;
    public String commitId;
    public transient Date crashTimestamp;
    public String flavor;
    public String gameVersionName;
    public Date recordDate;
    public String sessionId;

    public SessionInfo() {
        this.sessionId = null;
        this.buildId = null;
        this.commitId = null;
        this.branchId = null;
        this.flavor = null;
        this.gameVersionName = null;
        this.appVersion = 0;
        this.recordDate = null;
        this.crashTimestamp = null;
        this.sessionId = NOT_YET_CONFIGURED;
        this.buildId = NOT_YET_CONFIGURED;
        this.commitId = NOT_YET_CONFIGURED;
        this.branchId = NOT_YET_CONFIGURED;
        this.flavor = NOT_YET_CONFIGURED;
        this.gameVersionName = NOT_YET_CONFIGURED;
        this.recordDate = new Date();
    }

    public SessionInfo(String aSessionId, String aBuildId, String aCommitId, String aBranchId, String aFlavor, String aGameVersionName, int anAppVersion, Date aRecordDate) {
        this.sessionId = null;
        this.buildId = null;
        this.commitId = null;
        this.branchId = null;
        this.flavor = null;
        this.gameVersionName = null;
        this.appVersion = 0;
        this.recordDate = null;
        this.crashTimestamp = null;
        this.sessionId = aSessionId;
        this.buildId = aBuildId;
        this.commitId = aCommitId;
        this.branchId = aBranchId;
        this.flavor = aFlavor;
        this.gameVersionName = aGameVersionName;
        this.appVersion = anAppVersion;
        this.recordDate = aRecordDate;
    }

    @NonNull
    public static SessionInfo fromString(String s) {
        SessionInfo sessionInfo = new SessionInfo();
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("Empty SessionInfo string");
        }
        String[] split = s.split(SEP);
        if (split.length != 8) {
            throw new IllegalArgumentException("Invalid SessionInfo string '" + s + "', must be 8 parts split by ';'");
        }
        sessionInfo.sessionId = split[0];
        sessionInfo.buildId = split[1];
        sessionInfo.commitId = split[2];
        sessionInfo.branchId = split[3];
        sessionInfo.flavor = split[4];
        sessionInfo.gameVersionName = split[5];
        try {
            sessionInfo.appVersion = Integer.parseInt(split[6]);
            Date parse = getDateFormat().parse(split[7], new ParsePosition(0));
            sessionInfo.recordDate = parse;
            if (parse != null) {
                return sessionInfo;
            }
            throw new IllegalArgumentException("Failed to parse date/time in SessionInfo string '" + s + "'");
        } catch (NumberFormatException unused) {
            throw new IllegalArgumentException("Failed to convert app version '" + split[6] + "' into an integer");
        }
    }

    @NonNull
    public static SimpleDateFormat getDateFormat() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    public void setContents(Context context, String aSessionId, String aBuildId, String aCommitId, String aBranchId, String aFlavor) {
        this.sessionId = aSessionId;
        this.buildId = aBuildId;
        this.commitId = aCommitId;
        this.branchId = aBranchId;
        this.flavor = aFlavor;
        updateJavaConstants(context);
    }

    public void updateJavaConstants(@NonNull Context context) {
        this.appVersion = AppConstants.APP_VERSION;
        try {
            this.gameVersionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException unused) {
            this.gameVersionName = "Not found";
        }
    }

    @NonNull
    public String toString() {
        return sessionId + SEP + buildId + SEP + commitId + SEP + branchId + SEP + flavor + SEP + gameVersionName + SEP + appVersion + SEP + getDateFormat().format(recordDate);
    }
}
