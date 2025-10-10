package com.microsoft.xbox.xle.viewmodel;

import com.mojang.minecraftpe.R;
import com.microsoft.xbox.service.model.FollowersData;
import com.microsoft.xbox.service.model.ProfileModel;
import com.microsoft.xbox.service.network.managers.AddFollowingUserResponseContainer;
import com.microsoft.xbox.telemetry.helpers.UTCChangeRelationship;
import com.microsoft.xbox.toolkit.AsyncActionStatus;
import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.NetworkAsyncTask;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.network.ListState;
import com.microsoft.xbox.xle.app.SGProjectSpecificDialogManager;
import com.microsoft.xbox.xle.app.activity.Profile.ProfileScreenViewModel;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


public class ChangeFriendshipDialogViewModel {
    private static final String TAG = ChangeFriendshipDialogViewModel.class.getSimpleName();
    private final HashSet<ProfileScreenViewModel.ChangeFriendshipFormOptions> changeFriendshipForm = new HashSet<>();
    public boolean isAddingUserToFavoriteList;
    public boolean isAddingUserToFollowingList;
    public boolean isAddingUserToShareIdentityList;
    public boolean isLoadingUserProfile;
    public boolean isRemovingUserFromFavoriteList;
    public boolean isRemovingUserFromFollowingList;
    public boolean isRemovingUserFromShareIdentityList;
    public ProfileModel model;
    private AddUserToFavoriteListAsyncTask addUserToFavoriteListAsyncTask;
    private AddUserToFollowingListAsyncTask addUserToFollowingListAsyncTask;
    private AddUserToShareIdentityListAsyncTask addUserToShareIdentityListAsyncTask;
    private boolean isFavorite = false;
    private boolean isFollowing = false;
    private boolean isSharingRealNameEnd;
    private boolean isSharingRealNameStart;
    private LoadPersonDataAsyncTask loadProfileAsyncTask;
    private RemoveUserFromFavoriteListAsyncTask removeUserFromFavoriteListAsyncTask;
    private RemoveUserFromFollowingListAsyncTask removeUserFromFollowingListAsyncTask;
    private RemoveUserFromShareIdentityListAsyncTask removeUserFromShareIdentityListAsyncTask;
    private ListState viewModelState = ListState.LoadingState;

    public ChangeFriendshipDialogViewModel(@NotNull ProfileModel profileModel) {
        XLEAssert.assertTrue(!ProfileModel.isMeXuid(profileModel.getXuid()));
        this.model = profileModel;
    }

    public ListState getViewModelState() {
        return this.viewModelState;
    }

    public String getGamerTag() {
        return this.model.getGamerTag();
    }

    public String getGamerPicUrl() {
        return this.model.getGamerPicImageUrl();
    }

    public String getRealName() {
        return this.model.getRealName();
    }

    public String getGamerScore() {
        return this.model.getGamerScore();
    }

    public int getPreferredColor() {
        return this.model.getPreferedColor();
    }

    public boolean getIsFollowing() {
        return this.model.isCallerFollowingTarget();
    }

    public boolean getIsFavorite() {
        return this.model.hasCallerMarkedTargetAsFavorite();
    }

    public String getXuid() {
        return this.model.getXuid();
    }

    public boolean getCallerMarkedTargetAsIdentityShared() {
        return this.model.hasCallerMarkedTargetAsIdentityShared();
    }

    public String getCallerShareRealNameStatus() {
        ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
        return meProfileModel != null ? meProfileModel.getShareRealNameStatus() : "";
    }

    public String getCallerGamerTag() {
        ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
        return meProfileModel != null ? meProfileModel.getGamerTag() : "";
    }

    public void setShouldAddUserToFriendList(boolean z) {
        if (z) {
            this.changeFriendshipForm.add(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToFriendList);
        } else {
            this.changeFriendshipForm.remove(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToFriendList);
        }
    }

    public void setShouldAddUserToFavoriteList(boolean z) {
        if (z) {
            this.changeFriendshipForm.add(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToFavoriteList);
        } else {
            this.changeFriendshipForm.remove(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToFavoriteList);
        }
    }

