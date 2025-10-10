package com.microsoft.xbox.service.model.sls;

import com.microsoft.xbox.toolkit.GsonUtil;

import java.util.ArrayList;


public class UserPresenceBatchRequest {
    public String level = "all";
    public ArrayList<String> users;

    public UserPresenceBatchRequest(ArrayList<String> arrayList) {
        this.users = arrayList;
    }

    public static String getUserPresenceBatchRequestBody(UserPresenceBatchRequest userPresenceBatchRequest) {
        return GsonUtil.toJsonString(userPresenceBatchRequest);
    }
}
