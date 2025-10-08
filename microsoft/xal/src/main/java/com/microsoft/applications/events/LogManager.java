package com.microsoft.applications.events;

import java.util.Date;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class LogManager {
    static Vector<Logger> loggers = new Vector<>();

    public static native void disableViewer();

    public static native String getCurrentEndpoint();

    public static native String getTransmitProfileName();

    public static native boolean initializeDiagnosticDataViewer(String str, String str2);

    public static native boolean isViewerEnabled();

    private static native int nativeFlush();

    private static native int nativeFlushAndTeardown();

    private static native LogConfigurationImpl nativeGetLogConfiguration();

    private static native long nativeGetLogger();

    private static native long nativeGetLoggerWithSource(String str);

    private static native long nativeGetLoggerWithTenantTokenAndSource(String str, String str2);

    private static native long nativeGetSemanticContext();

    private static native long nativeInitializeConfig(String str, ILogConfiguration iLogConfiguration);

    private static native long nativeInitializeWithTenantToken(String str);

    private static native long nativeInitializeWithoutTenantToken();

    private static native int nativeLoadTransmitProfilesString(String str);

    private static native int nativePauseTransmission();

    private static native boolean nativeRegisterPrivacyGuardOnDefaultLogManager();

    private static native int nativeResetTransmitProfiles();

    private static native int nativeResumeTransmission();

    private static native int nativeSetContextBoolValue(String str, boolean z, int i);

    private static native int nativeSetContextDoubleValue(String str, double d, int i);

    private static native int nativeSetContextGuidValue(String str, String str2, int i);

    private static native int nativeSetContextIntValue(String str, int i, int i2);

    private static native int nativeSetContextLongValue(String str, long j, int i);

    private static native int nativeSetContextStringValue(String str, String str2, int i);

    private static native int nativeSetContextTimeTicksValue(String str, long j, int i);

    private static native int nativeSetIntTransmitProfile(int i);

    private static native int nativeSetTransmitProfileString(String str);

    private static native boolean nativeUnregisterPrivacyGuardOnDefaultLogManager();

    private static native int nativeUploadNow();

    private LogManager() {
    }

    public static class LogConfigurationImpl extends ILogConfiguration {
        TreeMap<String, Object> configMap = new TreeMap<>();

        @Override
        public native ILogConfiguration roundTrip();

        LogConfigurationImpl() {
        }

        public TreeMap<String, Object> getConfigMap() {
            return this.configMap;
        }

        public boolean equals(Object obj) {
            if (!getClass().isAssignableFrom(obj.getClass())) {
                return false;
            }
            LogConfigurationImpl logConfigurationImpl = (LogConfigurationImpl) obj;
            if (this.configMap.size() != logConfigurationImpl.configMap.size()) {
                return false;
            }
            for (String str : this.configMap.navigableKeySet()) {
                if (!logConfigurationImpl.configMap.containsKey(str) || logConfigurationImpl.configMap.get(str) != this.configMap.get(str)) {
                    return false;
                }
            }
            return true;
        }

        public boolean valueContainsAll(LogConfigurationImpl logConfigurationImpl, StringBuffer stringBuffer) {
            for (String str : logConfigurationImpl.configMap.navigableKeySet()) {
                Object obj = logConfigurationImpl.configMap.get(str);
                if (obj != null) {
                    if (!this.configMap.containsKey(str)) {
                        stringBuffer.append(String.format("Key %s missing from superset", str));
                        return false;
                    }
                    Object obj2 = this.configMap.get(str);
                    if (obj2 == null) {
                        stringBuffer.append(String.format("Value for key %s is null in superset", str));
                        return false;
                    }
                    if (obj2 == obj) {
                        continue;
                    } else {
                        if (!obj2.getClass().isAssignableFrom(obj.getClass())) {
                            stringBuffer.append(String.format("Value for key %s is class %s in superset, %s in subset", str, obj2.getClass().getName(), obj.getClass().getName()));
                            return false;
                        }
                        if (LogConfigurationImpl.class.isAssignableFrom(obj2.getClass())) {
                            StringBuffer stringBuffer2 = new StringBuffer();
                            if (!((LogConfigurationImpl) obj2).valueContainsAll((LogConfigurationImpl) obj, stringBuffer2)) {
                                stringBuffer.append(String.format("Sub-map %s: %s", str, stringBuffer2));
                                return false;
                            }
                        } else if (obj2.getClass().isArray()) {
                            if (!obj.getClass().isArray()) {
                                stringBuffer.append(String.format("Super array %s: %s, sub %s", str, obj2, obj));
                                return false;
                            }
                            Object[] objArr = (Object[]) obj2;
                            Object[] objArr2 = (Object[]) obj;
                            if (objArr.length != objArr2.length) {
                                stringBuffer.append(String.format("Super array length %s: %d, sub %d", str, Integer.valueOf(objArr.length), Integer.valueOf(objArr2.length)));
                                return false;
                            }
                            for (int i = 0; i < objArr.length; i++) {
                                Object obj3 = objArr[i];
                                Object obj4 = objArr2[i];
                                if (obj4 != null) {
                                    if (obj3 == null) {
                                        stringBuffer.append(String.format("Super %s[%d] is null", str, Integer.valueOf(i)));
                                        return false;
                                    }
                                    if (!obj3.getClass().isAssignableFrom(obj4.getClass())) {
                                        stringBuffer.append(String.format("Value for key %s[%d] is class %s in superset, %s in subset", str, Integer.valueOf(i), obj3.getClass().getName(), obj4.getClass().getName()));
                                        return false;
                                    }
                                    if (LogConfigurationImpl.class.isAssignableFrom(obj3.getClass())) {
                                        StringBuffer stringBuffer3 = new StringBuffer();
                                        if (!((LogConfigurationImpl) obj3).valueContainsAll((LogConfigurationImpl) obj4, stringBuffer3)) {
                                            stringBuffer.append(String.format("Sub-map %s: %s", str, stringBuffer3));
                                            return false;
                                        }
                                    } else if (!obj3.equals(obj3.getClass().cast(obj4))) {
                                        stringBuffer.append(String.format("not equal %s[%d]: %s != %s", str, Integer.valueOf(i), obj3, obj4));
                                        return false;
                                    }
                                }
                            }
                        } else if (!obj2.equals(obj2.getClass().cast(obj))) {
                            stringBuffer.append(String.format("key %s, superset value %s, subset %s", str, obj2, obj));
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        public int hashCode() {
            int iHashCode = 0;
            for (String str : this.configMap.navigableKeySet()) {
                iHashCode ^= str.hashCode();
                Object obj = this.configMap.get(str);
                if (obj != null) {
                    iHashCode ^= obj.hashCode();
                }
            }
            return iHashCode;
        }

        public String toString() {
            return this.configMap.toString();
        }

        @Override
        public Object getObject(String str) {
            if (str == null) {
                return null;
            }
            return this.configMap.get(str);
        }

        @Override
        public Object getObject(LogConfigurationKey logConfigurationKey) {
            return this.configMap.get(logConfigurationKey.getKey());
        }

        @Override
        public Long getLong(LogConfigurationKey logConfigurationKey) {
            Object object = getObject(logConfigurationKey);
            if (Long.class.isInstance(object)) {
                return (Long) Long.class.cast(object);
            }
            return null;
        }

        @Override
        public String getString(LogConfigurationKey logConfigurationKey) {
            Object object = getObject(logConfigurationKey);
            if (String.class.isInstance(object)) {
                return (String) String.class.cast(object);
            }
            return null;
        }

        @Override
        public Boolean getBoolean(LogConfigurationKey logConfigurationKey) {
            Object object = getObject(logConfigurationKey);
            if (Boolean.class.isInstance(object)) {
                return (Boolean) Boolean.class.cast(object);
            }
            return null;
        }

        @Override
        public ILogConfiguration getLogConfiguration(LogConfigurationKey logConfigurationKey) {
            Object object = getObject(logConfigurationKey);
            if (ILogConfiguration.class.isInstance(object)) {
                return (ILogConfiguration) ILogConfiguration.class.cast(object);
            }
            return null;
        }

        @Override
        public boolean set(LogConfigurationKey logConfigurationKey, Boolean bool) {
            if (logConfigurationKey.getValueType() != Boolean.class) {
                return false;
            }
            this.configMap.put(logConfigurationKey.getKey(), bool);
            return true;
        }

        @Override
        public boolean set(LogConfigurationKey logConfigurationKey, Long l) {
            if (logConfigurationKey.getValueType() != Long.class) {
                return false;
            }
            this.configMap.put(logConfigurationKey.getKey(), l);
            return true;
        }

        @Override
        public boolean set(LogConfigurationKey logConfigurationKey, String str) {
            if (logConfigurationKey.getValueType() != String.class) {
                return false;
            }
            this.configMap.put(logConfigurationKey.getKey(), str);
            return true;
        }

        @Override
        public boolean set(LogConfigurationKey logConfigurationKey, ILogConfiguration iLogConfiguration) {
            if (logConfigurationKey.getValueType() != ILogConfiguration.class) {
                return false;
            }
            this.configMap.put(logConfigurationKey.getKey(), iLogConfiguration);
            return true;
        }

        @Override
        public void set(String str, Object obj) {
            this.configMap.put(str, obj);
        }

        public String[] getKeyArray() {
            NavigableSet<String> navigableSetNavigableKeySet = this.configMap.navigableKeySet();
            String[] strArr = new String[navigableSetNavigableKeySet.size()];
            Iterator<String> it = navigableSetNavigableKeySet.iterator();
            int i = 0;
            while (it.hasNext()) {
                strArr[i] = it.next();
                i++;
            }
            return strArr;
        }
    }

    public static synchronized void registerLogger(Logger logger) {
        if (logger != null) {
            loggers.add(logger);
        }
    }

    public static synchronized void removeLogger(Logger logger) {
        if (logger != null) {
            Vector<Logger> vector = loggers;
            while (true) {
                int iIndexOf = vector.indexOf(logger);
                if (iIndexOf < 0) {
                    break;
                }
                Vector<Logger> vector2 = loggers;
                vector2.set(iIndexOf, vector2.lastElement());
//                loggers.setSize(r1.size() - 1);
                vector = loggers;
            }
        }
    }

    public static ILogger initialize() {
        long jNativeInitializeWithoutTenantToken = nativeInitializeWithoutTenantToken();
        if (jNativeInitializeWithoutTenantToken == 0) {
            return null;
        }
        return new Logger(jNativeInitializeWithoutTenantToken);
    }

    public static ILogger initialize(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("tenantToken is null or empty");
        }
        long jNativeInitializeWithTenantToken = nativeInitializeWithTenantToken(str);
        if (jNativeInitializeWithTenantToken == 0) {
            return null;
        }
        return new Logger(jNativeInitializeWithTenantToken);
    }

    public static ILogger initialize(String str, ILogConfiguration iLogConfiguration) {
        if (str == null) {
            str = "";
        }
        long jNativeInitializeConfig = nativeInitializeConfig(str, iLogConfiguration);
        if (jNativeInitializeConfig == 0) {
            return null;
        }
        return new Logger(jNativeInitializeConfig);
    }

    public static synchronized Status flushAndTeardown() {
        Iterator<Logger> it = loggers.iterator();
        while (it.hasNext()) {
            it.next().clearNative();
        }
        loggers.clear();
        return Status.getEnum(nativeFlushAndTeardown());
    }

    public static Status flush() {
        return Status.getEnum(nativeFlush());
    }

    public static Status uploadNow() {
        return Status.getEnum(nativeUploadNow());
    }

    public static Status pauseTransmission() {
        return Status.getEnum(nativePauseTransmission());
    }

    public static Status resumeTransmission() {
        return Status.getEnum(nativeResumeTransmission());
    }

    public static Status setTransmitProfile(TransmitProfile transmitProfile) {
        if (transmitProfile == null) {
            throw new IllegalArgumentException("profile is null");
        }
        return Status.getEnum(nativeSetIntTransmitProfile(transmitProfile.getValue()));
    }

    public static Status setTransmitProfile(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("profile is null or empty");
        }
        return Status.getEnum(nativeSetTransmitProfileString(str));
    }

    public static Status loadTransmitProfiles(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("profilesJson is null or empty");
        }
        return Status.getEnum(nativeLoadTransmitProfilesString(str));
    }

    public static Status resetTransmitProfiles() {
        return Status.getEnum(nativeResetTransmitProfiles());
    }

    public static ISemanticContext getSemanticContext() {
        long jNativeGetSemanticContext = nativeGetSemanticContext();
        if (jNativeGetSemanticContext == 0) {
            return null;
        }
        return new SemanticContext(jNativeGetSemanticContext);
    }

    public static Status setContext(String str, String str2) {
        return setContext(str, str2, PiiKind.None);
    }

    public static Status setContext(String str, String str2, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        return Status.getEnum(nativeSetContextStringValue(str, str2, piiKind.getValue()));
    }

    public static Status setContext(String str, int i) {
        return setContext(str, i, PiiKind.None);
    }

    public static Status setContext(String str, int i, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        return Status.getEnum(nativeSetContextIntValue(str, i, piiKind.getValue()));
    }

    public static Status setContext(String str, long j) {
        return setContext(str, j, PiiKind.None);
    }

    public static Status setContext(String str, long j, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        return Status.getEnum(nativeSetContextLongValue(str, j, piiKind.getValue()));
    }

    public static Status setContext(String str, double d) {
        return setContext(str, d, PiiKind.None);
    }

    public static Status setContext(String str, double d, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        return Status.getEnum(nativeSetContextDoubleValue(str, d, piiKind.getValue()));
    }

    public static Status setContext(String str, boolean z) {
        return setContext(str, z, PiiKind.None);
    }

    public static Status setContext(String str, boolean z, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        return Status.getEnum(nativeSetContextBoolValue(str, z, piiKind.getValue()));
    }

    private static Status setContext(String str, TimeTicks timeTicks, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (timeTicks == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        return Status.getEnum(nativeSetContextTimeTicksValue(str, timeTicks.getTicks(), piiKind.getValue()));
    }

    public static Status setContext(String str, Date date) {
        return setContext(str, date, PiiKind.None);
    }

    public static Status setContext(String str, Date date, PiiKind piiKind) {
        return setContext(str, new TimeTicks(date), piiKind);
    }

    public static Status setContext(String str, UUID uuid) {
        return setContext(str, uuid, PiiKind.None);
    }

    public static Status setContext(String str, UUID uuid, PiiKind piiKind) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if (uuid == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (piiKind == null) {
            throw new IllegalArgumentException("piiKind is null");
        }
        return Status.getEnum(nativeSetContextGuidValue(str, uuid.toString(), piiKind.getValue()));
    }

    public static ILogger getLogger() {
        long jNativeGetLogger = nativeGetLogger();
        if (jNativeGetLogger == 0) {
            return null;
        }
        return new Logger(jNativeGetLogger);
    }

    public static ILogger getLogger(String str) {
        long jNativeGetLoggerWithSource = nativeGetLoggerWithSource(str);
        if (jNativeGetLoggerWithSource == 0) {
            return null;
        }
        return new Logger(jNativeGetLoggerWithSource);
    }

    public static ILogger getLogger(String str, String str2) {
        long jNativeGetLoggerWithTenantTokenAndSource = nativeGetLoggerWithTenantTokenAndSource(str, str2);
        if (jNativeGetLoggerWithTenantTokenAndSource == 0) {
            return null;
        }
        return new Logger(jNativeGetLoggerWithTenantTokenAndSource);
    }

    public static ILogConfiguration logConfigurationFactory() {
        return new LogConfigurationImpl();
    }

    public static ILogConfiguration getLogConfigurationCopy() {
        return nativeGetLogConfiguration();
    }

    public static boolean registerPrivacyGuard() {
        return PrivacyGuard.isInitialized() && nativeRegisterPrivacyGuardOnDefaultLogManager();
    }

    public static boolean unregisterPrivacyGuard() {
        return PrivacyGuard.isInitialized() && nativeUnregisterPrivacyGuardOnDefaultLogManager();
    }
}