    public void setShouldRemoveUserFromFavoriteList(boolean z) {
        if (z) {
            this.changeFriendshipForm.add(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldRemoveUserFromFavoriteList);
        } else {
            this.changeFriendshipForm.remove(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldRemoveUserFromFavoriteList);
        }
    }

    public void setShouldAddUserToShareIdentityList(boolean z) {
        if (z) {
            this.changeFriendshipForm.add(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToShareIdentityList);
        } else {
            this.changeFriendshipForm.remove(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToShareIdentityList);
        }
    }

    public void setShouldRemoveUserFroShareIdentityList(boolean z) {
        if (z) {
            this.changeFriendshipForm.add(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldRemoveUserFromShareIdentityList);
        } else {
            this.changeFriendshipForm.remove(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldRemoveUserFromShareIdentityList);
        }
    }

    public void onChangeRelationshipCompleted() {
        boolean z;
        UTCChangeRelationship.Relationship relationship = this.model.isCallerFollowingTarget() ? UTCChangeRelationship.Relationship.EXISTINGFRIEND : UTCChangeRelationship.Relationship.NOTCHANGED;
        UTCChangeRelationship.FavoriteStatus favoriteStatus = this.model.hasCallerMarkedTargetAsFavorite() ? UTCChangeRelationship.FavoriteStatus.EXISTINGFAVORITE : UTCChangeRelationship.FavoriteStatus.EXISTINGNOTFAVORITED;
        UTCChangeRelationship.RealNameStatus realNameStatus = this.model.hasCallerMarkedTargetAsIdentityShared() ? UTCChangeRelationship.RealNameStatus.EXISTINGSHARED : UTCChangeRelationship.RealNameStatus.EXISTINGNOTSHARED;
        UTCChangeRelationship.GamerType gamerType = UTCChangeRelationship.GamerType.NORMAL;
        boolean z2 = true;
        if (this.changeFriendshipForm.contains(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToFriendList)) {
            relationship = UTCChangeRelationship.Relationship.ADDFRIEND;
            addFollowingUser();
            z = true;
        } else {
            z = false;
        }
        if (this.changeFriendshipForm.contains(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldRemoveUserFromFriendList)) {
            relationship = UTCChangeRelationship.Relationship.REMOVEFRIEND;
            removeFollowingUser();
            z = true;
        }
        if (this.changeFriendshipForm.contains(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToFavoriteList)) {
            favoriteStatus = UTCChangeRelationship.FavoriteStatus.FAVORITED;
            addFavoriteUser();
            z = true;
        }
        if (this.changeFriendshipForm.contains(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldRemoveUserFromFavoriteList)) {
            favoriteStatus = UTCChangeRelationship.FavoriteStatus.UNFAVORITED;
            removeFavoriteUser();
            z = true;
        }
        if (this.changeFriendshipForm.contains(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldAddUserToShareIdentityList)) {
            realNameStatus = UTCChangeRelationship.RealNameStatus.SHARINGON;
            addUserToShareIdentityList();
            z = true;
        }
        if (this.changeFriendshipForm.contains(ProfileScreenViewModel.ChangeFriendshipFormOptions.ShouldRemoveUserFromShareIdentityList)) {
            realNameStatus = UTCChangeRelationship.RealNameStatus.SHARINGOFF;
            removeUserFromShareIdentityList();
        } else {
            z2 = z;
        }
        if (!z2) {
            notifyDialogAsyncTaskCompleted();
        } else {
            UTCChangeRelationship.trackChangeRelationshipDone(relationship, realNameStatus, favoriteStatus, gamerType);
        }
    }

    public void clearChangeFriendshipForm() {
        this.changeFriendshipForm.clear();
    }

    public void setInitialRealNameSharingState(boolean z) {
        this.isSharingRealNameStart = z;
        this.isSharingRealNameEnd = z;
    }

    public boolean getIsSharingRealNameStart() {
        return this.isSharingRealNameStart;
    }

    public boolean getIsSharingRealNameEnd() {
        return this.isSharingRealNameEnd;
    }

