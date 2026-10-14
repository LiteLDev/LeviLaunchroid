package com.mojang.minecraftpe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.annotation.Keep;

import com.mojang.minecraftpe.hardwareinfo.CPUCluster;
import com.mojang.minecraftpe.hardwareinfo.CPUTopologyInfo;
import com.mojang.minecraftpe.hardwareinfo.SystemCPU;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressLint({"DefaultLocale"})
@Keep
public class HardwareInformation {
    private static final CPUInfo cpuInfo = getCPUInfo();
    private final ApplicationInfo appInfo;
    private final Context context;
    private final PackageManager packageManager;

    HardwareInformation(Context context) {
        this.packageManager = context.getPackageManager();
        this.appInfo = context.getApplicationInfo();
        this.context = context;

    }

    @NotNull
    public static String getDeviceModelName() {
        return Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
    }

    public static String getSoCName() {
        String cPULine;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            cPULine = Build.SOC_MODEL.equals("unknown") ? "" : Build.SOC_MODEL;
        } else {
            cPULine = cpuInfo.getCPULine("Hardware");
        }
        return cPULine.isEmpty() ? cpuInfo.getCPULine("model name") : cPULine;
    }

    public static int getPerformanceCoreCount() {
        long highestFrequency = 0;
        int performanceCores = 0;
        for (CPUCluster cluster : CPUTopologyInfo.getInstance().getClusterArray()) {
            for (SystemCPU cpu : cluster.getCPUArray()) {
                long frequency = cpu.getMaxFrequencyHz();
                if (frequency > highestFrequency) {
                    highestFrequency = frequency;
                    performanceCores = 1;
                } else if (frequency > 0 && frequency == highestFrequency) {
                    performanceCores++;
                }
            }
        }
        return highestFrequency > 0 ? performanceCores : 1;
    }

    public static @NotNull String getLocale() {
        return Locale.getDefault().toString();
    }

    public static String getCPUType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return TextUtils.join(" ", Build.SUPPORTED_ABIS);
        }
        return Build.CPU_ABI;
    }

    @NotNull
    @Contract(pure = true)
    public static String getCPUName() {
        CPUInfo cPUInfo = cpuInfo;
        String cPULine = cPUInfo.getCPULine("model name");
        return !cPULine.isEmpty() ? cPULine : cPUInfo.getCPULine("Hardware");

    }

    @NotNull
    @Contract(pure = true)
    public static String getCPUFeatures() {
        return cpuInfo.getCPULine("Features");
    }

    public static int getNumCores() {
        return CPUTopologyInfo.getInstance().getCPUCount();
    }

    public static int getNumClusters() {
        return CPUTopologyInfo.getInstance().getCPUClusterCount();
    }

    @NotNull
    @Contract(" -> new")
    public static CPUInfo getCPUInfo() {
        Map<String, String> list = new HashMap<>();
        int processorCount = 0;
        if (new File("/proc/cpuinfo").exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                Pattern pattern = Pattern.compile("([^:]+)\\s*:\\s*(.*)");
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        String key = matcher.group(1).trim();
                        if (!list.containsKey(key)) {
                            list.put(key, matcher.group(2).trim());
                        }
                        if (key.equals("processor")) {
                            processorCount++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new CPUInfo(list, processorCount);
    }

    @SuppressLint("HardwareIds")
    public static String getSerialNumber() {
        return Build.SERIAL;
    }

    public static String getBoard() {
        return Build.BOARD;
    }

    public String getAndroidVersion() {
        if (((MainActivity) context).isChromebook()) {
            return "ChromeOS " + Build.VERSION.RELEASE;
        }
        return "Android " + Build.VERSION.RELEASE;
    }

    public String getSecureId() {
        return Settings.Secure.getString(context.getContentResolver(), "android_id");
    }

    public String getInstallerPackageName() {
        PackageManager packageManager = this.packageManager;
        return (packageManager == null || this.appInfo == null) ? "" : packageManager.getInstallerPackageName(this.context.getPackageName());
    }

    public String getVulkanHardwareVersion() {
        return Integer.toString(getSystemFeatureVersion(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION));
    }

    public String getVulkanDeqpLevel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "0";
        }
        return Integer.toString(getSystemFeatureVersion(PackageManager.FEATURE_VULKAN_DEQP_LEVEL));
    }

    private int getSystemFeatureVersion(String featureName) {
        try {
            FeatureInfo[] features = packageManager.getSystemAvailableFeatures();
            int version = 0;
            if (features != null) {
                for (FeatureInfo feature : features) {
                    if (feature != null && featureName.equals(feature.name)) {
                        version = Math.max(version, feature.version);
                    }
                }
            }
            return version;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public int getSignaturesHashCode() {
        int hashCode = 0;

        try {
            Signature[] signatures = this.packageManager.getPackageInfo(this.context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;

            for (Signature signature : signatures) {
                hashCode ^= signature.hashCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashCode;
    }

    public boolean getIsRooted() {
        return checkRootA() || checkRootB() || checkRootC();
    }

    private boolean checkRootA() {
        String str = Build.TAGS;
        return str != null && str.contains("test-keys");
    }

    private boolean checkRootB() {
        String[] strArr = {"/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/system/app/Superuser.apk", "/data/local/su", "/su/bin/su"};
        for (int i = 0; i < 10; i++) {
            if (new File(strArr[i]).exists()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkRootC() {
        String[] strArr = {"eu.chainfire.supersu", "eu.chainfire.supersu.pro"};
        for (int i = 0; i < 2; i++) {
            if (appInstalled(strArr[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean appInstalled(String str) {
        try {
            this.packageManager.getPackageInfo(str, 0);
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    public static class CPUInfo {
        private final Map<String, String> cpuLines;
        private final int numberCPUCores;

        public CPUInfo(Map<String, String> cpuLines, int numberCPUCores) {
            this.cpuLines = cpuLines;
            this.numberCPUCores = numberCPUCores;
        }

        public String getCPULine(String line) {
            if (this.cpuLines.containsKey(line)) {
                return cpuLines.get(line);
            }
            return "";
        }

        public int getNumberCPUCores() {
            return numberCPUCores;
        }
    }
}
