package com.microsoft.xbox.service.model;

import com.microsoft.xbox.service.network.managers.IPeopleHubResult;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.xle.app.XLEUtil;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class RecommendationsPeopleData extends FollowersData {
    private IPeopleHubResult.PeopleHubRecommendation recommendationInfo;

    public RecommendationsPeopleData(IPeopleHubResult.PeopleHubPersonSummary peopleHubPersonSummary) {
        super(peopleHubPersonSummary);
        XLEAssert.assertNotNull(peopleHubPersonSummary.recommendation);
        this.recommendationInfo = peopleHubPersonSummary.recommendation;
    }

    public RecommendationsPeopleData(boolean z, FollowersData.DummyType dummyType) {
        super(z, dummyType);
    }

    public String getRecommendationFirstReason() {
        return XLEUtil.isNullOrEmpty(this.recommendationInfo.Reasons) ? "" : this.recommendationInfo.Reasons.get(0);
    }

    public boolean getIsFacebookFriend() {
        return this.recommendationInfo.getRecommendationType() == IPeopleHubResult.RecommendationType.FacebookFriend;
    }

    public IPeopleHubResult.RecommendationType getRecommendationType() {
        return this.recommendationInfo.getRecommendationType();
    }
}
