package org.fmod;

import android.media.AudioTrack;
import android.util.Log;

public class AudioDevice {
    private AudioTrack mTrack = null;

    private int fetchChannelConfigFromCount(int i) {
        if (i == 1) {
            return 2;
        }
        if (i == 2) {
            return 3;
        }
        if (i == 6) {
            return 252;
        }
        return i == 8 ? 6396 : 0;
    }

    public boolean init(int i, int i2, int i3, int i4) {
        int fetchChannelConfigFromCount = fetchChannelConfigFromCount(i);
        int minBufferSize = AudioTrack.getMinBufferSize(i2, fetchChannelConfigFromCount, 2);
        if (minBufferSize < 0) {
            Log.w("fmod", "AudioDevice::init : Couldn't query minimum buffer size, possibly unsupported sample rate or channel count");
        } else {
            Log.i("fmod", "AudioDevice::init : Min buffer size: " + minBufferSize + " bytes");
        }
        int i5 = i3 * i4 * i * 2;
        int i6 = Math.max(i5, minBufferSize);
        Log.i("fmod", "AudioDevice::init : Actual buffer size: " + i6 + " bytes");
        try {
            AudioTrack audioTrack = new AudioTrack(3, i2, fetchChannelConfigFromCount, 2, i6, 1);
            mTrack = audioTrack;
            try {
                audioTrack.play();
                return true;
            } catch (IllegalStateException unused) {
                Log.e("fmod", "AudioDevice::init : AudioTrack play caused IllegalStateException");
                mTrack.release();
                mTrack = null;
                return false;
            }
        } catch (IllegalArgumentException unused2) {
            Log.e("fmod", "AudioDevice::init : AudioTrack creation caused IllegalArgumentException");
            return false;
        }
    }

    public void close() {
        try {
            mTrack.stop();
        } catch (IllegalStateException unused) {
            Log.e("fmod", "AudioDevice::init : AudioTrack stop caused IllegalStateException");
        }
        mTrack.release();
        mTrack = null;
    }

    public void write(byte[] bArr, int i) {
        mTrack.write(bArr, 0, i);
    }

    public void write(short[] sArr, int i) {
        mTrack.write(sArr, 0, i);
    }
}