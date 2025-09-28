package com.microsoft.xbox.xle.viewmodel;

import com.microsoft.xbox.service.model.FollowersData;
import com.microsoft.xbox.service.model.ProfileModel;
import com.microsoft.xbox.service.model.UserProfileData;

import org.jetbrains.annotations.NotNull;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class FriendSelectorItem extends FollowersData {
    private static final long serialVersionUID = 5799344980951867134L;
    private boolean selected;

    public FriendSelectorItem(FollowersData followersData) {
        super(followersData);
        this.selected = false;
    }

    public FriendSelectorItem(@NotNull ProfileModel profileModel) {
        this.xuid = profileModel.getXuid();
        this.userProfileData = new UserProfileData();
        this.userProfileData.gamerTag = profileModel.getGamerTag();
        this.userProfileData.xuid = profileModel.getXuid();
        this.userProfileData.profileImageUrl = profileModel.getGamerPicImageUrl();
        this.userProfileData.gamerScore = profileModel.getGamerScore();
        this.userProfileData.appDisplayName = profileModel.getAppDisplayName();
        this.userProfileData.accountTier = profileModel.getAccountTier();
        this.userProfileData.gamerRealName = profileModel.getRealName();
    }

    public void toggleSelection() {
        this.selected = !this.selected;
    }

    public boolean getIsSelected() {
        return this.selected;
    }

    public void setSelected(boolean z) {
        this.selected = z;
    }

    public int hashCode() {
        return 31 + ((this.userProfileData == null || this.userProfileData.gamerTag == null) ? 0 : this.userProfileData.gamerTag.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FriendSelectorItem friendSelectorItem = (FriendSelectorItem) obj;
        if (this.userProfileData == null || this.userProfileData.gamerTag == null) {
            return friendSelectorItem.userProfileData == null && friendSelectorItem.userProfileData.gamerTag == null;
        } else
            return this.userProfileData.gamerTag.equals(friendSelectorItem.userProfileData.gamerTag);
    }
}
