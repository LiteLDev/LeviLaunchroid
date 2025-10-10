package com.microsoft.xbox.xle.app.activity.Profile;

import android.app.AlertDialog;

import com.mojang.minecraftpe.R;
import com.microsoft.xbox.service.model.FollowersData;
import com.microsoft.xbox.service.model.ProfileModel;
import com.microsoft.xbox.service.network.managers.AddFollowingUserResponseContainer;
import com.microsoft.xbox.service.network.managers.MutedListResultContainer;
import com.microsoft.xbox.service.network.managers.NeverListResultContainer;
import com.microsoft.xbox.telemetry.helpers.UTCChangeRelationship;
import com.microsoft.xbox.telemetry.helpers.UTCPeopleHub;
import com.microsoft.xbox.toolkit.AsyncActionStatus;
import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.NetworkAsyncTask;
import com.microsoft.xbox.toolkit.ThreadManager;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.toolkit.ui.ScreenLayout;
import com.microsoft.xbox.xle.app.SGProjectSpecificDialogManager;
import com.microsoft.xbox.xle.app.activity.ReportUserScreen;
import com.microsoft.xbox.xle.app.adapter.ProfileScreenAdapter;
import com.microsoft.xbox.xle.viewmodel.ChangeFriendshipDialogViewModel;
import com.microsoft.xbox.xle.viewmodel.ViewModelBase;
import com.microsoft.xbox.xle.viewmodel.XLEGlobalData;
import com.microsoft.xboxtcui.XboxAppDeepLinker;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


public class ProfileScreenViewModel extends ViewModelBase {
    private static final String TAG = ProfileScreenViewModel.class.getSimpleName();
    private final HashSet<ChangeFriendshipFormOptions> changeFriendshipForm = new HashSet<>();
    private final boolean isFavorite = false;
    public boolean isAddingUserToBlockList;
    public boolean isAddingUserToFollowingList;
    public boolean isAddingUserToMutedList;
    public boolean isAddingUserToShareIdentityList;
    public boolean isLoadingUserMutedList;
    public boolean isLoadingUserNeverList;
    public boolean isLoadingUserProfile;
    public boolean isRemovingUserFromBlockList;
    public boolean isRemovingUserFromMutedList;
    protected ProfileModel model = ProfileModel.getProfileModel(NavigationManager.getInstance().getActivityParameters().getSelectedProfile());
    private AddUserToFollowingListAsyncTask addUserToFollowingListAsyncTask;
    private AddUserToMutedListAsyncTask addUserToMutedListAsyncTask;
    private AddUserToNeverListAsyncTask addUserToNeverListAsyncTask;
    private AddUserToShareIdentityListAsyncTask addUserToShareIdentityListAsyncTask;
    private FollowersData basicData;
    private ChangeFriendshipDialogViewModel changeFriendshipDialogViewModel;
    private boolean isBlocked = false;
    private boolean isFollowing = false;
    private boolean isMuted = false;
    private boolean isShowingFailureDialog;
    private LoadUserProfileAsyncTask loadMeProfileTask;
    private LoadUserMutedListAsyncTask loadUserMutedListTask;
    private LoadUserNeverListAsyncTask loadUserNeverListTask;
    private LoadUserProfileAsyncTask loadUserProfileTask;
    private RemoveUserFromMutedListAsyncTask removeUserFromMutedListAsyncTask;
    private RemoveUserToNeverListAsyncTask removeUserToNeverListAsyncTask;

    public ProfileScreenViewModel(ScreenLayout screenLayout) {
        super(screenLayout);
        this.adapter = new ProfileScreenAdapter(this);
    }

    public boolean isFacebookFriend() {
        return false;
    }

    public void onRehydrate() {
        this.adapter = new ProfileScreenAdapter(this);
    }

    public String getGamerTag() {
        return this.model.getGamerTag();
    }

    public String getGamerScore() {
        return this.model.getGamerScore();
    }

    public String getGamerPicUrl() {
        return this.model.getGamerPicImageUrl();
    }

    public String getXuid() {
        return this.model.getXuid();
    }

    public String getRealName() {
        return this.model.getRealName();
    }

    public boolean isCallerFollowingTarget() {
        return this.isFollowing;
    }

    public int getPreferredColor() {
        return this.model.getPreferedColor();
    }