    public void setIsSharingRealNameEnd(boolean z) {
        this.isSharingRealNameEnd = z;
    }

    private void showError(int i) {
        DialogManager.getInstance().showToast(i);
    }

    public String getDialogButtonText() {
        if (this.isFollowing) {
            return XboxTcuiSdk.getResources().getString(R.string.TextInput_Confirm);
        }
        return XboxTcuiSdk.getResources().getString(R.string.OK_Text);
    }

    public boolean isBusy() {
        return this.isLoadingUserProfile || this.isAddingUserToFavoriteList || this.isRemovingUserFromFavoriteList || this.isAddingUserToFollowingList || this.isRemovingUserFromFollowingList || this.isAddingUserToShareIdentityList || this.isRemovingUserFromShareIdentityList;
    }

    public void load() {
        if (loadProfileAsyncTask != null) {
            loadProfileAsyncTask.cancel();
        }
        loadProfileAsyncTask = new LoadPersonDataAsyncTask();
        loadProfileAsyncTask.load(true);
    }

    public void addFavoriteUser() {
        AddUserToFavoriteListAsyncTask addUserToFavoriteListAsyncTask2 = this.addUserToFavoriteListAsyncTask;
        if (addUserToFavoriteListAsyncTask2 != null) {
            addUserToFavoriteListAsyncTask2.cancel();
        }
        AddUserToFavoriteListAsyncTask addUserToFavoriteListAsyncTask3 = new AddUserToFavoriteListAsyncTask(this.model.getXuid());
        this.addUserToFavoriteListAsyncTask = addUserToFavoriteListAsyncTask3;
        addUserToFavoriteListAsyncTask3.load(true);
    }

    public void removeFavoriteUser() {
        RemoveUserFromFavoriteListAsyncTask removeUserFromFavoriteListAsyncTask2 = this.removeUserFromFavoriteListAsyncTask;
        if (removeUserFromFavoriteListAsyncTask2 != null) {
            removeUserFromFavoriteListAsyncTask2.cancel();
        }
        RemoveUserFromFavoriteListAsyncTask removeUserFromFavoriteListAsyncTask3 = new RemoveUserFromFavoriteListAsyncTask(this.model.getXuid());
        this.removeUserFromFavoriteListAsyncTask = removeUserFromFavoriteListAsyncTask3;
        removeUserFromFavoriteListAsyncTask3.load(true);
    }

    public void addUserToShareIdentityList() {
        AddUserToShareIdentityListAsyncTask addUserToShareIdentityListAsyncTask2 = this.addUserToShareIdentityListAsyncTask;
        if (addUserToShareIdentityListAsyncTask2 != null) {
            addUserToShareIdentityListAsyncTask2.cancel();
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.model.getXuid());
        AddUserToShareIdentityListAsyncTask addUserToShareIdentityListAsyncTask3 = new AddUserToShareIdentityListAsyncTask(arrayList);
        this.addUserToShareIdentityListAsyncTask = addUserToShareIdentityListAsyncTask3;
        addUserToShareIdentityListAsyncTask3.load(true);
    }

    public void removeUserFromShareIdentityList() {
        if (this.removeUserFromFollowingListAsyncTask != null) {
            this.removeUserFromFavoriteListAsyncTask.cancel();
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.model.getXuid());
        RemoveUserFromShareIdentityListAsyncTask removeUserFromShareIdentityListAsyncTask2 = new RemoveUserFromShareIdentityListAsyncTask(arrayList);
        this.removeUserFromShareIdentityListAsyncTask = removeUserFromShareIdentityListAsyncTask2;
        removeUserFromShareIdentityListAsyncTask2.load(true);
    }

    private void notifyDialogUpdateView() {
        ((SGProjectSpecificDialogManager) DialogManager.getInstance().getManager()).notifyChangeFriendshipDialogUpdateView();
    }

    private void notifyDialogAsyncTaskCompleted() {
        ((SGProjectSpecificDialogManager) DialogManager.getInstance().getManager()).notifyChangeFriendshipDialogAsyncTaskCompleted();
    }

