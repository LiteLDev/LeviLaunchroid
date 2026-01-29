#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdint>
#include <sys/mman.h>
#include <unistd.h>
#include <fstream>
#include <string>

#include "pl/Gloss.h"

#define LOG_TAG "LeviSnaplook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_initialized = false;
static bool g_snaplookActive = false;

static int (*g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig)(void*) = nullptr;

static int VanillaCameraAPI_getPlayerViewPerspectiveOption_hook(void* thisPtr) {
    if (g_snaplookActive) {
        return 2;
    }
    if (g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig) {
        return g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig(thisPtr);
    }
    return 0;
}

static bool findAndHookVanillaCameraAPI() {
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

    const char* typeinfoName = "16VanillaCameraAPI";
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
        LOGE("Failed to find VanillaCameraAPI typeinfo name");
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
        LOGE("Failed to find VanillaCameraAPI typeinfo");
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
        LOGE("Failed to find VanillaCameraAPI vtable");
        return false;
    }

    uintptr_t* perspectiveSlot = (uintptr_t*)(vtableAddr + 7 * sizeof(void*));
    g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig = (int(*)(void*))(*perspectiveSlot);

    LOGI("Original getPlayerViewPerspectiveOption at 0x%lx", (uintptr_t)g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig);

    uintptr_t pageStart = (uintptr_t)perspectiveSlot & ~(4095UL);
    if (mprotect((void*)pageStart, 4096, PROT_READ | PROT_WRITE) != 0) {
        LOGE("Failed to make vtable writable");
        return false;
    }

    *perspectiveSlot = (uintptr_t)VanillaCameraAPI_getPlayerViewPerspectiveOption_hook;

    mprotect((void*)pageStart, 4096, PROT_READ);

    LOGI("Successfully hooked VanillaCameraAPI::getPlayerViewPerspectiveOption");
    return true;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeInit(JNIEnv* env, jclass clazz) {
    if (g_initialized) {
        return JNI_TRUE;
    }

    LOGI("Initializing snaplook mod...");

    GlossInit(true);

    if (!findAndHookVanillaCameraAPI()) {
        LOGE("Failed to hook VanillaCameraAPI");
        return JNI_FALSE;
    }

    g_initialized = true;
    LOGI("Snaplook mod initialized successfully");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeOnKeyDown(JNIEnv* env, jclass clazz) {
    if (!g_initialized) return;
    g_snaplookActive = true;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeOnKeyUp(JNIEnv* env, jclass clazz) {
    if (!g_initialized) return;
    g_snaplookActive = false;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeIsActive(JNIEnv* env, jclass clazz) {
    return g_snaplookActive ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeIsInitialized(JNIEnv* env, jclass clazz) {
    return g_initialized ? JNI_TRUE : JNI_FALSE;
}

}