    public boolean isMeProfile() {
        return this.model.isMeProfile();
    }

    public void onStartOverride() {
        this.isShowingFailureDialog = false;
    }

    public boolean isBusy() {
        return this.isLoadingUserProfile || this.isLoadingUserNeverList || this.isLoadingUserMutedList || this.isAddingUserToFollowingList || this.isAddingUserToShareIdentityList || this.isRemovingUserFromBlockList || this.isAddingUserToBlockList || this.isAddingUserToMutedList || this.isRemovingUserFromMutedList;
    }

    public void onStopOverride() {
        LoadUserProfileAsyncTask loadUserProfileAsyncTask = this.loadMeProfileTask;
        if (loadUserProfileAsyncTask != null) {
            loadUserProfileAsyncTask.cancel();
        }
        LoadUserNeverListAsyncTask loadUserNeverListAsyncTask = this.loadUserNeverListTask;
        if (loadUserNeverListAsyncTask != null) {
            loadUserNeverListAsyncTask.cancel();
        }
        LoadUserMutedListAsyncTask loadUserMutedListAsyncTask = this.loadUserMutedListTask;
        if (loadUserMutedListAsyncTask != null) {
            loadUserMutedListAsyncTask.cancel();
        }
        LoadUserProfileAsyncTask loadUserProfileAsyncTask2 = this.loadUserProfileTask;
        if (loadUserProfileAsyncTask2 != null) {
            loadUserProfileAsyncTask2.cancel();
        }
        AddUserToFollowingListAsyncTask addUserToFollowingListAsyncTask2 = this.addUserToFollowingListAsyncTask;
        if (addUserToFollowingListAsyncTask2 != null) {
            addUserToFollowingListAsyncTask2.cancel();
        }
        AddUserToShareIdentityListAsyncTask addUserToShareIdentityListAsyncTask2 = this.addUserToShareIdentityListAsyncTask;
        if (addUserToShareIdentityListAsyncTask2 != null) {
            addUserToShareIdentityListAsyncTask2.cancel();
        }
        AddUserToNeverListAsyncTask addUserToNeverListAsyncTask2 = this.addUserToNeverListAsyncTask;
        if (addUserToNeverListAsyncTask2 != null) {
            addUserToNeverListAsyncTask2.cancel();
        }
        RemoveUserToNeverListAsyncTask removeUserToNeverListAsyncTask2 = this.removeUserToNeverListAsyncTask;
        if (removeUserToNeverListAsyncTask2 != null) {
            removeUserToNeverListAsyncTask2.cancel();
        }
        AddUserToMutedListAsyncTask addUserToMutedListAsyncTask2 = this.addUserToMutedListAsyncTask;
        if (addUserToMutedListAsyncTask2 != null) {
            addUserToMutedListAsyncTask2.cancel();
        }
        RemoveUserFromMutedListAsyncTask removeUserFromMutedListAsyncTask2 = this.removeUserFromMutedListAsyncTask;
        if (removeUserFromMutedListAsyncTask2 != null) {
            removeUserFromMutedListAsyncTask2.cancel();
        }
    }

    public void load(boolean z) {
        LoadUserProfileAsyncTask loadUserProfileAsyncTask = this.loadUserProfileTask;
        if (loadUserProfileAsyncTask != null) {
            loadUserProfileAsyncTask.cancel();
        }
        LoadUserProfileAsyncTask loadUserProfileAsyncTask2 = new LoadUserProfileAsyncTask(ProfileModel.getMeProfileModel());
        this.loadMeProfileTask = loadUserProfileAsyncTask2;
        loadUserProfileAsyncTask2.load(true);
        if (!isMeProfile()) {
            LoadUserNeverListAsyncTask loadUserNeverListAsyncTask = this.loadUserNeverListTask;
            if (loadUserNeverListAsyncTask != null) {
                loadUserNeverListAsyncTask.cancel();
            }
            LoadUserNeverListAsyncTask loadUserNeverListAsyncTask2 = new LoadUserNeverListAsyncTask(ProfileModel.getMeProfileModel());
            this.loadUserNeverListTask = loadUserNeverListAsyncTask2;
            loadUserNeverListAsyncTask2.load(true);
            LoadUserMutedListAsyncTask loadUserMutedListAsyncTask = this.loadUserMutedListTask;
            if (loadUserMutedListAsyncTask != null) {
                loadUserMutedListAsyncTask.cancel();
            }
            LoadUserMutedListAsyncTask loadUserMutedListAsyncTask2 = new LoadUserMutedListAsyncTask(ProfileModel.getMeProfileModel());
            this.loadUserMutedListTask = loadUserMutedListAsyncTask2;
            loadUserMutedListAsyncTask2.load(true);
            LoadUserProfileAsyncTask loadUserProfileAsyncTask3 = new LoadUserProfileAsyncTask(this.model);
            this.loadUserProfileTask = loadUserProfileAsyncTask3;
            loadUserProfileAsyncTask3.load(true);
        }
    }

