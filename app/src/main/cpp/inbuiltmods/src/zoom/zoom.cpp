#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdint>
#include <sys/mman.h>
#include <unistd.h>
#include <fstream>
#include <string>

#include "common/transition.h"
#include "pl/Gloss.h"

#define LOG_TAG "LeviZoom"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_initialized = false;
static bool g_zoomKeyDown = false;
static bool g_animated = true;
static uint64_t g_zoomLevel = 5345000000ULL;
static uint64_t g_lastClientZoom = 0;
static Transition g_transition;

static uint64_t (*g_CameraAPI_tryGetFOV_orig)(void*) = nullptr;

static uint64_t unsignedDiff(uint64_t a, uint64_t b) {
    return (a > b) ? (a - b) : (b - a);
}

static int clamp(int minVal, int v, int maxVal) {
    if (v < minVal) return minVal;
    if (v > maxVal) return maxVal;
    return v;
}

static uint64_t CameraAPI_tryGetFOV_hook(void* thisPtr) {
    if (!g_CameraAPI_tryGetFOV_orig) {
        return 0;
    }
    
    g_lastClientZoom = g_CameraAPI_tryGetFOV_orig(thisPtr);
    
    if (!g_animated) {
        return g_zoomKeyDown ? g_zoomLevel : g_lastClientZoom;
    }
    
    if (g_transition.inProgress() || g_zoomKeyDown) {
        g_transition.tick();
        uint64_t current = g_transition.getCurrent();
        if (current == 0) {
            return g_lastClientZoom;
        }
        return current;
    }
    
    return g_lastClientZoom;
}

