package com.microsoft.xbox.service.model.sls;

import com.microsoft.xbox.toolkit.GsonUtil;


public class MutedListRequest {
    public long xuid;

    public MutedListRequest(long j) {
        this.xuid = j;
    }

    public static String getNeverListRequestBody(MutedListRequest mutedListRequest) {
        return GsonUtil.toJsonString(mutedListRequest);
    }
}