    public boolean getIsFavorite() {
        return this.isFavorite;
    }

    public boolean getIsBlocked() {
        return this.isBlocked;
    }

    public boolean getIsMuted() {
        return this.isMuted;
    }

    public boolean getIsAddingUserToBlockList() {
        return this.isAddingUserToBlockList;
    }

    public boolean getIsRemovingUserFromBlockList() {
        return this.isRemovingUserFromBlockList;
    }

    public boolean getIsAddingUserToMutedList() {
        return this.isAddingUserToMutedList;
    }

    public boolean getIsRemovingUserFromMutedList() {
        return this.isRemovingUserFromMutedList;
    }

    public void navigateToChangeRelationship() {
        if (ProfileModel.hasPrivilegeToAddFriend()) {
            UTCChangeRelationship.trackChangeRelationshipAction(getScreen().getName(), getXuid(), isCallerFollowingTarget(), isFacebookFriend());
            showChangeFriendshipDialog();
            return;
        }
        showError(R.string.Global_MissingPrivilegeError_DialogBody);
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

    private void notifyDialogAsyncTaskCompleted() {
        ((SGProjectSpecificDialogManager) DialogManager.getInstance().getManager()).notifyChangeFriendshipDialogAsyncTaskCompleted();
    }

    private void notifyDialogAsyncTaskFailed(String str) {
        ((SGProjectSpecificDialogManager) DialogManager.getInstance().getManager()).notifyChangeFriendshipDialogAsyncTaskFailed(str);
    }

    public void showChangeFriendshipDialog() {
        if (this.changeFriendshipDialogViewModel == null) {
            this.changeFriendshipDialogViewModel = new ChangeFriendshipDialogViewModel(this.model);
        }
        ((SGProjectSpecificDialogManager) DialogManager.getInstance().getManager()).showChangeFriendshipDialog(this.changeFriendshipDialogViewModel, this);
    }

    public void blockUser() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        showOkCancelDialog(XboxTcuiSdk.getResources().getString(R.string.Messages_BlockUserConfirmation_DialogTitle), XboxTcuiSdk.getResources().getString(R.string.Messages_BlockUserConfirmation_DialogBody), XboxTcuiSdk.getResources().getString(R.string.OK_Text), () -> ProfileScreenViewModel.this.blockUserInternal(), XboxTcuiSdk.getResources().getString(R.string.MessageDialog_Cancel), null);
        updateAdapter();
    }

    public void blockUserInternal() {
        UTCPeopleHub.trackBlockDialogComplete();
        AddUserToNeverListAsyncTask addUserToNeverListAsyncTask2 = this.addUserToNeverListAsyncTask;
        if (addUserToNeverListAsyncTask2 != null) {
            addUserToNeverListAsyncTask2.cancel();
        }
        AddUserToNeverListAsyncTask addUserToNeverListAsyncTask3 = new AddUserToNeverListAsyncTask(this.model.getXuid());
        this.addUserToNeverListAsyncTask = addUserToNeverListAsyncTask3;
        addUserToNeverListAsyncTask3.load(true);
    }

    public void unblockUser() {
        RemoveUserToNeverListAsyncTask removeUserToNeverListAsyncTask2 = this.removeUserToNeverListAsyncTask;
        if (removeUserToNeverListAsyncTask2 != null) {
            removeUserToNeverListAsyncTask2.cancel();
        }
        RemoveUserToNeverListAsyncTask removeUserToNeverListAsyncTask3 = new RemoveUserToNeverListAsyncTask(this.model.getXuid());
        this.removeUserToNeverListAsyncTask = removeUserToNeverListAsyncTask3;
        removeUserToNeverListAsyncTask3.load(true);
    }

