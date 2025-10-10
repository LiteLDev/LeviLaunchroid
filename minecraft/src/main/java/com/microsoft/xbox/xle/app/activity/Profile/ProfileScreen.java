package com.microsoft.xbox.xle.app.activity.Profile;

import android.content.Context;
import android.util.AttributeSet;

import com.mojang.minecraftpe.R;
import com.microsoft.xbox.telemetry.helpers.UTCPeopleHub;
import com.microsoft.xbox.xle.app.activity.ActivityBase;


public class ProfileScreen extends ActivityBase {
    public ProfileScreen() {
    }

    public ProfileScreen(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public String getActivityName() {
        return "PeopleHub Info";
    }

    public void onCreate() {
        super.onCreate();
        onCreateContentView();
        ProfileScreenViewModel profileScreenViewModel = new ProfileScreenViewModel(this);
        this.viewModel = profileScreenViewModel;
        UTCPeopleHub.trackPeopleHubView(getActivityName(), profileScreenViewModel.getXuid(), profileScreenViewModel.isMeProfile());
    }

    public void onCreateContentView() {
        setContentView(R.layout.profile_screen);
    }
}
