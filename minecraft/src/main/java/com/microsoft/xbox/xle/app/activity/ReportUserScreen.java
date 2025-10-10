package com.microsoft.xbox.xle.app.activity;

import com.mojang.minecraftpe.R;
import com.microsoft.xbox.telemetry.helpers.UTCReportUser;
import com.microsoft.xbox.xle.viewmodel.ReportUserScreenViewModel;


public class ReportUserScreen extends ActivityBase {
    private ReportUserScreenViewModel reportUserScreenViewModel;

    public String getActivityName() {
        return "Report user";
    }

    public void onCreate() {
        super.onCreate();
        onCreateContentView();
        this.viewModel = new ReportUserScreenViewModel(this);
        this.reportUserScreenViewModel = (ReportUserScreenViewModel) this.viewModel;
        UTCReportUser.trackReportView(getName(), this.reportUserScreenViewModel.getXUID());
    }

    public void onStart() {
        super.onStart();
        setBackgroundColor(this.reportUserScreenViewModel.getPreferredColor());
    }

    public void onCreateContentView() {
        setContentView(R.layout.report_user_screen);
    }
}
