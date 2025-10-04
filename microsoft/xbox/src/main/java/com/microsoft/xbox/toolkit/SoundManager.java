package com.microsoft.xbox.toolkit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.microsoft.xboxtcui.XboxTcuiSdk;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class SoundManager {
    private static final int MAX_STREAM_SIZE = 14;
    private static final int NO_LOOP = 0;
    private final AudioManager audioManager;
    private final Context context;
    private final ArrayList<Integer> recentlyPlayedResourceIds;
    private final HashMap<Integer, Integer> resourceSoundIdMap;
    private final SoundPool soundPool;
    private boolean isEnabled;

    @SuppressLint("WrongConstant")
    private SoundManager() {
        this.resourceSoundIdMap = new HashMap<>();
        this.recentlyPlayedResourceIds = new ArrayList<>();
        this.isEnabled = false;
        XLEAssert.assertTrue("You must access sound manager on UI thread.", Thread.currentThread() == ThreadManager.UIThread);
        this.context = XboxTcuiSdk.getApplicationContext();
        this.soundPool = new SoundPool(14, 3, 0);
        this.audioManager = (AudioManager) this.context.getSystemService("audio");
    }

    public static SoundManager getInstance() {
        return SoundManagerHolder.instance;
    }

    public void clearMostRecentlyPlayedResourceIds() {
    }

    public Integer[] getMostRecentlyPlayedResourceIds() {
        return new Integer[0];
    }

    public void setEnabled(boolean z) {
        if (this.isEnabled != z) {
            this.isEnabled = z;
        }
    }

    public void loadSound(int i) {
        if (!this.resourceSoundIdMap.containsKey(Integer.valueOf(i))) {
            this.resourceSoundIdMap.put(Integer.valueOf(i), Integer.valueOf(this.soundPool.load(this.context, i, 1)));
        }
    }

    public void playSound(int i) {
        int i2;
        if (this.isEnabled) {
            if (!this.resourceSoundIdMap.containsKey(Integer.valueOf(i))) {
                i2 = this.soundPool.load(this.context, i, 1);
                this.resourceSoundIdMap.put(Integer.valueOf(i), Integer.valueOf(i2));
            } else {
                i2 = this.resourceSoundIdMap.get(Integer.valueOf(i)).intValue();
            }
            int i3 = i2;
            float streamVolume = ((float) this.audioManager.getStreamVolume(3)) / ((float) this.audioManager.getStreamMaxVolume(3));
            this.soundPool.play(i3, streamVolume, streamVolume, 1, 0, 1.0f);
        }
    }

    private static class SoundManagerHolder {
        public static final SoundManager instance = new SoundManager();

        private SoundManagerHolder() {
        }
    }
}
