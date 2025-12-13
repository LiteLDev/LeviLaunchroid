#include <jni.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <vector>
#include <string>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <unistd.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <errno.h>

struct MemoryRegion {
    uintptr_t start;
    uintptr_t end;
    bool readable;
    bool writable;
    std::string name;
};

static std::vector<MemoryRegion> g_regions;
static std::vector<uintptr_t> g_results;
static int g_searchType = 0;
static int g_memFd = -1;

static void parseMemoryMaps() {
    g_regions.clear();
    std::ifstream maps("/proc/self/maps");
    std::string line;
    while (std::getline(maps, line)) {
        uintptr_t start, end;
        char perms[5];
        char name[256] = {0};
        if (sscanf(line.c_str(), "%lx-%lx %4s %*s %*s %*s %255[^\n]", &start, &end, perms, name) >= 3) {
            MemoryRegion region;
            region.start = start;
            region.end = end;
            region.readable = (perms[0] == 'r');
            region.writable = (perms[1] == 'w');
            char* n = name;
            while (*n == ' ' || *n == '\t') n++;
            region.name = n;
            if (region.readable) {
                g_regions.push_back(region);
            }
        }
    }
}

static bool openMemFd() {
    if (g_memFd < 0) {
        g_memFd = open("/proc/self/mem", O_RDWR);
    }
    return g_memFd >= 0;
}

template<typename T>
static bool readMemory(uintptr_t addr, T* value) {
    if (g_memFd < 0 && !openMemFd()) return false;
    if (lseek64(g_memFd, addr, SEEK_SET) == -1) return false;
    return read(g_memFd, value, sizeof(T)) == sizeof(T);
}

template<typename T>
static bool writeMemory(uintptr_t addr, T value) {
    if (g_memFd < 0 && !openMemFd()) return false;
    if (lseek64(g_memFd, addr, SEEK_SET) == -1) return false;
    return write(g_memFd, &value, sizeof(T)) == sizeof(T);
}

static bool shouldSearchRegion(const MemoryRegion& region) {
    if (!region.readable || !region.writable) return false;
    if (region.name.find("libminecraftpe") != std::string::npos) return true;
    if (region.name.find("[heap]") != std::string::npos) return true;
    if (region.name.find("[anon:libc_malloc]") != std::string::npos) return true;
    if (region.name.find("[anon:scudo:") != std::string::npos) return true;
    if (region.name.empty() || region.name.find("[anon:") == 0) {
        size_t size = region.end - region.start;
        if (size > 4096 && size < 256 * 1024 * 1024) return true;
    }
    return false;
}

#define CHUNK_SIZE 4096
static uint8_t g_chunk[CHUNK_SIZE];

static bool readChunk(uintptr_t addr, size_t size) {
    if (g_memFd < 0 && !openMemFd()) return false;
    if (lseek64(g_memFd, addr, SEEK_SET) == -1) return false;
    return read(g_memFd, g_chunk, size) == (ssize_t)size;
}

template<typename T>
static void searchValue(T targetValue, bool isXor, uint64_t xorKey) {
    g_results.clear();
    for (const auto& region : g_regions) {
        if (!shouldSearchRegion(region)) continue;
        for (uintptr_t chunkAddr = region.start; chunkAddr < region.end; chunkAddr += CHUNK_SIZE) {
            size_t chunkSize = std::min((size_t)(region.end - chunkAddr), (size_t)CHUNK_SIZE);
            if (!readChunk(chunkAddr, chunkSize)) continue;
            for (size_t offset = 0; offset + sizeof(T) <= chunkSize; offset += sizeof(T)) {
                T value = *reinterpret_cast<T*>(g_chunk + offset);
                T compareValue = isXor ? (value ^ static_cast<T>(xorKey)) : value;
                if (compareValue == targetValue) {
                    g_results.push_back(chunkAddr + offset);
                    if (g_results.size() >= 50000) return;
                }
            }
        }
    }
}

template<typename T>
static void filterValue(T targetValue, int condition, bool isXor, uint64_t xorKey) {
    std::vector<uintptr_t> newResults;
    for (uintptr_t addr : g_results) {
        T value;
        if (readMemory(addr, &value)) {
            T compareValue = isXor ? (value ^ static_cast<T>(xorKey)) : value;
            bool match = false;
            switch (condition) {
                case 0: match = (compareValue == targetValue); break;
                case 1: match = (compareValue != targetValue); break;
                case 2: match = (compareValue > targetValue); break;
                case 3: match = (compareValue < targetValue); break;
                case 4: match = (compareValue >= targetValue); break;
                case 5: match = (compareValue <= targetValue); break;
                default: match = (compareValue == targetValue); break;
            }
            if (match) newResults.push_back(addr);
        }
    }
    g_results = std::move(newResults);
}

