package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ContentLinks implements Serializable {
    @SerializedName("websiteUrl")
    public String websiteUrl;
    @SerializedName("wikiUrl")
    public String wikiUrl;
    @SerializedName("issuesUrl")
    public String issuesUrl;
    @SerializedName("sourceUrl")
    public String sourceUrl;
}
