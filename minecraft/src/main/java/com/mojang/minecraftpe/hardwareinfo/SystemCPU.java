package com.mojang.minecraftpe.hardwareinfo;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

/**
 * 29.03.2023
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class SystemCPU implements Comparable<SystemCPU> {
    protected static final String SYSTEM_CPU_PATH = "/sys/devices/system/cpu";
    private final BitSet CPU_BIT_MASK;
    private final int CPU_ID;
    private final String PATH;
    private BitSet siblingCoresMask;
    private long cpuMinFreq = 0;
    private long cpuMaxFreq = 0;

    public SystemCPU(int cpuId) {
        CPU_ID = cpuId;
        BitSet bitSet = new BitSet();
        CPU_BIT_MASK = bitSet;
        bitSet.set(cpuId);
        PATH = SYSTEM_CPU_PATH + "/cpu" + cpuId;
    }

    public int getCPUId() {
        return CPU_ID;
    }

    public BitSet getCPUMask() {
        return (BitSet) CPU_BIT_MASK.clone();
    }

    public long getMinFrequencyHz() {
        return cpuMinFreq;
    }

    public long getMaxFrequencyHz() {
        return cpuMaxFreq;
    }

    public void updateCPUFreq() {
        long tryReadFreq = tryReadFreq("cpuinfo", "min");
        cpuMinFreq = tryReadFreq;
        if (tryReadFreq == 0) {
            cpuMinFreq = tryReadFreq("scaling", "min");
        }
        long tryReadFreq2 = tryReadFreq("cpuinfo", "max");
        cpuMaxFreq = tryReadFreq2;
        if (tryReadFreq2 == 0) {
            cpuMaxFreq = tryReadFreq("scaling", "max");
        }
    }

    private long tryReadFreq(String source, String value) {
        File file = new File(PATH + "/cpufreq/" + source + "_" + value + "_freq");
        if (file.exists() && file.canRead()) {
            try {
                Scanner scanner = new Scanner(file);
                long nextInt = scanner.nextInt();
                scanner.close();
                return nextInt;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return 0L;
            }
        }
        return 0L;
    }

    public boolean exists() {
        return getSystemCPUFile() != null;
    }

    @Nullable
    private File getSystemCPUFile() {
        File file = new File(PATH);
        if (!file.exists()) {
            return null;
        } else if (file.canRead()) {
            return file;
        } else {
            return null;
        }
    }

    public String getSiblingString() {
        String str = PATH + "/topology";
        File file = new File(str + "/core_siblings_list");
        if (!file.exists() || !file.canRead()) {
            file = new File(str + "/package_cpus_list");
        }
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String readLine = bufferedReader.readLine();
            bufferedReader.close();
            return readLine;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public BitSet getSiblingsMask() {
        if (siblingCoresMask == null) {
            siblingCoresMask = retrieveSiblingsMask();
        }
        return siblingCoresMask;
    }

    @Nullable
    private BitSet retrieveSiblingsMask() {
        File file;
        String[] strArr = {"/core_siblings", "/package_cpus"};
        String str = PATH + "/topology";
        int i = 0;
        while (true) {
            if (i >= 2) {
                file = null;
                break;
            }
            file = new File(str + strArr[i]);
            if (file.exists() && file.canRead()) {
                break;
            }
            i++;
        }
        if (file == null) {
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String[] split = bufferedReader.readLine().split(",");
            BitSet bitSet = new BitSet(split.length * 32);
            for (int i2 = 0; i2 < split.length; i2 += 2) {
                int i3 = i2 + 1;
                bitSet.or(BitSet.valueOf(new long[]{((i3 < split.length ? Long.parseLong(split[i3].trim().toUpperCase(), 16) : 0L) << 32) | Long.parseLong(split[i2].trim().toUpperCase(), 16)}));
            }
            bufferedReader.close();
            return bitSet;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<SystemCPU> getSiblingCPUs() {
        List<SystemCPU> cpus = CPUTopologyInfo.getInstance().getCPUS();
        TreeSet<SystemCPU> treeSet = new TreeSet<>();
        BitSet siblingsMask = getSiblingsMask();
        if (siblingsMask != null && !siblingsMask.isEmpty()) {
            int i = 0;
            while (true) {
                int nextSetBit = siblingsMask.nextSetBit(i);
                if (nextSetBit < 0) {
                    break;
                }
                treeSet.add(cpus.get(nextSetBit));
                i = nextSetBit + 1;
            }
        }
        return treeSet;
    }

    public int hashCode() {
        return CPU_ID;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other != null && other.getClass() == getClass() && ((SystemCPU) other).CPU_ID == CPU_ID;
    }

    @NonNull
    public String toString() {
        return PATH;
    }

    @Override
    public int compareTo(@NonNull SystemCPU other) {
        return Integer.compare(CPU_ID, other.CPU_ID);
    }
}