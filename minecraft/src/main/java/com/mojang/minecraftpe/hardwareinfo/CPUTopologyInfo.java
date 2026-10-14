package com.mojang.minecraftpe.hardwareinfo;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CPUTopologyInfo {
    private static final Pattern CPU_LIST_VALUE_FORMAT = Pattern.compile("(\\d{1,4})(?:-(\\d{1,4}))?");
    private static final CPUTopologyInfo SINGLETON_INSTANCE = new CPUTopologyInfo();
    private final List<SystemCPU> cpus = new ArrayList<>();
    private final Map<Integer, SystemCPU> cpusById = new HashMap<>();
    private final Set<CPUCluster> clusters;

    public interface BitCollector {
        void setBit(int cpuId);
    }

    public static CPUTopologyInfo getInstance() {
        return SINGLETON_INSTANCE;
    }

    private CPUTopologyInfo() {
        BitSet cpuIds = readCPUIds();
        for (int id = cpuIds.nextSetBit(0); id >= 0; id = cpuIds.nextSetBit(id + 1)) {
            SystemCPU cpu = new SystemCPU(id);
            cpu.updateCPUFreq();
            cpus.add(cpu);
            cpusById.put(id, cpu);
        }
        clusters = getClustersFromSiblingCPUs();
    }

    private BitSet readCPUIds() {
        for (String name : new String[]{"present", "possible"}) {
            File file = new File(SystemCPU.SYSTEM_CPU_PATH, name);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                BitSetCollector collector = parseCPUListString(reader.readLine(), new BitSetCollector());
                if (collector.getBitCount() > 0) {
                    return collector.getBitSet();
                }
            } catch (IOException | SecurityException ignored) {
            }
        }

        BitSet ids = new BitSet();
        try {
            File[] directories = new File(SystemCPU.SYSTEM_CPU_PATH).listFiles();
            if (directories != null) {
                for (File directory : directories) {
                    String name = directory.getName();
                    if (directory.isDirectory() && name.matches("cpu\\d{1,4}")) {
                        ids.set(Integer.parseInt(name.substring(3)));
                    }
                }
            }
        } catch (SecurityException ignored) {
        }
        if (ids.isEmpty()) {
            ids.set(0, Math.max(1, Runtime.getRuntime().availableProcessors()));
        }
        return ids;
    }

    private Set<CPUCluster> getClustersFromSiblingCPUs() {
        Set<CPUCluster> result = new TreeSet<>();
        Set<SystemCPU> remaining = new TreeSet<>(cpus);
        while (!remaining.isEmpty()) {
            SystemCPU cpu = remaining.iterator().next();
            String siblingsString = cpu.getSiblingString();
            Set<SystemCPU> siblings = cpuSetFromCPUListString(siblingsString);
            if (siblingsString.isEmpty()) {
                for (SystemCPU candidate : remaining) {
                    if (candidate.getSiblingString().isEmpty()) {
                        siblings.add(candidate);
                    }
                }
            }
            siblings.retainAll(remaining);
            siblings.add(cpu);
            remaining.removeAll(siblings);
            result.add(new CPUCluster(siblingsString, siblings));
        }
        return result;
    }

    List<SystemCPU> getCPUS() {
        return Collections.unmodifiableList(cpus);
    }

    SystemCPU getCPU(int cpuId) {
        return cpusById.get(cpuId);
    }

    public int getCPUCount() {
        return cpus.size();
    }

    public Set<SystemCPU> cpuSetFromCPUListString(String value) {
        Set<SystemCPU> result = new TreeSet<>();
        parseCPUListString(value, cpuId -> {
            SystemCPU cpu = cpusById.get(cpuId);
            if (cpu != null) {
                result.add(cpu);
            }
        });
        return result;
    }

    public static class BitSetCollector implements BitCollector {
        private final BitSet bits = new BitSet();

        int getBitCount() {
            return bits.cardinality();
        }

        BitSet getBitSet() {
            return (BitSet) bits.clone();
        }

        @Override
        public void setBit(int cpuId) {
            if (cpuId >= 0) {
                bits.set(cpuId);
            }
        }
    }

    public static <T extends BitCollector> T parseCPUListString(String value, T collector) {
        if (value == null || value.trim().isEmpty()) {
            return collector;
        }
        for (String part : value.split(",")) {
            Matcher matcher = CPU_LIST_VALUE_FORMAT.matcher(part.trim());
            if (!matcher.matches()) {
                Log.w("MCPE", "Unknown CPU List format: " + part);
                continue;
            }
            int first = Integer.parseInt(matcher.group(1));
            String end = matcher.group(2);
            int last = end == null ? first : Integer.parseInt(end);
            if (last < first) {
                Log.w("MCPE", "Unknown CPU List format: " + part);
                continue;
            }
            for (int cpuId = first; cpuId <= last; cpuId++) {
                collector.setBit(cpuId);
            }
        }
        return collector;
    }

    public Set<CPUCluster> getClusterSet() {
        return new TreeSet<>(clusters);
    }

    public CPUCluster[] getClusterArray() {
        return clusters.toArray(new CPUCluster[0]);
    }

    public boolean isMultiClusterSystem() {
        return clusters.size() > 1;
    }

    public int getCPUClusterCount() {
        return clusters.size();
    }
}
