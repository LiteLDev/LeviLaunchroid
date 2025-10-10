package com.microsoft.xbox.service.model.sls;

import com.microsoft.xbox.toolkit.GsonUtil;


public class SubmitFeedbackRequest {
    public String evidenceId;
    public FeedbackType feedbackType;
    public String sessionRef;
    public String textReason;
    public String voiceReasonId;
    public long xuid;

    public SubmitFeedbackRequest(long j, String str, FeedbackType feedbackType2, String str2, String str3, String str4) {
        this.xuid = j;
        this.sessionRef = str;
        this.feedbackType = feedbackType2;
        this.textReason = str2;
        this.voiceReasonId = str3;
        this.evidenceId = str4;
    }

    public static String getSubmitFeedbackRequestBody(SubmitFeedbackRequest submitFeedbackRequest) {
        return GsonUtil.toJsonString(submitFeedbackRequest);
    }
}
