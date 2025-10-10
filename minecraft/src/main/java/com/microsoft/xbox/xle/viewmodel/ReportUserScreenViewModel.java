package com.microsoft.xbox.xle.viewmodel;

import com.mojang.minecraftpe.R;
import com.microsoft.xbox.service.model.ProfileModel;
import com.microsoft.xbox.service.model.sls.FeedbackType;
import com.microsoft.xbox.telemetry.helpers.UTCPageView;
import com.microsoft.xbox.toolkit.AsyncActionStatus;
import com.microsoft.xbox.toolkit.DialogManager;
import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.NetworkAsyncTask;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLEException;
import com.microsoft.xbox.toolkit.ui.NavigationManager;
import com.microsoft.xbox.toolkit.ui.ScreenLayout;
import com.microsoft.xbox.xle.app.adapter.ReportUserScreenAdapter;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class ReportUserScreenViewModel extends ViewModelBase {
    private final FeedbackType[] feedbackReasons;
    private final ProfileModel model;
    public boolean isSubmittingReport;
    private FeedbackType selectedReason;
    private SubmitReportAsyncTask submitReportAsyncTask;

    public ReportUserScreenViewModel(ScreenLayout screenLayout) {
        super(screenLayout);
        String selectedProfile = NavigationManager.getInstance().getActivityParameters().getSelectedProfile();
        XLEAssert.assertTrue(!JavaUtil.isNullOrEmpty(selectedProfile));
        if (JavaUtil.isNullOrEmpty(selectedProfile)) {
            popScreenWithXuidError();
        }
        ProfileModel profileModel = ProfileModel.getProfileModel(selectedProfile);
        this.model = profileModel;
        XLEAssert.assertTrue(!JavaUtil.isNullOrEmpty(profileModel.getGamerTag()));
        this.adapter = new ReportUserScreenAdapter(this);
        FeedbackType[] feedbackTypeArr = new FeedbackType[7];
        feedbackTypeArr[0] = FeedbackType.UserContentPersonalInfo;
        feedbackTypeArr[1] = FeedbackType.FairPlayCheater;
        feedbackTypeArr[2] = JavaUtil.isNullOrEmpty(this.model.getRealName()) ? FeedbackType.UserContentGamertag : FeedbackType.UserContentRealName;
        feedbackTypeArr[3] = FeedbackType.UserContentGamerpic;
        feedbackTypeArr[4] = FeedbackType.FairPlayQuitter;
        feedbackTypeArr[5] = FeedbackType.FairplayUnsporting;
        feedbackTypeArr[6] = FeedbackType.CommsAbusiveVoice;
        this.feedbackReasons = feedbackTypeArr;
    }

    public void load(boolean z) {
    }

    public void onRehydrate() {
    }

    public void onStartOverride() {
    }

    public boolean onBackButtonPressed() {
        UTCPageView.removePage();
        try {
            NavigationManager.getInstance().PopScreensAndReplace(1, null, false, false, false, NavigationManager.getInstance().getActivityParameters());
            return true;
        } catch (XLEException unused) {
            return false;
        }
    }

    public void onStopOverride() {
        SubmitReportAsyncTask submitReportAsyncTask2 = this.submitReportAsyncTask;
        if (submitReportAsyncTask2 != null) {
            submitReportAsyncTask2.cancel();
        }
    }

    public boolean isBusy() {
        return this.isSubmittingReport;
    }

    private void popScreenWithXuidError() {
        try {
            showError(R.string.Service_ErrorText);
            NavigationManager.getInstance().PopScreen();
        } catch (XLEException unused) {
        }
    }

    public int getPreferredColor() {
        return this.model.getPreferedColor();
    }

    public String getTitle() {
        return String.format(XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_InfoString_Android), this.model.getGamerTag());
    }

    public ArrayList<String> getReasonTitles() {
        ArrayList<String> arrayList = new ArrayList<>(this.feedbackReasons.length);
        arrayList.add(XboxTcuiSdk.getResources().getString(R.string.ProfileCard_Report_SelectReason));
        for (FeedbackType title : this.feedbackReasons) {
            arrayList.add(title.getTitle());
        }
        return arrayList;
    }

    public boolean validReasonSelected() {
        return this.selectedReason != null;
    }

    public FeedbackType getReason() {
        return this.selectedReason;
    }

    public void setReason(int i) {
        this.selectedReason = i != 0 && i + -1 < this.feedbackReasons.length ? this.feedbackReasons[i - 1] : null;
        updateAdapter();
    }

    public void submitReport(String textReason) {
        if (submitReportAsyncTask != null) {
            submitReportAsyncTask.cancel();
        }
        if (selectedReason != null) {
            submitReportAsyncTask = new SubmitReportAsyncTask(model, selectedReason, textReason);
            submitReportAsyncTask.load(true);
        }
    }

    public void onSubmitReportCompleted(@NotNull AsyncActionStatus status) {
        switch (status) {
            case SUCCESS:
            case NO_CHANGE:
            case NO_OP_SUCCESS:
                DialogManager.getInstance().showToast(R.string.ProfileCard_Report_SuccessSubtext);
                onBackButtonPressed();
                return;
            case FAIL:
            case NO_OP_FAIL:
                showError(R.string.ProfileCard_Report_Error);
                return;
            default:
                return;
        }
    }


    public String getXUID() {
        return this.model.getXuid();
    }

    private class SubmitReportAsyncTask extends NetworkAsyncTask<AsyncActionStatus> {
        private final FeedbackType feedbackType;
        private final ProfileModel model;
        private final String textReason;

        private SubmitReportAsyncTask(ProfileModel profileModel, FeedbackType feedbackType2, String str) {
            this.model = profileModel;
            this.feedbackType = feedbackType2;
            this.textReason = str;
        }

        public boolean checkShouldExecute() {
            XLEAssert.assertIsUIThread();
            return true;
        }

        public void onNoAction() {
            XLEAssert.assertIsUIThread();
            ReportUserScreenViewModel.this.onSubmitReportCompleted(AsyncActionStatus.NO_CHANGE);
        }

        public void onPreExecute() {
            XLEAssert.assertIsUIThread();
            boolean unused = ReportUserScreenViewModel.this.isSubmittingReport = true;
            ReportUserScreenViewModel.this.updateAdapter();
        }

        public void onPostExecute(AsyncActionStatus asyncActionStatus) {
            ReportUserScreenViewModel.this.onSubmitReportCompleted(asyncActionStatus);
        }

        public AsyncActionStatus onError() {
            return AsyncActionStatus.FAIL;
        }

        public AsyncActionStatus loadDataInBackground() {
            XLEAssert.assertNotNull(this.model);
            return this.model.submitFeedbackForUser(this.forceLoad, this.feedbackType, this.textReason).getStatus();
        }
    }
}
