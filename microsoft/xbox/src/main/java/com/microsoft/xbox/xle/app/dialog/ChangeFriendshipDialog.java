package com.microsoft.xbox.xle.app.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.xboxtcui.R;
import com.microsoft.xbox.telemetry.helpers.UTCChangeRelationship;
import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.XLEManagedDialog;
import com.microsoft.xbox.toolkit.network.ListState;
import com.microsoft.xbox.toolkit.ui.CustomTypefaceTextView;
import com.microsoft.xbox.toolkit.ui.FastProgressBar;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.toolkit.ui.SwitchPanel;
import com.microsoft.xbox.toolkit.ui.XLEButton;
import com.microsoft.xbox.toolkit.ui.XLECheckBox;
import com.microsoft.xbox.toolkit.ui.XLEClickableLayout;
import com.microsoft.xbox.toolkit.ui.XLEUniversalImageView;
import com.microsoft.xbox.xle.app.ImageUtil;
import com.microsoft.xbox.xle.app.SGProjectSpecificDialogManager;
import com.microsoft.xbox.xle.app.XLEUtil;
import com.microsoft.xbox.xle.viewmodel.ChangeFriendshipDialogViewModel;
import com.microsoft.xbox.xle.viewmodel.ViewModelBase;
import com.microsoft.xboxtcui.XboxTcuiSdk;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ChangeFriendshipDialog extends XLEManagedDialog {
    private final ViewModelBase previousVM;
    public SwitchPanel changeFriendshipSwitchPanel;
    public ChangeFriendshipDialogViewModel vm;
    private RadioButton addFavorite;
    private RadioButton addFriend;
    private XLEButton cancelButton;
    private XLEButton confirmButton;
    private CustomTypefaceTextView favoriteIconView;
    private CustomTypefaceTextView gamertag;
    private FastProgressBar overlayLoadingIndicator;
    private TextView profileAccountTier;
    private CustomTypefaceTextView profileGamerScore;
    private XLEUniversalImageView profilePic;
    private CustomTypefaceTextView realName;
    private XLEClickableLayout removeFriendLayout;
    private XLECheckBox shareRealNameCheckbox;

    public ChangeFriendshipDialog(Context context, ChangeFriendshipDialogViewModel changeFriendshipDialogViewModel, ViewModelBase viewModelBase) {
        super(context, R.style.TcuiDialog);
        this.previousVM = viewModelBase;
        this.vm = changeFriendshipDialogViewModel;
    }

    public String getActivityName() {
        return "ChangeRelationship Info";
    }

    public void onStop() {
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        getWindow().setLayout(-1, -1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.change_friendship_dialog);
        this.profilePic = findViewById(R.id.change_friendship_profile_pic);
        this.gamertag = findViewById(R.id.gamertag_text);
        this.realName = findViewById(R.id.realname_text);
        this.profileAccountTier = findViewById(R.id.peoplehub_info_gamerscore_icon);
        this.profileGamerScore = findViewById(R.id.peoplehub_info_gamerscore);
        this.addFriend = findViewById(R.id.add_as_friend);
        this.addFavorite = findViewById(R.id.add_as_favorite);
        this.shareRealNameCheckbox = findViewById(R.id.share_real_name_checkbox);
        this.confirmButton = findViewById(R.id.submit_button);
        this.cancelButton = findViewById(R.id.cancel_button);
        this.changeFriendshipSwitchPanel = findViewById(R.id.change_friendship_switch_panel);
        this.removeFriendLayout = findViewById(R.id.remove_friend_btn_layout);
        this.favoriteIconView = findViewById(R.id.people_favorites_icon);
        this.overlayLoadingIndicator = findViewById(R.id.overlay_loading_indicator);
        FrameLayout frameLayout = new FrameLayout(getContext());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 5;
        XLEButton xLEButton = new XLEButton(getContext());
        xLEButton.setPadding(60, 0, 0, 0);
        xLEButton.setBackgroundResource(R.drawable.common_button_background);
        xLEButton.setText(R.string.ic_Close);
        xLEButton.setTextColor(-1);
        xLEButton.setTextSize(2, 14.0f);
        xLEButton.setTypeFace("fonts/SegXboxSymbol.ttf");
        xLEButton.setContentDescription(getContext().getResources().getString(R.string.TextInput_Confirm));
        xLEButton.setOnClickListener(view -> {
            try {
                ChangeFriendshipDialog.this.dismiss();
                NavigationManager.getInstance().PopAllScreens();
            } catch (XLEException unused) {
            }
        });
        xLEButton.setOnKeyListener((view, i, keyEvent) -> {
            if (i != 4 || keyEvent.getAction() != 1) {
                return false;
            }
            ChangeFriendshipDialog.this.dismiss();
            return true;
        });
        frameLayout.addView(xLEButton);
        addContentView(frameLayout, layoutParams);
    }

    public void onStart() {
        this.vm.load();
        updateView();
        this.changeFriendshipSwitchPanel.setBackgroundColor(this.vm.getPreferredColor());
        UTCChangeRelationship.trackChangeRelationshipView(getActivityName(), this.vm.getXuid());
    }

    @SuppressLint("WrongConstant")
    public void updateView() {
        if (this.vm.getViewModelState() == ListState.ValidContentState) {
            setDialogValidContentView();
            XLEUtil.updateAndShowTextViewUnlessEmpty(this.gamertag, this.vm.getGamerTag());
            XLEUniversalImageView xLEUniversalImageView = this.profilePic;
            if (xLEUniversalImageView != null) {
                xLEUniversalImageView.setImageURI2(ImageUtil.getMedium(this.vm.getGamerPicUrl()), R.drawable.gamerpic_missing, R.drawable.gamerpic_missing);
            }
            XLEUtil.updateAndShowTextViewUnlessEmpty(this.realName, this.vm.getRealName());
            XLEUtil.updateVisibilityIfNotNull(this.favoriteIconView, this.vm.getIsFavorite() ? 0 : 4);
            if (this.vm.getIsFavorite()) {
                this.favoriteIconView.setTextColor(getContext().getResources().getColor(R.color.XboxGreen));
            }
            String gamerScore = this.vm.getGamerScore();
            if (gamerScore != null && !gamerScore.equalsIgnoreCase("0")) {
                XLEUtil.updateAndShowTextViewUnlessEmpty(this.profileGamerScore, this.vm.getGamerScore());
                XLEUtil.updateVisibilityIfNotNull(this.profileAccountTier, 0);
            }
            if (this.addFriend != null) {
                if (this.vm.getIsFollowing()) {
                    this.addFriend.setChecked(true);
                } else {
                    this.vm.setShouldAddUserToFriendList(true);
                    this.addFriend.setChecked(true);
                }
                this.addFriend.setOnClickListener(view -> {
                    if (!vm.getIsFollowing()) {
                        vm.setShouldAddUserToFriendList(true);
                    }
                    if (vm.getIsFavorite()) {
                        vm.setShouldRemoveUserFromFavoriteList(true);
                    }
                    vm.setShouldAddUserToFavoriteList(false);
                });
            }
            if (this.addFavorite != null) {
                if (this.vm.getIsFavorite()) {
                    this.addFavorite.setChecked(true);
                }
                this.addFavorite.setOnClickListener(view -> {
                    if (!vm.getIsFavorite()) {
                        vm.setShouldAddUserToFavoriteList(true);
                    }
                    vm.setShouldRemoveUserFromFavoriteList(false);
                });
            }
            XLEButton xLEButton = this.confirmButton;
            if (xLEButton != null) {
                xLEButton.setOnClickListener(view -> {
                    changeFriendshipSwitchPanel.setState(1);
                    vm.onChangeRelationshipCompleted();
                });
            }
            XLEButton xLEButton2 = this.cancelButton;
            if (xLEButton2 != null) {
                xLEButton2.setOnClickListener(view -> {
                    dismissSelf();
                    vm.clearChangeFriendshipForm();
                });
            }
            XLECheckBox xLECheckBox = this.shareRealNameCheckbox;
            if (xLECheckBox != null) {
                xLECheckBox.setChecked(this.vm.getCallerMarkedTargetAsIdentityShared());
                this.shareRealNameCheckbox.setOnCheckedChangeListener((compoundButton, z) -> {
                    vm.setIsSharingRealNameEnd(z);
                    if (z) {
                        if (!vm.getCallerMarkedTargetAsIdentityShared()) {
                            vm.setShouldAddUserToShareIdentityList(true);
                        }
                        vm.setShouldRemoveUserFroShareIdentityList(false);
                        return;
                    }
                    if (vm.getCallerMarkedTargetAsIdentityShared()) {
                        vm.setShouldRemoveUserFroShareIdentityList(true);
                    }
                    vm.setShouldAddUserToShareIdentityList(false);
                });
                updateShareIdentityCheckboxStatus();
            }
            if (this.removeFriendLayout != null) {
                if (this.vm.getIsFollowing()) {
                    this.removeFriendLayout.setVisibility(0);
                    this.removeFriendLayout.setOnClickListener(view -> {
                        UTCChangeRelationship.trackChangeRelationshipRemoveFriend();
                        changeFriendshipSwitchPanel.setState(1);
                        vm.removeFollowingUser();
                    });
                } else {
                    this.removeFriendLayout.setEnabled(false);
                    this.removeFriendLayout.setVisibility(8);
                }
                this.confirmButton.setText(this.vm.getDialogButtonText());
            }
            updateShareIdentityCheckboxStatus();
        } else if (this.vm.getViewModelState() == ListState.LoadingState) {
            setDialogLoadingView();
        }
    }

    private void setDialogValidContentView() {
        XLEUtil.updateVisibilityIfNotNull(this.overlayLoadingIndicator, 8);
        XLEButton xLEButton = this.confirmButton;
        if (xLEButton != null) {
            xLEButton.setEnabled(true);
        }
        XLEButton xLEButton2 = this.cancelButton;
        if (xLEButton2 != null) {
            xLEButton2.setEnabled(true);
        }
    }

    private void setDialogLoadingView() {
        XLEUtil.updateVisibilityIfNotNull(this.overlayLoadingIndicator, 0);
        XLEButton xLEButton = this.confirmButton;
        if (xLEButton != null) {
            xLEButton.setEnabled(false);
        }
        XLEButton xLEButton2 = this.cancelButton;
        if (xLEButton2 != null) {
            xLEButton2.setEnabled(false);
        }
    }

    public void closeDialog() {
        dismissSelf();
        this.previousVM.load(true);
    }

    public void onBackPressed() {
        dismissSelf();
    }

    public void dismissSelf() {
        ((SGProjectSpecificDialogManager) DialogManager.getInstance().getManager()).dismissChangeFriendshipDialog();
    }

    public void setVm(ChangeFriendshipDialogViewModel changeFriendshipDialogViewModel) {
        this.vm = changeFriendshipDialogViewModel;
    }

    @SuppressLint("WrongConstant")
    public void updateShareIdentityCheckboxStatus() {
        String callerShareRealNameStatus = this.vm.getCallerShareRealNameStatus();
        if (callerShareRealNameStatus != null) {
            boolean equalsIgnoreCase = callerShareRealNameStatus.equalsIgnoreCase("Blocked");
            this.shareRealNameCheckbox.setVisibility(equalsIgnoreCase ? 8 : 0);
            if (!equalsIgnoreCase) {
                boolean z = !JavaUtil.isNullOrEmpty(this.vm.getRealName());
                if (callerShareRealNameStatus.compareToIgnoreCase("Everyone") == 0) {
                    this.shareRealNameCheckbox.setChecked(true);
                    this.vm.setInitialRealNameSharingState(true);
                    this.shareRealNameCheckbox.setEnabled(false);
                    this.shareRealNameCheckbox.setSubText(XboxTcuiSdk.getResources().getString(R.string.ChangeRelationship_Checkbox_Subtext_ShareRealName_Everyone));
                }
                if (callerShareRealNameStatus.compareToIgnoreCase("PeopleOnMyList") == 0) {
                    this.shareRealNameCheckbox.setChecked(true);
                    this.vm.setInitialRealNameSharingState(true);
                    this.shareRealNameCheckbox.setEnabled(false);
                    this.shareRealNameCheckbox.setSubText(XboxTcuiSdk.getResources().getString(R.string.ChangeRelationship_Checkbox_Subtext_ShareRealName_Friends));
                }
                if (callerShareRealNameStatus.compareToIgnoreCase("FriendCategoryShareIdentity") == 0) {
                    if (this.vm.getIsFollowing()) {
                        if (this.vm.getCallerMarkedTargetAsIdentityShared()) {
                            this.shareRealNameCheckbox.setChecked(true);
                            this.vm.setInitialRealNameSharingState(true);
                        } else {
                            this.shareRealNameCheckbox.setChecked(false);
                            this.vm.setInitialRealNameSharingState(false);
                        }
                    } else if (z) {
                        this.shareRealNameCheckbox.setChecked(true);
                        this.vm.setInitialRealNameSharingState(true);
                        this.vm.setShouldAddUserToShareIdentityList(true);
                    } else {
                        this.shareRealNameCheckbox.setChecked(false);
                        this.vm.setInitialRealNameSharingState(false);
                    }
                    this.shareRealNameCheckbox.setSubText(String.format(XboxTcuiSdk.getResources().getString(R.string.ChangeRelationship_Checkbox_Subtext_ShareRealName), this.vm.getGamerTag()));
                    this.shareRealNameCheckbox.setEnabled(true);
                }
            }
        }
    }

    public void reportAsyncTaskCompleted() {
        if (!this.vm.isBusy() && this.changeFriendshipSwitchPanel.getState() == 1) {
            closeDialog();
        }
    }

    @SuppressLint("WrongConstant")
    public void reportAsyncTaskFailed(String str) {
        if (this.changeFriendshipSwitchPanel.getState() == 1) {
            this.changeFriendshipSwitchPanel.setState(0);
            Toast.makeText(XboxTcuiSdk.getActivity(), str, 0).show();
        }
        updateView();
    }
}
