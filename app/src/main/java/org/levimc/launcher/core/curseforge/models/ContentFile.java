package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.io.Serializable;

public class ContentFile implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("modId")
    public int modId;
    @SerializedName("displayName")
    public String displayName;
    @SerializedName("fileName")
    public String fileName;
    @SerializedName("hashes")
    public List<FileHash> hashes;
    @SerializedName("fileDate")
    public String fileDate;
    @SerializedName("downloadUrl")
    public String downloadUrl;
    @SerializedName("gameVersions")
    public List<String> gameVersions;
    @SerializedName("dependencies")
    public List<FileDependency> dependencies;
    @SerializedName("modules")
    public List<FileModule> modules;

    public static class FileHash implements Serializable {
        @SerializedName("value")
        public String value;
        @SerializedName("algo")
        public int algo;
    }

    public static class FileDependency implements Serializable {
        @SerializedName("modId")
        public int modId;
    }

    public static class FileModule implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("fingerprint")
        public long fingerprint;
    }
}
