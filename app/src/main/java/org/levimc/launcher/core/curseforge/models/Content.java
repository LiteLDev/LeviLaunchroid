package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.io.Serializable;

public class Content implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("name")
    public String name;
    @SerializedName("links")
    public ContentLinks links;
    @SerializedName("summary")
    public String summary;
    @SerializedName("status")
    public int status;
    @SerializedName("downloadCount")
    public long downloadCount;
    @SerializedName("categories")
    public List<ContentCategory> categories;
    @SerializedName("authors")
    public List<ContentAuthor> authors;
    @SerializedName("logo")
    public ContentAsset logo;
    @SerializedName("latestFiles")
    public List<ContentFile> latestFiles;
    @SerializedName("dateModified")
    public String dateModified;
}
