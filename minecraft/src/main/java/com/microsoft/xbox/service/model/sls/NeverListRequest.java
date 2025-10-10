package com.microsoft.xbox.service.model.sls;

import com.microsoft.xbox.toolkit.GsonUtil;


public class NeverListRequest {
    public long xuid;

    public NeverListRequest(long j) {
        this.xuid = j;
    }

    public static String getNeverListRequestBody(NeverListRequest neverListRequest) {
        return GsonUtil.toJsonString(neverListRequest);
    }
}
