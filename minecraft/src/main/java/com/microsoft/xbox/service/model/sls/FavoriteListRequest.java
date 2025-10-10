package com.microsoft.xbox.service.model.sls;

import com.microsoft.xbox.toolkit.GsonUtil;

import java.util.ArrayList;


public class FavoriteListRequest {
    public ArrayList<String> xuids;

    public FavoriteListRequest(ArrayList<String> arrayList) {
        this.xuids = arrayList;
    }

    public static String getFavoriteListRequestBody(FavoriteListRequest favoriteListRequest) {
        return GsonUtil.toJsonString(favoriteListRequest);
    }
}
