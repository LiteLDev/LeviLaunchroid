package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.io.Serializable;

public class ContentFile implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("gameId")
    public int gameId;
    @SerializedName("modId")
    public int modId;
    @SerializedName("isAvailable")
    public boolean isAvailable;
    @SerializedName("displayName")
    public String displayName;
    @SerializedName("fileName")
    public String fileName;
    @SerializedName("releaseType")
    public int releaseType;
    @SerializedName("fileStatus")
    public int fileStatus;
    @SerializedName("hashes")
    public List<FileHash> hashes;
    @SerializedName("fileDate")
    public String fileDate;
    @SerializedName("fileLength")
    public long fileLength;
    @SerializedName("downloadCount")
    public long downloadCount;
    @SerializedName("downloadUrl")
    public String downloadUrl;
    @SerializedName("gameVersions")
    public List<String> gameVersions;
    @SerializedName("sortableGameVersions")
    public List<SortableGameVersion> sortableGameVersions;
    @SerializedName("dependencies")
    public List<FileDependency> dependencies;
    @SerializedName("exposeNewAPI")
    public boolean exposeNewAPI;
    @SerializedName("parentProjectFileId")
    public int parentProjectFileId;
    @SerializedName("alternateFileId")
    public int alternateFileId;
    @SerializedName("isServerPack")
    public boolean isServerPack;
    @SerializedName("serverPackFileId")
    public int serverPackFileId;
    @SerializedName("fileFingerprint")
    public long fileFingerprint;
    @SerializedName("modules")
    public List<FileModule> modules;

    public static class FileHash implements Serializable {
        @SerializedName("value")
        public String value;
        @SerializedName("algo")
        public int algo;
    }

    public static class SortableGameVersion implements Serializable {
        @SerializedName("gameVersionName")
        public String gameVersionName;
        @SerializedName("gameVersionPadded")
        public String gameVersionPadded;
        @SerializedName("gameVersion")
        public String gameVersion;
        @SerializedName("gameVersionReleaseDate")
        public String gameVersionReleaseDate;
        @SerializedName("gameVersionTypeId")
        public int gameVersionTypeId;
    }

    public static class FileDependency implements Serializable {
        @SerializedName("modId")
        public int modId;
        @SerializedName("relationType")
        public int relationType;
    }

    public static class FileModule implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("fingerprint")
        public long fingerprint;
    }
}
