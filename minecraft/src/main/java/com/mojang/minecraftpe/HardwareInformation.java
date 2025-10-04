package com.mojang.minecraftpe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;
import com.mojang.minecraftpe.hardwareinfo.CPUCluster;
import com.mojang.minecraftpe.hardwareinfo.CPUTopologyInfo;
import com.mojang.minecraftpe.platforms.Platform;
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

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
@SuppressLint({"DefaultLocale"})
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
        CPUCluster[] clusterArray = CPUTopologyInfo.getInstance().getClusterArray();
        int numCores = getNumCores();
        if (clusterArray.length == 0) {
            if (numCores > 2) {
                return numCores >> 1;
            }
            return 1;
        } else if (clusterArray.length == 1) {
            return numCores;
        } else {
            CPUCluster cPUCluster = clusterArray[0];
            long maxFreq = cPUCluster.getMaxFreq();
            for (int i = 1; i < clusterArray.length; i++) {
                if (clusterArray[i].getMaxFreq() < maxFreq) {
                    cPUCluster = clusterArray[i];
                }
            }
            return numCores - cPUCluster.getClusterCoreCount();
        }
    }

    public static @NotNull String getLocale() {
        return Locale.getDefault().toString();
    }

    public static String getCPUType() {
        return Platform.createPlatform(false).getABIS();
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
        int cPUCount = CPUTopologyInfo.getInstance().getCPUCount();
        return cPUCount == 0 ? cpuInfo.getNumberCPUCores() : cPUCount;
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
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
                Pattern pattern = Pattern.compile("(\\w*)\\s*:\\s([^\\n]*)");
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find() && matcher.groupCount() == 2) {
                        if (!list.containsKey(matcher.group(1))) {
                            list.put(matcher.group(1), matcher.group(2));
                        }
                        if (matcher.group(1).contentEquals("processor")) {
                            processorCount++;
                        }
                    }
                }
                br.close();
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

    public int getSignaturesHashCode() {
        int hashCode = 0;

        try {
            // Retrieve the package info with signatures
            Signature[] signatures = this.packageManager.getPackageInfo(this.context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;

            // Compute the combined hash code of the signatures
            for (Signature signature : signatures) {
                hashCode ^= signature.hashCode();
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
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