    public void muteUser() {
        AddUserToMutedListAsyncTask addUserToMutedListAsyncTask2 = this.addUserToMutedListAsyncTask;
        if (addUserToMutedListAsyncTask2 != null) {
            addUserToMutedListAsyncTask2.cancel();
        }
        AddUserToMutedListAsyncTask addUserToMutedListAsyncTask3 = new AddUserToMutedListAsyncTask(this.model.getXuid());
        this.addUserToMutedListAsyncTask = addUserToMutedListAsyncTask3;
        addUserToMutedListAsyncTask3.load(true);
    }

    public void unmuteUser() {
        RemoveUserFromMutedListAsyncTask removeUserFromMutedListAsyncTask2 = this.removeUserFromMutedListAsyncTask;
        if (removeUserFromMutedListAsyncTask2 != null) {
            removeUserFromMutedListAsyncTask2.cancel();
        }
        RemoveUserFromMutedListAsyncTask removeUserFromMutedListAsyncTask3 = new RemoveUserFromMutedListAsyncTask(this.model.getXuid());
        this.removeUserFromMutedListAsyncTask = removeUserFromMutedListAsyncTask3;
        removeUserFromMutedListAsyncTask3.load(true);
    }

    public void showReportDialog() {
        try {
            NavigationManager.getInstance().PopScreensAndReplace(0, ReportUserScreen.class, false, false, false, NavigationManager.getInstance().getActivityParameters());
        } catch (XLEException unused) {
        }
    }

    public void launchXboxApp() {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        showOkCancelDialog(XboxTcuiSdk.getResources().getString(R.string.Messages_BlockUserConfirmation_DialogTitle), XboxTcuiSdk.getResources().getString(R.string.Messages_ViewInXboxApp_DialogBody), XboxTcuiSdk.getResources().getString(R.string.ConnectDialog_ContinueAsGuest), new Runnable() {
            public void run() {
                UTCPeopleHub.trackViewInXboxAppDialogComplete();
                XboxAppDeepLinker.showUserProfile(XboxTcuiSdk.getActivity(), ProfileScreenViewModel.this.model.getXuid());
            }
        }, XboxTcuiSdk.getResources().getString(R.string.MessageDialog_Cancel), null);
        updateAdapter();
    }

