package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.io.Serializable;

public class Content implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("gameId")
    public int gameId;
    @SerializedName("name")
    public String name;
    @SerializedName("slug")
    public String slug;
    @SerializedName("links")
    public ContentLinks links;
    @SerializedName("summary")
    public String summary;
    @SerializedName("status")
    public int status;
    @SerializedName("downloadCount")
    public long downloadCount;
    @SerializedName("isFeatured")
    public boolean isFeatured;
    @SerializedName("primaryCategoryId")
    public int primaryCategoryId;
    @SerializedName("categories")
    public List<ContentCategory> categories;
    @SerializedName("classId")
    public int classId;
    @SerializedName("authors")
    public List<ContentAuthor> authors;
    @SerializedName("logo")
    public ContentAsset logo;
    @SerializedName("screenshots")
    public List<ContentAsset> screenshots;
    @SerializedName("mainFileId")
    public int mainFileId;
    @SerializedName("latestFiles")
    public List<ContentFile> latestFiles;
    @SerializedName("dateCreated")
    public String dateCreated;
    @SerializedName("dateModified")
    public String dateModified;
    @SerializedName("dateReleased")
    public String dateReleased;
    @SerializedName("allowModDistribution")
    public boolean allowModDistribution;
    @SerializedName("gamePopularityRank")
    public int gamePopularityRank;
    @SerializedName("isAvailable")
    public boolean isAvailable;
    @SerializedName("thumbsUpCount")
    public int thumbsUpCount;
}