    private void notifyDialogAsyncTaskFailed(String str) {
        ((SGProjectSpecificDialogManager) DialogManager.getInstance().getManager()).notifyChangeFriendshipDialogAsyncTaskFailed(str);
    }

    public void addFollowingUser() {
        if (ProfileModel.hasPrivilegeToAddFriend()) {
            AddUserToFollowingListAsyncTask addUserToFollowingListAsyncTask2 = this.addUserToFollowingListAsyncTask;
            if (addUserToFollowingListAsyncTask2 != null) {
                addUserToFollowingListAsyncTask2.cancel();
            }
            AddUserToFollowingListAsyncTask addUserToFollowingListAsyncTask3 = new AddUserToFollowingListAsyncTask(this.model.getXuid());
            this.addUserToFollowingListAsyncTask = addUserToFollowingListAsyncTask3;
            addUserToFollowingListAsyncTask3.load(true);
            return;
        }
        showError(R.string.Global_MissingPrivilegeError_DialogBody);
    }

    public void removeFollowingUser() {
        RemoveUserFromFollowingListAsyncTask removeUserFromFollowingListAsyncTask2 = this.removeUserFromFollowingListAsyncTask;
        if (removeUserFromFollowingListAsyncTask2 != null) {
            removeUserFromFollowingListAsyncTask2.cancel();
        }
        RemoveUserFromFollowingListAsyncTask removeUserFromFollowingListAsyncTask3 = new RemoveUserFromFollowingListAsyncTask(this.model.getXuid());
        this.removeUserFromFollowingListAsyncTask = removeUserFromFollowingListAsyncTask3;
        removeUserFromFollowingListAsyncTask3.load(true);
    }

