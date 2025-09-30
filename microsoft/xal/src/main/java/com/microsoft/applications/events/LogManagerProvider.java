package com.microsoft.applications.events;

import java.util.Date;
import java.util.UUID;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class LogManagerProvider {
    protected static native long nativeCreateLogManager(ILogConfiguration iLogConfiguration);

    public static ILogManager createLogManager(ILogConfiguration iLogConfiguration) {
        return new LogManagerImpl(nativeCreateLogManager(iLogConfiguration));
    }

    static class LogManagerImpl implements ILogManager {
        long nativeLogManager;

        private native long nativeGetLogger(String str, String str2, String str3);

        private native boolean nativeRegisterPrivacyGuard(long j);

        private native boolean nativeUnregisterPrivacyGuard(long j);

        public native long nativeAddEventListener(long j, long j2, DebugEventListener debugEventListener, long j3);

        protected native void nativeClose(long j);

        protected native void nativeDisableViewer(long j);

        protected native int nativeFlush(long j);

        protected native void nativeFlushAndTeardown(long j);

        protected native String nativeGetCurrentEndpoint(long j);

        protected native ILogConfiguration nativeGetLogConfigurationCopy(long j);

        protected native void nativeGetLogSessionData(long j, LogSessionDataImpl logSessionDataImpl);

        protected native long nativeGetSemanticContext(long j);

        protected native String nativeGetTransmitProfileName(long j);

        protected native boolean nativeInitializeDDV(long j, String str, String str2);

        protected native boolean nativeIsViewerEnabled(long j);

        protected native int nativeLoadTransmitProfiles(long j, String str);

        protected native int nativePauseTransmission(long j);

        public native void nativeRemoveEventListener(long j, long j2, long j3);

        protected native int nativeResetTransmitProfiles(long j);

        protected native int nativeResumeTransmission(long j);

        protected native int nativeSetContextBoolean(long j, String str, boolean z, int i);

        protected native int nativeSetContextDate(long j, String str, Date date, int i);

        protected native int nativeSetContextDouble(long j, String str, double d, int i);

        protected native int nativeSetContextInt(long j, String str, int i, int i2);

        protected native int nativeSetContextLong(long j, String str, long j2, int i);

        protected native int nativeSetContextString(long j, String str, String str2, int i);

        protected native int nativeSetContextUUID(long j, String str, String str2, int i);

        protected native void nativeSetLevelFilter(long j, int i, int[] iArr);

        protected native int nativeSetTransmitProfileS(long j, String str);

        protected native int nativeSetTransmitProfileTP(long j, int i);

        protected native int nativeUploadNow(long j);

        private LogManagerImpl() {
            this.nativeLogManager = 0L;
        }

        LogManagerImpl(long j) {
            this.nativeLogManager = j;
        }

        @Override
        public ILogger getLogger(String str, String str2, String str3) {
            long jNativeGetLogger = nativeGetLogger(str, str2, str3);
            if (jNativeGetLogger == 0) {
                throw new NullPointerException("Null native logger pointer");
            }
            return new Logger(jNativeGetLogger);
        }

        @Override
        public ILogConfiguration getLogConfigurationCopy() {
            return nativeGetLogConfigurationCopy(this.nativeLogManager);
        }

        @Override
        public void close() {
            nativeClose(this.nativeLogManager);
            this.nativeLogManager = -1L;
        }

        @Override
        public void flushAndTeardown() {
            nativeFlushAndTeardown(this.nativeLogManager);
        }

        @Override
        public Status flush() {
            return Status.getEnum(nativeFlush(this.nativeLogManager));
        }

        @Override
        public Status uploadNow() {
            return Status.getEnum(nativeUploadNow(this.nativeLogManager));
        }

        @Override
        public Status pauseTransmission() {
            return Status.getEnum(nativePauseTransmission(this.nativeLogManager));
        }

        @Override
        public Status resumeTransmission() {
            return Status.getEnum(nativeResumeTransmission(this.nativeLogManager));
        }

        @Override
        public Status setTransmitProfile(TransmitProfile transmitProfile) {
            return Status.getEnum(nativeSetTransmitProfileTP(this.nativeLogManager, transmitProfile.getValue()));
        }

        @Override
        public Status setTransmitProfile(String str) {
            return Status.getEnum(nativeSetTransmitProfileS(this.nativeLogManager, str));
        }

        @Override
        public Status loadTransmitProfiles(String str) {
            return Status.getEnum(nativeLoadTransmitProfiles(this.nativeLogManager, str));
        }

        @Override
        public Status resetTransmitProfiles() {
            return Status.getEnum(nativeResetTransmitProfiles(this.nativeLogManager));
        }

        @Override
        public String getTransmitProfileName() {
            return nativeGetTransmitProfileName(this.nativeLogManager);
        }

        @Override
        public ISemanticContext getSemanticContext() {
            return new SemanticContext(nativeGetSemanticContext(this.nativeLogManager));
        }

        @Override
        public Status setContext(String str, String str2, PiiKind piiKind) {
            return Status.getEnum(nativeSetContextString(this.nativeLogManager, str, str2, piiKind.getValue()));
        }

        @Override
        public Status setContext(String str, int i, PiiKind piiKind) {
            return Status.getEnum(nativeSetContextInt(this.nativeLogManager, str, i, piiKind.getValue()));
        }

        @Override
        public Status setContext(String str, long j, PiiKind piiKind) {
            return Status.getEnum(nativeSetContextLong(this.nativeLogManager, str, j, piiKind.getValue()));
        }

        @Override
        public Status setContext(String str, double d, PiiKind piiKind) {
            return Status.getEnum(nativeSetContextDouble(this.nativeLogManager, str, d, piiKind.getValue()));
        }

        @Override
        public Status setContext(String str, boolean z, PiiKind piiKind) {
            return Status.getEnum(nativeSetContextBoolean(this.nativeLogManager, str, z, piiKind.getValue()));
        }

        @Override
        public Status setContext(String str, Date date, PiiKind piiKind) {
            return Status.getEnum(nativeSetContextDate(this.nativeLogManager, str, date, piiKind.getValue()));
        }

        @Override
        public Status setContext(String str, UUID uuid, PiiKind piiKind) {
            return Status.getEnum(nativeSetContextUUID(this.nativeLogManager, str, uuid.toString(), piiKind.getValue()));
        }

        @Override
        public boolean initializeDiagnosticDataViewer(String str, String str2) {
            return nativeInitializeDDV(this.nativeLogManager, str, str2);
        }

        @Override
        public void disableViewer() {
            nativeDisableViewer(this.nativeLogManager);
        }

        @Override
        public boolean isViewerEnabled() {
            return nativeIsViewerEnabled(this.nativeLogManager);
        }

        @Override
        public String getCurrentEndpoint() {
            return nativeGetCurrentEndpoint(this.nativeLogManager);
        }

        protected static class LogSessionDataImpl implements LogSessionData {
            private long m_first_time = 0;
            private String m_uuid = null;

            @Override
            public long getSessionFirstTime() {
                return this.m_first_time;
            }

            @Override
            public String getSessionSDKUid() {
                return this.m_uuid;
            }
        }

        @Override
        public LogSessionData getLogSessionData() {
            LogSessionDataImpl logSessionDataImpl = new LogSessionDataImpl();
            nativeGetLogSessionData(this.nativeLogManager, logSessionDataImpl);
            return logSessionDataImpl;
        }

        @Override
        public void setLevelFilter(int i, int[] iArr) {
            nativeSetLevelFilter(this.nativeLogManager, i, iArr);
        }

        @Override
        public void addEventListener(DebugEventType debugEventType, DebugEventListener debugEventListener) {
            debugEventListener.nativeIdentity = nativeAddEventListener(this.nativeLogManager, debugEventType.value(), debugEventListener, debugEventListener.nativeIdentity);
        }

        @Override
        public void removeEventListener(DebugEventType debugEventType, DebugEventListener debugEventListener) {
            nativeRemoveEventListener(this.nativeLogManager, debugEventType.value(), debugEventListener.nativeIdentity);
        }

        @Override
        public boolean registerPrivacyGuard() {
            return PrivacyGuard.isInitialized() && nativeRegisterPrivacyGuard(this.nativeLogManager);
        }

        @Override
        public boolean unregisterPrivacyGuard() {
            return PrivacyGuard.isInitialized() && nativeUnregisterPrivacyGuard(this.nativeLogManager);
        }
    }
}