    public void onLoadUserProfileCompleted(@NotNull AsyncActionStatus status) {
        isLoadingUserProfile = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                if (!isMeProfile() && ProfileModel.getMeProfileModel() != null) {
                    isFollowing = model.isCallerFollowingTarget();
                    break;
                }
            case FAIL:
            case NO_OP_FAIL:
                if (!isShowingFailureDialog) {
                    isShowingFailureDialog = true;
                    AlertDialog.Builder builder = new AlertDialog.Builder(XboxTcuiSdk.getActivity());
                    builder.setMessage(R.string.Service_ErrorText);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.OK_Text, (dialog, which) -> {
                        try {
                            NavigationManager.getInstance().PopAllScreens();
                        } catch (XLEException e) {
                            e.printStackTrace();
                        }
                    });
                    builder.create().show();
                    break;
                }
                break;
        }
        updateAdapter();
    }

    public void onLoadUserNeverListCompleted(@NotNull AsyncActionStatus status) {
        isLoadingUserNeverList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                ProfileModel meProfile = ProfileModel.getMeProfileModel();
                if (!isMeProfile() && meProfile != null) {
                    isBlocked = false;
                    NeverListResultContainer.NeverListResult meNeverList = meProfile.getNeverListData();
                    if (meNeverList != null) {
                        isBlocked = meNeverList.contains(model.getXuid());
                        break;
                    }
                }
                break;
        }
        updateAdapter();
    }

    public void onLoadUserMutedListCompleted(@NotNull AsyncActionStatus status) {
        isLoadingUserMutedList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                ProfileModel meProfile = ProfileModel.getMeProfileModel();
                if (!isMeProfile() && meProfile != null) {
                    isMuted = false;
                    MutedListResultContainer.MutedListResult meMutedList = meProfile.getMutedList();
                    if (meMutedList != null) {
                        isMuted = meMutedList.contains(model.getXuid());
                        break;
                    }
                }
                break;
        }
        updateAdapter();
    }

    public void onAddUserToFollowingListCompleted(@NotNull AsyncActionStatus status, boolean isFollowing2) {
        isAddingUserToFollowingList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                isFollowing = isFollowing2;
                XLEGlobalData.getInstance().AddForceRefresh(ProfileScreenViewModel.class);
                notifyDialogAsyncTaskCompleted();
                break;
            case FAIL:
            case NO_OP_FAIL:
                AddFollowingUserResponseContainer.AddFollowingUserResponse result = null;
                ProfileModel meProfile = ProfileModel.getMeProfileModel();
                if (meProfile != null) {
                    result = meProfile.getAddUserToFollowingResult();
                }
                if (result != null && !result.getAddFollowingRequestStatus() && result.code == 1028) {
                    notifyDialogAsyncTaskFailed(result.description);
                    break;
                } else {
                    notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorAddingFriend));
                    break;
                }
        }
        updateAdapter();
    }

    public void onAddUseToShareIdentityListCompleted(@NotNull AsyncActionStatus status) {
        isAddingUserToShareIdentityList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                notifyDialogAsyncTaskCompleted();
                break;
            case FAIL:
            case NO_OP_FAIL:
                notifyDialogAsyncTaskFailed(XboxTcuiSdk.getResources().getString(R.string.RealNameSharing_ErrorChangeRemove));
                break;
        }
        updateAdapter();
    }

    public void onAddUserToBlockListCompleted(@NotNull AsyncActionStatus status) {
        isAddingUserToBlockList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                ProfileModel meProfile = ProfileModel.getMeProfileModel();
                if (meProfile != null) {
                    isBlocked = false;
                    NeverListResultContainer.NeverListResult neverList = meProfile.getNeverListData();
                    if (neverList != null) {
                        isBlocked = neverList.contains(model.getXuid());
                    }
                    isFollowing = false;
                    break;
                }
                break;
            case FAIL:
            case NO_OP_FAIL:
                showError(R.string.Messages_Error_FailedToBlockUser);
                break;
        }
        updateAdapter();
    }

    public void onRemoveUserFromBlockListCompleted(@NotNull AsyncActionStatus status) {
        isRemovingUserFromBlockList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                ProfileModel meProfile = ProfileModel.getMeProfileModel();
                if (meProfile != null) {
                    isBlocked = false;
                    NeverListResultContainer.NeverListResult neverList = meProfile.getNeverListData();
                    if (neverList != null) {
                        isBlocked = neverList.contains(model.getXuid());
                        break;
                    }
                }
                break;
            case FAIL:
            case NO_OP_FAIL:
                showError(R.string.Messages_Error_FailedToUnblockUser);
                break;
        }
        updateAdapter();
    }

    public void onAddUserToMutedListCompleted(@NotNull AsyncActionStatus status) {
        isAddingUserToMutedList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                if (ProfileModel.getMeProfileModel() != null) {
                    isMuted = true;
                    break;
                }
                break;
            case FAIL:
            case NO_OP_FAIL:
                showError(R.string.Messages_Error_FailedToMuteUser);
                break;
        }
        updateAdapter();
    }

    public void onRemoveUserFromMutedListCompleted(@NotNull AsyncActionStatus status) {
        isRemovingUserFromMutedList = false;
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                if (ProfileModel.getMeProfileModel() != null) {
                    isMuted = false;
                    break;
                }
                break;
            case FAIL:
            case NO_OP_FAIL:
                showError(R.string.Messages_Error_FailedToUnmuteUser);
                break;
        }
        updateAdapter();
    }

    public enum ChangeFriendshipFormOptions {
        ShouldAddUserToFriendList,
        ShouldRemoveUserFromFriendList,
        ShouldAddUserToFavoriteList,
        ShouldRemoveUserFromFavoriteList,
        ShouldAddUserToShareIdentityList,
        ShouldRemoveUserFromShareIdentityList
    }

    private class LoadUserProfileAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final ProfileModel model;

        private LoadUserProfileAsyncTask(ProfileModel profileModel) {
            this.model = profileModel;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return this.model.shouldRefresh() || this.model.shouldRefreshProfileSummary();
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ProfileScreenViewModel.this.onLoadUserProfileCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isLoadingUserProfile = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onLoadUserProfileCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            XLEAssert.assertNotNull(this.model);
            AsyncActionStatus status = this.model.loadSync(this.forceLoad).getStatus();
            if (status == AsyncActionStatus.SUCCESS || status == AsyncActionStatus.NO_CHANGE || status == AsyncActionStatus.NO_OP_SUCCESS) {
                return this.model.loadProfileSummary(this.forceLoad).getStatus();
            }
            return status;
        }
    }

    private class LoadUserNeverListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final ProfileModel model;

        private LoadUserNeverListAsyncTask(ProfileModel profileModel) {
            this.model = profileModel;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return this.model.shouldRefresh();
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ProfileScreenViewModel.this.onLoadUserProfileCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isLoadingUserNeverList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onLoadUserNeverListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            XLEAssert.assertNotNull(this.model);
            AsyncActionStatus status = this.model.loadSync(this.forceLoad).getStatus();
            if (status == AsyncActionStatus.SUCCESS || status == AsyncActionStatus.NO_CHANGE || status == AsyncActionStatus.NO_OP_SUCCESS) {
                return this.model.loadUserNeverList(true).getStatus();
            }
            return status;
        }
    }

    private class LoadUserMutedListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final ProfileModel model;

        private LoadUserMutedListAsyncTask(ProfileModel profileModel) {
            this.model = profileModel;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return this.model.shouldRefresh();
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ProfileScreenViewModel.this.onLoadUserProfileCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isLoadingUserMutedList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onLoadUserMutedListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            XLEAssert.assertNotNull(this.model);
            AsyncActionStatus status = this.model.loadSync(this.forceLoad).getStatus();
            if (status == AsyncActionStatus.SUCCESS || status == AsyncActionStatus.NO_CHANGE || status == AsyncActionStatus.NO_OP_SUCCESS) {
                return this.model.loadUserMutedList(true).getStatus();
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
            ProfileScreenViewModel.this.onAddUserToFollowingListCompleted(AsyncActionStatus.NO_CHANGE, this.isFollowingUser);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isAddingUserToFollowingList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onAddUserToFollowingListCompleted(asyncActionStatus, this.isFollowingUser);
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
                ProfileScreenViewModel.this.model.loadProfileSummary(true);
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
            boolean unused = ProfileScreenViewModel.this.isAddingUserToShareIdentityList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onAddUseToShareIdentityListCompleted(asyncActionStatus);
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

    private class AddUserToNeverListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String blockUserXuid;

        public AddUserToNeverListAsyncTask(String str) {
            this.blockUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ProfileScreenViewModel.this.onAddUserToBlockListCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isAddingUserToBlockList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onAddUserToBlockListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel != null) {
                return meProfileModel.addUserToNeverList(this.forceLoad, this.blockUserXuid).getStatus();
            }
            return AsyncActionStatus.FAIL;
        }
    }

    private class RemoveUserToNeverListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String unblockUserXuid;

        public RemoveUserToNeverListAsyncTask(String str) {
            this.unblockUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ProfileScreenViewModel.this.onRemoveUserFromBlockListCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isRemovingUserFromBlockList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onRemoveUserFromBlockListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel != null) {
                return meProfileModel.removeUserFromNeverList(this.forceLoad, this.unblockUserXuid).getStatus();
            }
            return AsyncActionStatus.FAIL;
        }
    }

    private class AddUserToMutedListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String mutedUserXuid;

        public AddUserToMutedListAsyncTask(String str) {
            this.mutedUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ProfileScreenViewModel.this.onAddUserToMutedListCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isAddingUserToMutedList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onAddUserToMutedListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel != null) {
                return meProfileModel.addUserToMutedList(this.forceLoad, this.mutedUserXuid).getStatus();
            }
            return AsyncActionStatus.FAIL;
        }
    }

    private class RemoveUserFromMutedListAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final String mutedUserXuid;

        public RemoveUserFromMutedListAsyncTask(String str) {
            this.mutedUserXuid = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ProfileScreenViewModel.this.onRemoveUserFromMutedListCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ProfileScreenViewModel.this.isRemovingUserFromMutedList = true;
            ProfileScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ProfileScreenViewModel.this.onRemoveUserFromMutedListCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            ProfileModel meProfileModel = ProfileModel.getMeProfileModel();
            if (meProfileModel != null) {
                return meProfileModel.removeUserFromMutedList(this.forceLoad, this.mutedUserXuid).getStatus();
            }
            return AsyncActionStatus.FAIL;
        }
    }
}
