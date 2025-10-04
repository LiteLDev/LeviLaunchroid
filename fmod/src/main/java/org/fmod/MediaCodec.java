package org.fmod;

import android.annotation.SuppressLint;
import android.media.MediaCrypto;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;

public class MediaCodec {
    private long mCodecPtr = 0;
    private long mLength = 0;
    private int mSampleRate = 0;
    private int mChannelCount = 0;
    private boolean mInputFinished = false;
    private boolean mOutputFinished = false;
    private android.media.MediaCodec mDecoder = null;
    private Object mDataSourceProxy = null;
    private MediaExtractor mExtractor = null;
    private ByteBuffer[] mInputBuffers = null;
    private ByteBuffer[] mOutputBuffers = null;
    private int mCurrentOutputBufferIndex = -1;

    public static native long fmodGetSize(long j);

    public static native int fmodReadAt(long j, long j2, byte[] bArr, int i, int i2);

    public long getLength() {
        return mLength;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getChannelCount() {
        return mChannelCount;
    }

    public boolean init(long j) {
        mCodecPtr = j;
        if (Build.VERSION.SDK_INT < 17) {
            Log.w("fmod", "MediaCodec::init : MediaCodec unavailable, ensure device is running at least 4.2 (JellyBean).\n");
            return false;
        }
        if (Build.VERSION.SDK_INT < 23) {
            try {
                @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.media.DataSource");
                Method method = Class.forName("android.media.MediaExtractor").getMethod("setDataSource", cls);
                mExtractor = new MediaExtractor();
                Object newProxyInstance = Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, (obj, method2, objArr) -> {
                    if (method2.getName().equals("readAt")) {
                        return MediaCodec.fmodReadAt(mCodecPtr, (Long) objArr[0], (byte[]) objArr[1], 0, (Integer) objArr[2]);
                    }
                    if (method2.getName().equals("getSize")) {
                        return MediaCodec.fmodGetSize(mCodecPtr);
                    }
                    if (method2.getName().equals("close")) {
                        return null;
                    }
                    Log.w("fmod", "MediaCodec::DataSource::invoke : Unrecognised method found: " + method2.getName());
                    return null;
                });
                mDataSourceProxy = newProxyInstance;
                method.invoke(mExtractor, newProxyInstance);
            } catch (ClassNotFoundException e) {
                Log.w("fmod", "MediaCodec::init : " + e);
                return false;
            } catch (IllegalAccessException e2) {
                Log.e("fmod", "MediaCodec::init : " + e2);
                return false;
            } catch (NoSuchMethodException e3) {
                Log.w("fmod", "MediaCodec::init : " + e3);
                return false;
            } catch (InvocationTargetException e4) {
                Log.e("fmod", "MediaCodec::init : " + e4);
                return false;
            }
        } else {
            try {
                MediaExtractor mediaExtractor = new MediaExtractor();
                mExtractor = mediaExtractor;
                mediaExtractor.setDataSource(new MediaDataSource() {
                    @Override
                    public void close() {
                    }

                    @Override
                    public int readAt(long j2, byte[] bArr, int i, int i2) {
                        return MediaCodec.fmodReadAt(mCodecPtr, j2, bArr, i, i2);
                    }

                    @Override
                    public long getSize() {
                        return MediaCodec.fmodGetSize(mCodecPtr);
                    }
                });
            } catch (IOException e5) {
                Log.w("fmod", "MediaCodec::init : " + e5);
                return false;
            }
        }
        int trackCount = mExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat trackFormat = mExtractor.getTrackFormat(i);
            String string = trackFormat.getString("mime");
            Log.d("fmod", "MediaCodec::init : Format " + i + " / " + trackCount + " -- " + trackFormat);
            if (string.equals("audio/mp4a-latm")) {
                try {
                    mDecoder = android.media.MediaCodec.createDecoderByType(string);
                    mExtractor.selectTrack(i);
                    mDecoder.configure(trackFormat, (Surface) null, (MediaCrypto) null, 0);
                    mDecoder.start();
                    mInputBuffers = mDecoder.getInputBuffers();
                    mOutputBuffers = mDecoder.getOutputBuffers();
                    int integer = trackFormat.containsKey("encoder-delay") ? trackFormat.getInteger("encoder-delay") : 0;
                    int integer2 = trackFormat.containsKey("encoder-padding") ? trackFormat.getInteger("encoder-padding") : 0;
                    long j2 = trackFormat.getLong("durationUs");
                    mChannelCount = trackFormat.getInteger("channel-count");
                    int integer3 = trackFormat.getInteger("sample-rate");
                    mSampleRate = integer3;
                    mLength = (((int) (((j2 * integer3) + 999999) / 1000000)) - integer) - integer2;
                    return true;
                } catch (IOException e6) {
                    Log.e("fmod", "MediaCodec::init : " + e6);
                    return false;
                }
            }
        }
        return false;
    }

    public void release() {
        android.media.MediaCodec mediaCodec = mDecoder;
        if (mediaCodec != null) {
            mediaCodec.stop();
            mDecoder.release();
            mDecoder = null;
        }
        MediaExtractor mediaExtractor = mExtractor;
        if (mediaExtractor != null) {
            mediaExtractor.release();
            mExtractor = null;
        }
    }

    public int read(byte[] bArr, int i) {
        int dequeueInputBuffer;
        int i2 = (mInputFinished && mOutputFinished && mCurrentOutputBufferIndex == -1) ? -1 : 0;
        while (!mInputFinished && (dequeueInputBuffer = mDecoder.dequeueInputBuffer(0L)) >= 0) {
            int readSampleData = mExtractor.readSampleData(mInputBuffers[dequeueInputBuffer], 0);
            if (readSampleData >= 0) {
                mDecoder.queueInputBuffer(dequeueInputBuffer, 0, readSampleData, mExtractor.getSampleTime(), 0);
                mExtractor.advance();
            } else {
                mDecoder.queueInputBuffer(dequeueInputBuffer, 0, 0, 0L, 4);
                mInputFinished = true;
            }
        }
        if (!mOutputFinished && mCurrentOutputBufferIndex == -1) {
            android.media.MediaCodec.BufferInfo bufferInfo = new android.media.MediaCodec.BufferInfo();
            int dequeueOutputBuffer = mDecoder.dequeueOutputBuffer(bufferInfo, 10000L);
            if (dequeueOutputBuffer >= 0) {
                mCurrentOutputBufferIndex = dequeueOutputBuffer;
                mOutputBuffers[dequeueOutputBuffer].limit(bufferInfo.size);
                mOutputBuffers[dequeueOutputBuffer].position(bufferInfo.offset);
            } else if (dequeueOutputBuffer == -3) {
                mOutputBuffers = mDecoder.getOutputBuffers();
            } else if (dequeueOutputBuffer == -2) {
                Log.d("fmod", "MediaCodec::read : MediaCodec::dequeueOutputBuffer returned MediaCodec.INFO_OUTPUT_FORMAT_CHANGED " + mDecoder.getOutputFormat());
            } else if (dequeueOutputBuffer == -1) {
                Log.d("fmod", "MediaCodec::read : MediaCodec::dequeueOutputBuffer returned MediaCodec.INFO_TRY_AGAIN_LATER.");
            } else {
                Log.w("fmod", "MediaCodec::read : MediaCodec::dequeueOutputBuffer returned " + dequeueOutputBuffer);
            }
            if ((bufferInfo.flags & 4) != 0) {
                mOutputFinished = true;
            }
        }
        int i3 = mCurrentOutputBufferIndex;
        if (i3 != -1) {
            ByteBuffer byteBuffer = mOutputBuffers[i3];
            int min = Math.min(byteBuffer.remaining(), i);
            byteBuffer.get(bArr, 0, min);
            if (!byteBuffer.hasRemaining()) {
                byteBuffer.clear();
                mDecoder.releaseOutputBuffer(mCurrentOutputBufferIndex, false);
                mCurrentOutputBufferIndex = -1;
            }
            return min;
        }
        return i2;
    }

    public void seek(int i) {
        int i2 = mCurrentOutputBufferIndex;
        if (i2 != -1) {
            mOutputBuffers[i2].clear();
            mCurrentOutputBufferIndex = -1;
        }
        mInputFinished = false;
        mOutputFinished = false;
        mDecoder.flush();
        long j = i;
        mExtractor.seekTo((j * 1000000) / mSampleRate, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        long sampleTime = ((mExtractor.getSampleTime() * mSampleRate) + 999999) / 1000000;
        int i3 = (int) ((j - sampleTime) * mChannelCount * 2);
        if (i3 >= 0) {
            byte[] bArr = new byte[1024];
            while (i3 > 0) {
                i3 -= read(bArr, Math.min(1024, i3));
            }
            return;
        }
        Log.w("fmod", "MediaCodec::seek : Seek to " + i + " resulted in position " + sampleTime);
    }
}