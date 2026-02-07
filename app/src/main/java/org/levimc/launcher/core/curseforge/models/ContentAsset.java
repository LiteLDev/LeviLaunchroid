package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ContentAsset implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("modId")
    public int modId;
    @SerializedName("title")
    public String title;
    @SerializedName("description")
    public String description;
    @SerializedName("thumbnailUrl")
    public String thumbnailUrl;
    @SerializedName("url")
    public String url;
}
