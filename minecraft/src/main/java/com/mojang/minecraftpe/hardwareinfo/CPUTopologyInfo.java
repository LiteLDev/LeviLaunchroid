package com.mojang.minecraftpe.hardwareinfo;

import android.util.Log;
import androidx.annotation.NonNull;
import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 29.03.2023
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class CPUTopologyInfo {
    private static final Pattern CPU_LIST_VALUE_FORMAT = Pattern.compile("(\\d{1,4})(?:-(\\d{1,4}))?");
    private static final CPUTopologyInfo SINGLETON_INSTANCE = new CPUTopologyInfo();
    private Set<CPUCluster> CLUSTERS;
    private List<SystemCPU> CPUS;
    private BitSet CPUS_BITSET;
    private int CPU_PROCESSOR_COUNT;

    private CPUTopologyInfo() {
        try {
            initializeCPUInformation();
            CLUSTERS = getClustersFromSiblingCPUs();
        } catch (Exception unused) {
            CPU_PROCESSOR_COUNT = 0;
            CPUS_BITSET = new BitSet();
            CPUS = new ArrayList<>();
            CLUSTERS = new TreeSet<>();
        }
    }

    public static CPUTopologyInfo getInstance() {
        return SINGLETON_INSTANCE;
    }

    @Contract("_, _ -> param2")
    public static <T extends BitCollector> T parseCPUListString(@NonNull String listString, T collector) {
        String[] split;
        if (listString.isEmpty()) {
            return collector;
        }
        for (String str : listString.split(",")) {
            Matcher matcher = CPU_LIST_VALUE_FORMAT.matcher(str);
            if (matcher.find()) {
                int parseInt = Integer.parseInt(matcher.group(1));
                String group = matcher.group(2);
                if (group != null && !group.isEmpty()) {
                    int parseInt2 = Integer.parseInt(group);
                    while (parseInt <= parseInt2) {
                        collector.setBit(parseInt);
                        parseInt++;
                    }
                } else {
                    collector.setBit(parseInt);
                }
            } else {
            }
        }
        return collector;
    }

    private void initializeCPUInformation() throws Exception {
        File file = new File("/sys/devices/system/cpu/present");
        int i = 0;
        if (!file.exists() || !file.canRead()) {
            CPU_PROCESSOR_COUNT = 0;
            CPUS_BITSET = new BitSet();
            CPUS = new ArrayList<>();
        }
        String readLine = new BufferedReader(new FileReader(file)).readLine();
        BitSetCollector bitSetCollector = new BitSetCollector();
        parseCPUListString(readLine, bitSetCollector);
        CPU_PROCESSOR_COUNT = bitSetCollector.getBitCount();
        CPUS = new ArrayList<>(CPU_PROCESSOR_COUNT);
        CPUS_BITSET = bitSetCollector.getBitSet();
        while (true) {
            int nextSetBit = CPUS_BITSET.nextSetBit(i);
            if (nextSetBit < 0) {
                return;
            }
            CPUS.add(new SystemCPU(nextSetBit));
            i = nextSetBit + 1;
        }
    }

    @NonNull
    private Set<CPUCluster> getClustersFromSiblingCPUs() {
        TreeSet<CPUCluster> treeSet = new TreeSet<>();
        List<SystemCPU> cpus = getCPUS();
        ArrayDeque<SystemCPU> arrayDeque = new ArrayDeque<>(cpus.size());
        arrayDeque.addAll(cpus);
        while (!arrayDeque.isEmpty()) {
            SystemCPU systemCPU = (SystemCPU) arrayDeque.poll();
            if (systemCPU != null && systemCPU.exists()) {
                systemCPU.updateCPUFreq();
                String siblingString = systemCPU.getSiblingString();
                Set<SystemCPU> cpuSetFromCPUListString = cpuSetFromCPUListString(siblingString);
                if (!cpuSetFromCPUListString.isEmpty()) {
                    for (SystemCPU systemCPU2 : cpuSetFromCPUListString) {
                        if (systemCPU2 != systemCPU) {
                            systemCPU2.updateCPUFreq();
                        }
                        arrayDeque.remove(systemCPU2);
                    }
                    treeSet.add(new CPUCluster(siblingString, cpuSetFromCPUListString));
                }
            }
        }
        return treeSet;
    }

    public List<SystemCPU> getCPUS() {
        return CPUS;
    }

    public int getCPUCount() {
        return CPU_PROCESSOR_COUNT;
    }

    public Set<SystemCPU> cpuSetFromCPUListString(@NonNull String siblingInfo) {
        final TreeSet<SystemCPU> treeSet = new TreeSet<>();
        if (siblingInfo.isEmpty()) {
            return treeSet;
        }
        parseCPUListString(siblingInfo, i -> treeSet.add(CPUS.get(i)));
        return treeSet;
    }

    public Set<CPUCluster> getClusterSet() {
        return new TreeSet<>(CLUSTERS);
    }

    public CPUCluster[] getClusterArray() {
        return (CPUCluster[]) CLUSTERS.toArray(new CPUCluster[0]);
    }

    public boolean isMultiClusterSystem() {
        return CLUSTERS.size() > 1;
    }

    public int getCPUClusterCount() {
        return CLUSTERS.size();
    }

    public interface BitCollector {
        void setBit(int bitIndex);
    }

    public static class BitSetCollector implements BitCollector {
        private final BitSet bits = new BitSet();
        private int bitsCounted = 0;

        int getBitCount() {
            return bitsCounted;
        }

        BitSet getBitSet() {
            return bits;
        }

        @Override
        public void setBit(int bitIndex) {
            bits.set(bitIndex);
            bitsCounted++;
        }
    }
}