package com.mojang.minecraftpe.hardwareinfo;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;

public class SystemCPU implements Comparable<SystemCPU> {
    protected static final String SYSTEM_CPU_PATH = "/sys/devices/system/cpu";
    private final BitSet cpuBitMask = new BitSet();
    private final int cpuId;
    private final String path;
    private volatile BitSet siblingCoresMask;
    private volatile String siblingsString;
    private long cpuMinFreq;
    private long cpuMaxFreq;

    SystemCPU(int cpuId) {
        this.cpuId = cpuId;
        cpuBitMask.set(cpuId);
        path = SYSTEM_CPU_PATH + "/cpu" + cpuId;
    }

    public int getCPUId() {
        return cpuId;
    }

    public BitSet getCPUMask() {
        return (BitSet) cpuBitMask.clone();
    }

    public long getMinFrequencyHz() {
        return cpuMinFreq;
    }

    public long getMaxFrequencyHz() {
        return cpuMaxFreq;
    }

    void updateCPUFreq() {
        cpuMinFreq = tryReadFreq("cpuinfo", "min");
        if (cpuMinFreq == 0) {
            cpuMinFreq = tryReadFreq("scaling", "min");
        }
        cpuMaxFreq = tryReadFreq("cpuinfo", "max");
        if (cpuMaxFreq == 0) {
            cpuMaxFreq = tryReadFreq("scaling", "max");
        }
    }

    private long tryReadFreq(String source, String limit) {
        String value = readFirstLine(new File(path + "/cpufreq/" + source + "_" + limit + "_freq"));
        if (value.isEmpty()) {
            return 0;
        }
        try {
            long frequency = Long.parseLong(value);
            return frequency > 0 && frequency <= Long.MAX_VALUE / 1000 ? frequency * 1000 : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    boolean exists() {
        try {
            File file = new File(path);
            return file.isDirectory() && file.canRead();
        } catch (SecurityException ignored) {
            return false;
        }
    }

    public String getSiblingString() {
        String value = siblingsString;
        if (value == null) {
            value = "";
            for (String name : new String[]{"cluster_cpus_list", "core_siblings_list", "package_cpus_list"}) {
                String candidate = readFirstLine(new File(path + "/topology/" + name));
                if (!candidate.isEmpty()) {
                    value = candidate;
                    break;
                }
            }
            siblingsString = value;
        }
        return value;
    }

    public BitSet getSiblingsMask() {
        BitSet mask = siblingCoresMask;
        if (mask == null) {
            synchronized (this) {
                mask = siblingCoresMask;
                if (mask == null) {
                    mask = retrieveSiblingsMask();
                    siblingCoresMask = mask;
                }
            }
        }
        return (BitSet) mask.clone();
    }

    private BitSet retrieveSiblingsMask() {
        String list = getSiblingString();
        if (!list.isEmpty()) {
            CPUTopologyInfo.BitSetCollector collector = CPUTopologyInfo.parseCPUListString(
                    list, new CPUTopologyInfo.BitSetCollector());
            if (collector.getBitCount() > 0) {
                return collector.getBitSet();
            }
        }
        for (String name : new String[]{"cluster_cpus", "core_siblings", "package_cpus"}) {
            String value = readFirstLine(new File(path + "/topology/" + name));
            if (value.isEmpty()) {
                continue;
            }
            try {
                String[] words = value.split(",");
                BitSet mask = new BitSet(words.length * 32);
                for (int word = words.length - 1; word >= 0; word--) {
                    String text = words[word].trim();
                    if (text.isEmpty() || text.length() > 8) {
                        throw new NumberFormatException();
                    }
                    long bits = Long.parseLong(text, 16);
                    if (bits < 0 || bits > 0xffffffffL) {
                        throw new NumberFormatException();
                    }
                    int offset = (words.length - word - 1) * 32;
                    for (int bit = 0; bit < 32; bit++) {
                        if ((bits & (1L << bit)) != 0) {
                            mask.set(offset + bit);
                        }
                    }
                }
                if (!mask.isEmpty()) {
                    return mask;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return getCPUMask();
    }

    private static String readFirstLine(File file) {
        try {
            if (!file.isFile() || !file.canRead()) {
                return "";
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                return line == null ? "" : line.trim();
            }
        } catch (IOException | SecurityException ignored) {
            Log.v("MCPE", "Cannot read file: " + file.getAbsolutePath());
            return "";
        }
    }

    public Set<SystemCPU> getSiblingCPUs() {
        CPUTopologyInfo topology = CPUTopologyInfo.getInstance();
        Set<SystemCPU> result = new TreeSet<>();
        BitSet mask = getSiblingsMask();
        for (int id = mask.nextSetBit(0); id >= 0; id = mask.nextSetBit(id + 1)) {
            SystemCPU cpu = topology.getCPU(id);
            if (cpu != null) {
                result.add(cpu);
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return cpuId;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof SystemCPU && ((SystemCPU) object).cpuId == cpuId;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public int compareTo(SystemCPU cpu) {
        return Integer.compare(cpuId, cpu.cpuId);
    }
}
