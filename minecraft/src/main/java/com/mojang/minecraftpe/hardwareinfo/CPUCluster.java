package com.mojang.minecraftpe.hardwareinfo;

import androidx.annotation.NonNull;

import java.util.BitSet;
import java.util.Set;

/**
 * 29.03.2023
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class CPUCluster implements Comparable<CPUCluster> {
    private final BitSet bitmask = new BitSet();
    private final Set<SystemCPU> clusterCPUs;
    int[] cpuIds;
    String siblingsString;
    private long maxFreq;
    private long minFreq;

    public CPUCluster(String siblingCPUs, @NonNull Set<SystemCPU> cpus) {
        minFreq = Integer.MAX_VALUE;
        maxFreq = Integer.MIN_VALUE;
        clusterCPUs = cpus;
        siblingsString = siblingCPUs;
        cpuIds = new int[cpus.size()];
        int i = 0;
        for (SystemCPU systemCPU : cpus) {
            cpuIds[i] = systemCPU.getCPUId();
            bitmask.or(systemCPU.getCPUMask());
            minFreq = Math.min(systemCPU.getMinFrequencyHz(), minFreq);
            maxFreq = Math.max(systemCPU.getMaxFrequencyHz(), maxFreq);
            i++;
        }
    }

    public String getSiblingsString() {
        return siblingsString;
    }

    public boolean contains(int cpuId) {
        return clusterCPUs.contains(cpuId);
    }

    public int[] getCPUIds() {
        return cpuIds.clone();
    }

    public int getClusterCoreCount() {
        return clusterCPUs.size();
    }

    public SystemCPU[] getCPUArray() {
        Set<SystemCPU> set = clusterCPUs;
        return set.toArray(new SystemCPU[set.size()]);
    }

    public long getMinFreq() {
        return minFreq;
    }

    public long getMaxFreq() {
        return maxFreq;
    }

    public int hashCode() {
        return bitmask.hashCode();
    }

    @Override
    public int compareTo(@NonNull CPUCluster other) {
        BitSet bitSet = (BitSet) bitmask.clone();
        bitSet.xor(other.bitmask);
        if (bitSet.isEmpty()) {
            return 0;
        }
        return bitSet.length() == other.bitmask.length() ? -1 : 1;
    }
}