static bool findAndHookCameraAPI() {
    void* mcLib = dlopen("libminecraftpe.so", RTLD_NOLOAD);
    if (!mcLib) {
        mcLib = dlopen("libminecraftpe.so", RTLD_LAZY);
    }
    if (!mcLib) {
        LOGE("Failed to open libminecraftpe.so");
        return false;
    }
    
    uintptr_t libBase = 0;
    
    std::ifstream maps("/proc/self/maps");
    std::string line;
    while (std::getline(maps, line)) {
        if (line.find("libminecraftpe.so") != std::string::npos && line.find("r-xp") != std::string::npos) {
            uintptr_t start, end;
            if (sscanf(line.c_str(), "%lx-%lx", &start, &end) == 2) {
                if (libBase == 0) libBase = start;
            }
        }
    }
    
    if (libBase == 0) {
        LOGE("Failed to find libminecraftpe.so base address");
        return false;
    }
    
    LOGI("libminecraftpe.so base: 0x%lx", libBase);
    
    const char* typeinfoName = "9CameraAPI";
    size_t nameLen = strlen(typeinfoName);
    
    uintptr_t typeinfoNameAddr = 0;
    
    std::ifstream maps2("/proc/self/maps");
    while (std::getline(maps2, line)) {
        if (line.find("libminecraftpe.so") == std::string::npos) continue;
        if (line.find("r--p") == std::string::npos && line.find("r-xp") == std::string::npos) continue;
        
        uintptr_t start, end;
        if (sscanf(line.c_str(), "%lx-%lx", &start, &end) != 2) continue;
        
        for (uintptr_t addr = start; addr < end - nameLen; addr++) {
            if (memcmp((void*)addr, typeinfoName, nameLen) == 0) {
                typeinfoNameAddr = addr;
                LOGI("Found typeinfo name at 0x%lx", typeinfoNameAddr);
                break;
            }
        }
        if (typeinfoNameAddr != 0) break;
    }
    
    if (typeinfoNameAddr == 0) {
        LOGE("Failed to find CameraAPI typeinfo name");
        return false;
    }
    
    uintptr_t typeinfoAddr = 0;
    
    std::ifstream maps3("/proc/self/maps");
    while (std::getline(maps3, line)) {
        if (line.find("libminecraftpe.so") == std::string::npos) continue;
        if (line.find("r--p") == std::string::npos) continue;
        
        uintptr_t start, end;
        if (sscanf(line.c_str(), "%lx-%lx", &start, &end) != 2) continue;
        
        for (uintptr_t addr = start; addr < end - sizeof(void*); addr += sizeof(void*)) {
            uintptr_t* ptr = (uintptr_t*)addr;
            if (*ptr == typeinfoNameAddr) {
                typeinfoAddr = addr - sizeof(void*);
                LOGI("Found typeinfo at 0x%lx", typeinfoAddr);
                break;
            }
        }
        if (typeinfoAddr != 0) break;
    }
    
    if (typeinfoAddr == 0) {
        LOGE("Failed to find CameraAPI typeinfo");
        return false;
    }
    
    uintptr_t vtableAddr = 0;
    
    std::ifstream maps4("/proc/self/maps");
    while (std::getline(maps4, line)) {
        if (line.find("libminecraftpe.so") == std::string::npos) continue;
        if (line.find("r--p") == std::string::npos) continue;
        
        uintptr_t start, end;
        if (sscanf(line.c_str(), "%lx-%lx", &start, &end) != 2) continue;
        
        for (uintptr_t addr = start; addr < end - sizeof(void*); addr += sizeof(void*)) {
            uintptr_t* ptr = (uintptr_t*)addr;
            if (*ptr == typeinfoAddr) {
                vtableAddr = addr + sizeof(void*);
                LOGI("Found vtable at 0x%lx", vtableAddr);
                break;
            }
        }
        if (vtableAddr != 0) break;
    }
    
    if (vtableAddr == 0) {
        LOGE("Failed to find CameraAPI vtable");
        return false;
    }
    
    uint64_t* tryGetFOVSlot = (uint64_t*)(vtableAddr + 7 * sizeof(void*));
    g_CameraAPI_tryGetFOV_orig = (uint64_t(*)(void*))(*tryGetFOVSlot);
    
    LOGI("Original tryGetFOV at 0x%lx", (uintptr_t)g_CameraAPI_tryGetFOV_orig);
    
    uintptr_t pageStart = (uintptr_t)tryGetFOVSlot & ~(4095UL);
    if (mprotect((void*)pageStart, 4096, PROT_READ | PROT_WRITE) != 0) {
        LOGE("Failed to make vtable writable");
        return false;
    }
    
    *tryGetFOVSlot = (uint64_t)CameraAPI_tryGetFOV_hook;
    
    mprotect((void*)pageStart, 4096, PROT_READ);
    
    LOGI("Successfully hooked CameraAPI::tryGetFOV");
    return true;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeInit(JNIEnv* env, jclass clazz) {
    if (g_initialized) {
        return JNI_TRUE;
    }
    
    LOGI("Initializing zoom mod...");
    
    GlossInit(true);
    
    if (!findAndHookCameraAPI()) {
        LOGE("Failed to hook CameraAPI");
        return JNI_FALSE;
    }
    
    g_initialized = true;
    LOGI("Zoom mod initialized successfully");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeOnKeyDown(JNIEnv* env, jclass clazz) {
    if (!g_initialized || g_zoomKeyDown) return;
    
    g_zoomKeyDown = true;
    
    if (g_animated) {
        uint64_t diff = unsignedDiff(g_lastClientZoom, g_zoomLevel);
        g_transition.startTransition(g_lastClientZoom, g_zoomLevel, clamp(100, diff / 150000, 250));
    }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeOnKeyUp(JNIEnv* env, jclass clazz) {
    if (!g_initialized || !g_zoomKeyDown) return;
    
    g_zoomKeyDown = false;
    
    if (g_animated) {
        uint64_t diff = unsignedDiff(g_lastClientZoom, g_zoomLevel);
        g_transition.startTransition(g_zoomLevel, g_lastClientZoom, clamp(100, diff / 150000, 250));
    }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeOnScroll(JNIEnv* env, jclass clazz, jfloat delta) {
    if (!g_initialized || !g_zoomKeyDown) return;
    
    if (delta > 0) {
        if (g_zoomLevel > 5310000000ULL) {
            if (g_animated) {
                g_transition.startTransition(g_zoomLevel, g_zoomLevel - 1000000, 100);
            }
            g_zoomLevel -= 1000000;
        }
    } else if (delta < 0) {
        if (g_zoomLevel < 5360000000ULL) {
            if (g_animated) {
                g_transition.startTransition(g_zoomLevel, g_zoomLevel + 1000000, 100);
            }
            g_zoomLevel += 1000000;
        }
    }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeSetAnimated(JNIEnv* env, jclass clazz, jboolean animated) {
    g_animated = animated;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeIsZooming(JNIEnv* env, jclass clazz) {
    return g_zoomKeyDown ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeSetZoomLevel(JNIEnv* env, jclass clazz, jlong level) {
    g_zoomLevel = static_cast<uint64_t>(level);
}

JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeGetZoomLevel(JNIEnv* env, jclass clazz) {
    return static_cast<jlong>(g_zoomLevel);
}

}