    public void onLoadPersonDataCompleted(@NotNull AsyncActionStatus status) {
        isLoadingUserProfile = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                if (model.getProfileSummaryData() == null) {
                    viewModelState = ListState.ErrorState;
                    break;
                } else {
                    viewModelState = ListState.ValidContentState;
                    break;
                }
            case FAIL:
            case NO_OP_FAIL:
                viewModelState = ListState.ErrorState;
                break;
        }
        notifyDialogUpdateView();
    }

    public void onAddUseToShareIdentityListCompleted(@NotNull AsyncActionStatus status) {
        isAddingUserToShareIdentityList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                notifyDialogAsyncTaskCompleted();
                return;
            case FAIL:
            case NO_OP_FAIL:
                notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorChangeRemove));
                return;
            default:
                return;
        }
    }

    public void onRemoveUserFromShareIdentityListCompleted(@NotNull AsyncActionStatus status) {
        isRemovingUserFromShareIdentityList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                notifyDialogAsyncTaskCompleted();
                return;
            case FAIL:
            case NO_OP_FAIL:
                notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorChangeRemove));
                return;
            default:
                return;
        }
    }

    public void onAddUserToFavoriteListCompleted(@NotNull AsyncActionStatus status, boolean isFavorite2) {
        isAddingUserToFavoriteList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                isFavorite = isFavorite2;
                notifyDialogAsyncTaskCompleted();
                return;
            case FAIL:
            case NO_OP_FAIL:
                notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorChangeRemove));
                return;
            default:
                return;
        }
    }

    public void onRemoveUserFromFavoriteListCompleted(@NotNull AsyncActionStatus status, boolean isFavorite2) {
        isRemovingUserFromFavoriteList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                isFavorite = isFavorite2;
                notifyDialogAsyncTaskCompleted();
                return;
            case FAIL:
            case NO_OP_FAIL:
                notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorChangeRemove));
                return;
            default:
                return;
        }
    }

    public void onAddUserToFollowingListCompleted(@NotNull AsyncActionStatus status, boolean isFollowing2) {
        isAddingUserToFollowingList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                isFollowing = isFollowing2;
                notifyDialogAsyncTaskCompleted();
                return;
            case FAIL:
            case NO_OP_FAIL:
                AddFollowingUserResponseContainer.AddFollowingUserResponse result = null;
                ProfileModel meProfile = ProfileModel.getMeProfileModel();
                if (meProfile != null) {
                    result = meProfile.getAddUserToFollowingResult();
                }
                if (result == null || result.getAddFollowingRequestStatus() || result.code != 1028) {
                    notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorAddingFriend));
                    return;
                } else {
                    notifyDialogAsyncTaskFailed(result.description);
                    return;
                }
            default:
                return;
        }
    }

    public void onRemoveUserFromFollowingListCompleted(@NotNull AsyncActionStatus status, boolean isFollowing2) {
        isRemovingUserFromFollowingList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                isFollowing = isFollowing2;
                if (isFavorite && !isFollowing) {
                    isFavorite = false;
                }
                notifyDialogAsyncTaskCompleted();
                return;
            case FAIL:
            case NO_OP_FAIL:
                notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorChangeRemove));
                return;
            default:
                return;
        }
    }

    private class LoadPersonDataAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private LoadPersonDataAsyncTask() {
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return false;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ChangeFriendshipDialogViewModel.this.onLoadPersonDataCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ChangeFriendshipDialogViewModel.this.isLoadingUserProfile = true;
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ChangeFriendshipDialogViewModel.this.onLoadPersonDataCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            XLEAssert.assertNotNull(ChangeFriendshipDialogViewModel.this.model);
            return ChangeFriendshipDialogViewModel.this.model.loadProfileSummary(this.forceLoad).getStatus();
        }
    }

    private class RemoveUserFromShareIdentityListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final ArrayList<String> usersToAdd;

        public RemoveUserFromShareIdentityListAsyncTask(ArrayList<String> arrayList) {
            this.usersToAdd = arrayList;
        }

        public boolean checkShouldExecute() {
            return true;
        }

        public void onNoAction() {
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ChangeFriendshipDialogViewModel.this.isRemovingUserFromShareIdentityList = true;
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ChangeFriendshipDialogViewModel.this.onRemoveUserFromShareIdentityListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel != null) {
                return meProfileModel.removeUserFromShareIdentity(this.forceLoad, this.usersToAdd).getStatus();
            }
            return AsyncActionStatus.FAIL;
        }
    }

    private class AddUserToShareIdentityListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final ArrayList<String> usersToAdd;

        public AddUserToShareIdentityListAsyncTask(ArrayList<String> arrayList) {
            this.usersToAdd = arrayList;
        }

        public boolean checkShouldExecute() {
            return true;
        }

        public void onNoAction() {
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ChangeFriendshipDialogViewModel.this.isAddingUserToShareIdentityList = true;
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ChangeFriendshipDialogViewModel.this.onAddUseToShareIdentityListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel != null) {
                return meProfileModel.addUserToShareIdentity(this.forceLoad, this.usersToAdd).getStatus();
            }
            return AsyncActionStatus.FAIL;
        }
    }

    private class AddUserToFavoriteListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String favoriteUserXuid;
        private boolean favoriteUser = false;

        public AddUserToFavoriteListAsyncTask(String str) {
            this.favoriteUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ChangeFriendshipDialogViewModel.this.onAddUserToFavoriteListCompleted(AsyncActionStatus.NO_CHANGE, this.favoriteUser);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ChangeFriendshipDialogViewModel.this.isAddingUserToFavoriteList = true;
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ChangeFriendshipDialogViewModel.this.onAddUserToFavoriteListCompleted(asyncActionStatus, this.favoriteUser);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        /* access modifiers changed from: protected */
        public AsyncActionStatus loadDataInBackground() {
            ArrayList<FollowersData> favorites;
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel == null) {
                return AsyncActionStatus.FAIL;
            }
            AsyncActionStatus status = meProfileModel.addUserToFavoriteList(this.forceLoad, this.favoriteUserXuid).getStatus();
            if ((status == AsyncActionStatus.SUCCESS || status == AsyncActionStatus.NO_CHANGE || status == AsyncActionStatus.NO_OP_SUCCESS) && (favorites = meProfileModel.getFavorites()) != null) {
                Iterator<FollowersData> it = favorites.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    FollowersData next = it.next();
                    if (next.xuid.equals(this.favoriteUserXuid)) {
                        this.favoriteUser = next.isFavorite;
                        break;
                    }
                }
            }
            return status;
        }
    }

    private class RemoveUserFromFavoriteListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String favoriteUserXuid;
        private boolean favoriteUser = false;

        public RemoveUserFromFavoriteListAsyncTask(String str) {
            this.favoriteUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ChangeFriendshipDialogViewModel.this.onRemoveUserFromFavoriteListCompleted(AsyncActionStatus.NO_CHANGE, this.favoriteUser);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ChangeFriendshipDialogViewModel.this.isRemovingUserFromFavoriteList = true;
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ChangeFriendshipDialogViewModel.this.onRemoveUserFromFavoriteListCompleted(asyncActionStatus, this.favoriteUser);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ArrayList<FollowersData> favorites;
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel == null) {
                return AsyncActionStatus.FAIL;
            }
            AsyncActionStatus status = meProfileModel.removeUserFromFavoriteList(this.forceLoad, this.favoriteUserXuid).getStatus();
            if ((status == AsyncActionStatus.SUCCESS || status == AsyncActionStatus.NO_CHANGE || status == AsyncActionStatus.NO_OP_SUCCESS) && (favorites = meProfileModel.getFavorites()) != null) {
                Iterator<FollowersData> it = favorites.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    FollowersData next = it.next();
                    if (next.xuid.equals(this.favoriteUserXuid)) {
                        this.favoriteUser = next.isFavorite;
                        break;
                    }
                }
            }
            return status;
        }
    }

    private class AddUserToFollowingListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String followingUserXuid;
        private boolean isFollowingUser = false;

        public AddUserToFollowingListAsyncTask(String str) {
            this.followingUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ChangeFriendshipDialogViewModel.this.onAddUserToFollowingListCompleted(AsyncActionStatus.NO_CHANGE, this.isFollowingUser);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ChangeFriendshipDialogViewModel.this.isAddingUserToFollowingList = true;
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ChangeFriendshipDialogViewModel.this.onAddUserToFollowingListCompleted(asyncActionStatus, this.isFollowingUser);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel == null) {
                return AsyncActionStatus.FAIL;
            }
            AsyncActionStatus status = meProfileModel.addUserToFollowingList(this.forceLoad, this.followingUserXuid).getStatus();
            if (!AsyncActionStatus.getIsFail(status)) {
                AddFollowingUserResponseContainer.AddFollowingUserResponse addUserToFollowingResult = meProfileModel.getAddUserToFollowingResult();
                if (addUserToFollowingResult != null && !addUserToFollowingResult.getAddFollowingRequestStatus() && addUserToFollowingResult.code == 1028) {
                    return AsyncActionStatus.FAIL;
                }
                ChangeFriendshipDialogViewModel.this.model.loadProfileSummary(true);
                meProfileModel.loadProfileSummary(true);
                ArrayList<FollowersData> followingData = meProfileModel.getFollowingData();
                if (followingData != null) {
                    Iterator<FollowersData> it = followingData.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (it.next().xuid.equals(this.followingUserXuid)) {
                                this.isFollowingUser = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
            return status;
        }
    }

    private class RemoveUserFromFollowingListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String followingUserXuid;
        private boolean isFollowingUser = true;

        public RemoveUserFromFollowingListAsyncTask(String str) {
            this.followingUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            onRemoveUserFromFollowingListCompleted(AsyncActionStatus.NO_CHANGE, this.isFollowingUser);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ChangeFriendshipDialogViewModel.this.isRemovingUserFromFollowingList = true;
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ChangeFriendshipDialogViewModel.this.onRemoveUserFromFollowingListCompleted(asyncActionStatus, this.isFollowingUser);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel == null) {
                return AsyncActionStatus.FAIL;
            }
            AsyncActionStatus status = meProfileModel.removeUserFromFollowingList(this.forceLoad, this.followingUserXuid).getStatus();
            if (!AsyncActionStatus.getIsFail(status)) {
                ChangeFriendshipDialogViewModel.this.model.loadProfileSummary(true);
                meProfileModel.loadProfileSummary(true);
                this.isFollowingUser = false;
            }
            return status;
        }
    }
}