extern "C" {

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeInit(JNIEnv *env, jclass clazz) {
    parseMemoryMaps();
    openMemFd();
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeRefreshRegions(JNIEnv *env, jclass clazz) {
    parseMemoryMaps();
}

JNIEXPORT jint JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeGetRegionCount(JNIEnv *env, jclass clazz) {
    return static_cast<jint>(g_regions.size());
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeSearchByte(JNIEnv *env, jclass clazz, jbyte value, jboolean isXor, jlong xorKey) {
    g_searchType = 0;
    searchValue<int8_t>(value, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeSearchWord(JNIEnv *env, jclass clazz, jshort value, jboolean isXor, jlong xorKey) {
    g_searchType = 1;
    searchValue<int16_t>(value, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeSearchDword(JNIEnv *env, jclass clazz, jint value, jboolean isXor, jlong xorKey) {
    g_searchType = 2;
    searchValue<int32_t>(value, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeSearchQword(JNIEnv *env, jclass clazz, jlong value, jboolean isXor, jlong xorKey) {
    g_searchType = 3;
    searchValue<int64_t>(value, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeSearchFloat(JNIEnv *env, jclass clazz, jfloat targetValue, jboolean isXor, jlong xorKey) {
    g_searchType = 4;
    g_results.clear();
    for (const auto& region : g_regions) {
        if (!shouldSearchRegion(region)) continue;
        for (uintptr_t chunkAddr = region.start; chunkAddr < region.end; chunkAddr += CHUNK_SIZE) {
            size_t chunkSize = std::min((size_t)(region.end - chunkAddr), (size_t)CHUNK_SIZE);
            if (!readChunk(chunkAddr, chunkSize)) continue;
            for (size_t offset = 0; offset + sizeof(float) <= chunkSize; offset += 4) {
                float v = *reinterpret_cast<float*>(g_chunk + offset);
                float diff = v - targetValue;
                if (diff < 0) diff = -diff;
                if (diff < 0.01f) {
                    g_results.push_back(chunkAddr + offset);
                    if (g_results.size() >= 50000) return;
                }
            }
        }
    }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeSearchDouble(JNIEnv *env, jclass clazz, jdouble targetValue, jboolean isXor, jlong xorKey) {
    g_searchType = 5;
    g_results.clear();
    for (const auto& region : g_regions) {
        if (!shouldSearchRegion(region)) continue;
        for (uintptr_t chunkAddr = region.start; chunkAddr < region.end; chunkAddr += CHUNK_SIZE) {
            size_t chunkSize = std::min((size_t)(region.end - chunkAddr), (size_t)CHUNK_SIZE);
            if (!readChunk(chunkAddr, chunkSize)) continue;
            for (size_t offset = 0; offset + sizeof(double) <= chunkSize; offset += 8) {
                double v = *reinterpret_cast<double*>(g_chunk + offset);
                double diff = v - targetValue;
                if (diff < 0) diff = -diff;
                if (diff < 0.001) {
                    g_results.push_back(chunkAddr + offset);
                    if (g_results.size() >= 50000) return;
                }
            }
        }
    }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeFilterByte(JNIEnv *env, jclass clazz, jbyte value, jint condition, jboolean isXor, jlong xorKey) {
    filterValue<int8_t>(value, condition, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeFilterWord(JNIEnv *env, jclass clazz, jshort value, jint condition, jboolean isXor, jlong xorKey) {
    filterValue<int16_t>(value, condition, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeFilterDword(JNIEnv *env, jclass clazz, jint value, jint condition, jboolean isXor, jlong xorKey) {
    filterValue<int32_t>(value, condition, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeFilterQword(JNIEnv *env, jclass clazz, jlong value, jint condition, jboolean isXor, jlong xorKey) {
    filterValue<int64_t>(value, condition, isXor, xorKey);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeFilterFloat(JNIEnv *env, jclass clazz, jfloat targetValue, jint condition, jboolean isXor, jlong xorKey) {
    std::vector<uintptr_t> newResults;
    for (uintptr_t addr : g_results) {
        float value;
        if (readMemory(addr, &value)) {
            bool match = false;
            switch (condition) {
                case 0: match = (std::abs(value - targetValue) < 0.01f); break;
                case 1: match = (std::abs(value - targetValue) >= 0.01f); break;
                case 2: match = (value > targetValue); break;
                case 3: match = (value < targetValue); break;
                case 4: match = (value >= targetValue); break;
                case 5: match = (value <= targetValue); break;
            }
            if (match) newResults.push_back(addr);
        }
    }
    g_results = std::move(newResults);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeFilterDouble(JNIEnv *env, jclass clazz, jdouble targetValue, jint condition, jboolean isXor, jlong xorKey) {
    std::vector<uintptr_t> newResults;
    for (uintptr_t addr : g_results) {
        double value;
        if (readMemory(addr, &value)) {
            bool match = false;
            switch (condition) {
                case 0: match = (std::abs(value - targetValue) < 0.001); break;
                case 1: match = (std::abs(value - targetValue) >= 0.001); break;
                case 2: match = (value > targetValue); break;
                case 3: match = (value < targetValue); break;
                case 4: match = (value >= targetValue); break;
                case 5: match = (value <= targetValue); break;
            }
            if (match) newResults.push_back(addr);
        }
    }
    g_results = std::move(newResults);
}

JNIEXPORT jint JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeGetResultCount(JNIEnv *env, jclass clazz) {
    return static_cast<jint>(g_results.size());
}

JNIEXPORT jlongArray JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeGetResults(JNIEnv *env, jclass clazz, jint offset, jint count) {
    int start = std::min(static_cast<size_t>(offset), g_results.size());
    int end = std::min(static_cast<size_t>(offset + count), g_results.size());
    int len = end - start;
    jlongArray result = env->NewLongArray(len);
    if (len > 0) {
        std::vector<jlong> arr(len);
        for (int i = 0; i < len; i++) {
            arr[i] = static_cast<jlong>(g_results[start + i]);
        }
        env->SetLongArrayRegion(result, 0, len, arr.data());
    }
    return result;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeClearResults(JNIEnv *env, jclass clazz) {
    g_results.clear();
}

JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeReadByte(JNIEnv *env, jclass clazz, jlong address) {
    int8_t value = 0;
    readMemory(static_cast<uintptr_t>(address), &value);
    return value;
}

JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeReadWord(JNIEnv *env, jclass clazz, jlong address) {
    int16_t value = 0;
    readMemory(static_cast<uintptr_t>(address), &value);
    return value;
}

JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeReadDword(JNIEnv *env, jclass clazz, jlong address) {
    int32_t value = 0;
    readMemory(static_cast<uintptr_t>(address), &value);
    return value;
}

JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeReadQword(JNIEnv *env, jclass clazz, jlong address) {
    int64_t value = 0;
    readMemory(static_cast<uintptr_t>(address), &value);
    return value;
}

JNIEXPORT jfloat JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeReadFloat(JNIEnv *env, jclass clazz, jlong address) {
    float value = 0;
    readMemory(static_cast<uintptr_t>(address), &value);
    return value;
}

JNIEXPORT jdouble JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeReadDouble(JNIEnv *env, jclass clazz, jlong address) {
    double value = 0;
    readMemory(static_cast<uintptr_t>(address), &value);
    return value;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeWriteByte(JNIEnv *env, jclass clazz, jlong address, jbyte value) {
    return writeMemory(static_cast<uintptr_t>(address), static_cast<int8_t>(value));
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeWriteWord(JNIEnv *env, jclass clazz, jlong address, jshort value) {
    return writeMemory(static_cast<uintptr_t>(address), static_cast<int16_t>(value));
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeWriteDword(JNIEnv *env, jclass clazz, jlong address, jint value) {
    return writeMemory(static_cast<uintptr_t>(address), static_cast<int32_t>(value));
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeWriteQword(JNIEnv *env, jclass clazz, jlong address, jlong value) {
    return writeMemory(static_cast<uintptr_t>(address), static_cast<int64_t>(value));
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeWriteFloat(JNIEnv *env, jclass clazz, jlong address, jfloat value) {
    return writeMemory(static_cast<uintptr_t>(address), value);
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeWriteDouble(JNIEnv *env, jclass clazz, jlong address, jdouble value) {
    return writeMemory(static_cast<uintptr_t>(address), value);
}

JNIEXPORT jint JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeGetSearchType(JNIEnv *env, jclass clazz) {
    return g_searchType;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_memoryeditor_MemoryEditorNative_nativeClose(JNIEnv *env, jclass clazz) {
    if (g_memFd >= 0) {
        close(g_memFd);
        g_memFd = -1;
    }
    g_results.clear();
    g_regions.clear();
}

}
