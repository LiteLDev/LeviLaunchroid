package org.levimc.launcher.core.versions;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class GameVersion implements Parcelable {
    public String directoryName;
    public String versionCode;
    public String displayName;
    public File versionDir;
    public boolean isInstalled;
    public String packageName;
    public boolean needsRepair;
    public boolean onlyVersionTxt;
    public boolean onlyAbiList;
    public boolean isExtractFalse;
    public String abiList;
    public boolean versionIsolation;
    public final File modsDir;

    public GameVersion(String directoryName, String displayName, String versionCode, File versionDir, boolean isOfficial, String packageName, String abiList) {
        this.directoryName = directoryName;
        this.displayName = displayName;
        this.versionCode = versionCode;
        this.versionDir = versionDir;
        this.isInstalled = isOfficial;
        this.packageName = packageName;
        this.needsRepair = false;
        this.onlyVersionTxt = false;
        this.onlyAbiList = false;
        this.isExtractFalse = false;
        this.abiList = abiList;
        this.versionIsolation = false;
        this.modsDir = versionDir == null ? null : new File(versionDir, "mods");
    }

    protected GameVersion(Parcel in) {
        directoryName = in.readString();
        displayName = in.readString();
        versionCode = in.readString();
        String versionDirPath = in.readString();
        versionDir = versionDirPath == null ? null : new File(versionDirPath);
        isInstalled = in.readByte() != 0;
        packageName = in.readString();
        needsRepair = in.readByte() != 0;
        onlyVersionTxt = in.readByte() != 0;
        onlyAbiList = in.readByte() != 0;
        isExtractFalse = in.readByte()!= 0;
        abiList = in.readString();
        versionIsolation = in.readByte() != 0;
        String modsDirPath = in.readString();
        modsDir = modsDirPath == null ? null : new File(modsDirPath);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(directoryName);
        dest.writeString(displayName);
        dest.writeString(versionCode);
        dest.writeString(versionDir == null ? null : versionDir.getAbsolutePath());
        dest.writeByte((byte) (isInstalled ? 1 : 0));
        dest.writeString(packageName);
        dest.writeByte((byte) (needsRepair ? 1 : 0));
        dest.writeByte((byte) (onlyVersionTxt ? 1 : 0));
        dest.writeByte((byte) (onlyAbiList ? 1 : 0));
        dest.writeByte((byte) (isExtractFalse? 1 : 0));
        dest.writeString(abiList);
        dest.writeByte((byte) (versionIsolation ? 1 : 0));
        dest.writeString(modsDir == null ? null : modsDir.getAbsolutePath());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GameVersion> CREATOR = new Creator<>() {
        @Override
        public GameVersion createFromParcel(Parcel in) {
            return new GameVersion(in);
        }

        @Override
        public GameVersion[] newArray(int size) {
            return new GameVersion[size];
        }
    };
}