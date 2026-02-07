package org.levimc.launcher.core.curseforge.models;

import com.google.gson.annotations.SerializedName;

public class Pagination {
    @SerializedName("index")
    public int index;
    @SerializedName("pageSize")
    public int pageSize;
}
