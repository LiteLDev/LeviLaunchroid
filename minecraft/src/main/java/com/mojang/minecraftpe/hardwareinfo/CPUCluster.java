package com.mojang.minecraftpe.hardwareinfo;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class CPUCluster implements Comparable<CPUCluster> {
    private final BitSet bitmask = new BitSet();
    private final Set<SystemCPU> clusterCPUs;
    final int[] cpuIds;
    private final long maxFreq;
    private final long minFreq;
    final String siblingsString;

    CPUCluster(String siblingsString, Set<SystemCPU> cpus) {
        this.clusterCPUs = Collections.unmodifiableSet(new TreeSet<>(cpus));
        this.siblingsString = siblingsString == null ? "" : siblingsString;
        this.cpuIds = new int[clusterCPUs.size()];
        long minimum = Long.MAX_VALUE;
        long maximum = 0;
        int index = 0;
        for (SystemCPU cpu : clusterCPUs) {
            cpuIds[index++] = cpu.getCPUId();
            bitmask.or(cpu.getCPUMask());
            long frequency = cpu.getMinFrequencyHz();
            if (frequency > 0) {
                minimum = Math.min(minimum, frequency);
            }
            maximum = Math.max(maximum, cpu.getMaxFrequencyHz());
        }
        minFreq = minimum == Long.MAX_VALUE ? 0 : minimum;
        maxFreq = maximum;
    }

    public String getSiblingsString() {
        return siblingsString;
    }

    public boolean contains(int cpuId) {
        return cpuId >= 0 && bitmask.get(cpuId);
    }

    public int[] getCPUIds() {
        return cpuIds.clone();
    }

    public int getClusterCoreCount() {
        return clusterCPUs.size();
    }

    public SystemCPU[] getCPUArray() {
        return clusterCPUs.toArray(new SystemCPU[0]);
    }

    public long getMinFreq() {
        return minFreq;
    }

    public long getMaxFreq() {
        return maxFreq;
    }

    @Override
    public int hashCode() {
        return bitmask.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof CPUCluster && bitmask.equals(((CPUCluster) object).bitmask);
    }

    @Override
    public int compareTo(CPUCluster cluster) {
        BitSet difference = (BitSet) bitmask.clone();
        difference.xor(cluster.bitmask);
        int lastDifference = difference.length() - 1;
        return lastDifference < 0 ? 0 : bitmask.get(lastDifference) ? 1 : -1;
    }
}
