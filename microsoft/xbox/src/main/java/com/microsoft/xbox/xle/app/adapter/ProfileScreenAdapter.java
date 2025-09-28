package com.microsoft.xbox.xle.app.adapter;

import android.annotation.SuppressLint;
import android.widget.ScrollView;

import com.microsoft.xboxtcui.R;
import com.microsoft.xbox.telemetry.helpers.UTCPeopleHub;
import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.ui.CustomTypefaceTextView;
import com.microsoft.xbox.toolkit.ui.FastProgressBar;
import com.microsoft.xbox.toolkit.ui.XLERoundedUniversalImageView;
import com.microsoft.xbox.xle.app.ImageUtil;
import com.microsoft.xbox.xle.app.XLEUtil;
import com.microsoft.xbox.xle.app.activity.Profile.ProfileScreenViewModel;
import com.microsoft.xbox.xle.ui.IconFontToggleButton;
import com.microsoft.xbox.xle.ui.XLERootView;
import com.microsoft.xbox.xle.viewmodel.AdapterBase;
import com.microsoft.xboxtcui.XboxAppDeepLinker;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ProfileScreenAdapter extends AdapterBase {
    private final ScrollView contentScrollView = ((ScrollView) findViewById(R.id.profile_screen_content_list));
    private final IconFontToggleButton followButton = ((IconFontToggleButton) findViewById(R.id.profile_follow));
    private final XLERoundedUniversalImageView gamerPicImageView = ((XLERoundedUniversalImageView) findViewById(R.id.profile_gamerpic));
    private final CustomTypefaceTextView gamerscoreIconTextView = ((CustomTypefaceTextView) findViewById(R.id.profile_gamerscore_icon));
    private final CustomTypefaceTextView gamerscoreTextView = ((CustomTypefaceTextView) findViewById(R.id.profile_gamerscore));
    private final CustomTypefaceTextView gamertagTextView = ((CustomTypefaceTextView) findViewById(R.id.profile_gamertag));
    private final FastProgressBar loadingProgressBar = ((FastProgressBar) findViewById(R.id.profile_screen_loading));
    private final CustomTypefaceTextView realNameTextView = ((CustomTypefaceTextView) findViewById(R.id.profile_realname));
    private final IconFontToggleButton reportButton = ((IconFontToggleButton) findViewById(R.id.profile_report));
    private final XLERootView rootView = ((XLERootView) findViewById(R.id.profile_root));
    private final IconFontToggleButton viewInXboxAppButton = ((IconFontToggleButton) findViewById(R.id.profile_view_in_xbox_app));
    private final CustomTypefaceTextView viewInXboxAppSubTextView = ((CustomTypefaceTextView) findViewById(R.id.profile_view_in_xbox_app_subtext));
    public IconFontToggleButton blockButton = ((IconFontToggleButton) findViewById(R.id.profile_block));
    public IconFontToggleButton muteButton = ((IconFontToggleButton) findViewById(R.id.profile_mute));
    public ProfileScreenViewModel viewModel;

    @SuppressLint("WrongConstant")
    public ProfileScreenAdapter(ProfileScreenViewModel profileScreenViewModel) {
        super(profileScreenViewModel);
        this.viewModel = profileScreenViewModel;
        this.viewInXboxAppButton.setVisibility(0);
        this.viewInXboxAppButton.setEnabled(true);
        this.viewInXboxAppButton.setChecked(true);
        if (this.viewModel.isMeProfile()) {
            this.followButton.setVisibility(8);
            this.muteButton.setVisibility(8);
            this.blockButton.setVisibility(8);
            this.reportButton.setVisibility(8);
            this.viewInXboxAppSubTextView.setText(R.string.Profile_ViewInXboxApp_Details_MeProfile);
            return;
        }
        this.followButton.setVisibility(0);
        this.followButton.setEnabled(true);
        this.muteButton.setVisibility(0);
        this.muteButton.setEnabled(true);
        this.muteButton.setChecked(false);
        this.blockButton.setVisibility(0);
        this.blockButton.setEnabled(false);
        this.reportButton.setVisibility(0);
        this.reportButton.setEnabled(true);
        this.reportButton.setChecked(false);
        this.viewInXboxAppSubTextView.setText(R.string.Profile_ViewInXboxApp_Details_YouProfile);
    }

    @SuppressLint("WrongConstant")
    public void onStart() {
        super.onStart();
        IconFontToggleButton iconFontToggleButton = this.followButton;
        if (iconFontToggleButton != null) {
            iconFontToggleButton.setOnClickListener(view -> viewModel.navigateToChangeRelationship());
        }
        IconFontToggleButton iconFontToggleButton2 = this.muteButton;
        if (iconFontToggleButton2 != null) {
            iconFontToggleButton2.setOnClickListener(view -> {
                muteButton.toggle();
                muteButton.setEnabled(false);
                if (muteButton.isChecked()) {
                    UTCPeopleHub.trackMute(true);
                    viewModel.muteUser();
                    return;
                }
                UTCPeopleHub.trackMute(false);
                viewModel.unmuteUser();
            });
        }
        IconFontToggleButton iconFontToggleButton3 = this.blockButton;
        if (iconFontToggleButton3 != null) {
            iconFontToggleButton3.setOnClickListener(view -> {
                blockButton.toggle();
                blockButton.setEnabled(false);
                if (blockButton.isChecked()) {
                    UTCPeopleHub.trackBlock();
                    viewModel.blockUser();
                    return;
                }
                UTCPeopleHub.trackUnblock();
                viewModel.unblockUser();
            });
        }
        IconFontToggleButton iconFontToggleButton4 = this.reportButton;
        if (iconFontToggleButton4 != null) {
            iconFontToggleButton4.setOnClickListener(view -> {
                UTCPeopleHub.trackReport();
                viewModel.showReportDialog();
            });
        }
        if (this.viewInXboxAppButton == null) {
            return;
        }
        if (XboxAppDeepLinker.appDeeplinkingSupported()) {
            this.viewInXboxAppButton.setOnClickListener(view -> {
                UTCPeopleHub.trackViewInXboxApp();
                ProfileScreenAdapter.this.viewModel.launchXboxApp();
            });
            return;
        }
        this.viewInXboxAppButton.setVisibility(8);
        this.viewInXboxAppSubTextView.setVisibility(8);
    }

    @SuppressLint("WrongConstant")
    public void updateViewOverride() {
        XLERootView xLERootView = this.rootView;
        if (xLERootView != null) {
            xLERootView.setBackgroundColor(this.viewModel.getPreferredColor());
        }
        boolean z = false;
        this.loadingProgressBar.setVisibility(this.viewModel.isBusy() ? 0 : 8);
        this.contentScrollView.setVisibility(this.viewModel.isBusy() ? 8 : 0);
        XLERoundedUniversalImageView xLERoundedUniversalImageView = this.gamerPicImageView;
        if (xLERoundedUniversalImageView != null) {
            xLERoundedUniversalImageView.setImageURI2(ImageUtil.getMedium(this.viewModel.getGamerPicUrl()), R.drawable.gamerpic_missing, R.drawable.gamerpic_missing);
        }
        if (this.realNameTextView != null) {
            String realName = this.viewModel.getRealName();
            if (!JavaUtil.isNullOrEmpty(realName)) {
                this.realNameTextView.setText(realName);
                this.realNameTextView.setVisibility(0);
            } else {
                this.realNameTextView.setVisibility(8);
            }
        }
        if (!(this.gamerscoreTextView == null || this.gamerscoreIconTextView == null)) {
            String gamerScore = this.viewModel.getGamerScore();
            if (!JavaUtil.isNullOrEmpty(gamerScore)) {
                XLEUtil.updateTextAndVisibilityIfNotNull(this.gamerscoreTextView, gamerScore, 0);
                XLEUtil.updateVisibilityIfNotNull(this.gamerscoreIconTextView, 0);
            }
        }
        if (this.gamertagTextView != null) {
            String gamerTag = this.viewModel.getGamerTag();
            if (!JavaUtil.isNullOrEmpty(gamerTag)) {
                XLEUtil.updateTextAndVisibilityIfNotNull(this.gamertagTextView, gamerTag, 0);
            }
        }
        if (!this.viewModel.isMeProfile()) {
            boolean z2 = this.viewModel.getIsAddingUserToBlockList() || this.viewModel.getIsRemovingUserFromBlockList();
            this.followButton.setChecked(this.viewModel.isCallerFollowingTarget());
            this.followButton.setEnabled(!z2 && !this.viewModel.getIsBlocked());
            this.muteButton.setChecked(this.viewModel.getIsMuted());
            IconFontToggleButton iconFontToggleButton = this.muteButton;
            if (!this.viewModel.getIsAddingUserToMutedList() && !this.viewModel.getIsRemovingUserFromMutedList()) {
                z = true;
            }
            iconFontToggleButton.setEnabled(z);
            this.blockButton.setChecked(this.viewModel.getIsBlocked());
            this.blockButton.setEnabled(!z2);
        }
    }
}
