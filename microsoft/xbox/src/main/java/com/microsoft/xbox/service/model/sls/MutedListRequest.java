package com.microsoft.xbox.service.model.sls;

import com.microsoft.xbox.toolkit.GsonUtil;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class MutedListRequest {
    public long xuid;

    public MutedListRequest(long j) {
        this.xuid = j;
    }

    public static String getNeverListRequestBody(MutedListRequest mutedListRequest) {
        return GsonUtil.toJsonString(mutedListRequest);
    }
}
