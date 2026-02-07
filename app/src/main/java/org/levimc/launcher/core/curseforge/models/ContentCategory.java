package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ContentCategory implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("gameId")
    public int gameId;
    @SerializedName("name")
    public String name;
    @SerializedName("slug")
    public String slug;
    @SerializedName("url")
    public String url;
    @SerializedName("iconUrl")
    public String iconUrl;
    @SerializedName("dateModified")
    public String dateModified;
    @SerializedName("isClass")
    public boolean isClass;
    @SerializedName("classId")
    public int classId;
    @SerializedName("parentCategoryId")
    public int parentCategoryId;
}
