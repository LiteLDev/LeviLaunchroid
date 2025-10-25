#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <android/dlext.h>
#include <string>
#include <cstring>
#include "android_linker_ns.h"
#include <dlfunc.h>

#define LOG_TAG "dlopenglobal"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
static android_namespace_t* g_ns = nullptr;

static std::string g_nativeLibDir;

extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_ModNativeLoader_initNativeLibraryDirAndModsDir(
        JNIEnv* env, jclass, jstring jNativeLibDir, jstring jModsDir) {
    if (jNativeLibDir == nullptr || jModsDir == nullptr) {
        jclass exc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(exc, "nativeLibDir or modsDir is null");
        return;
    }

    const char* nativeLibDir = env->GetStringUTFChars(jNativeLibDir, nullptr);
    const char* modsDir = env->GetStringUTFChars(jModsDir, nullptr);
    g_nativeLibDir = nativeLibDir;

    ALOGI("Initializing with nativeLibraryDir=%s, modsDir=%s",
          nativeLibDir, modsDir);

    linkernsbypass_load(env);

    g_ns = android_create_namespace_escape("levi_mod",
                                           std::string(std::string(modsDir) +":"+std::string(nativeLibDir)).c_str(),
                                           nullptr ,
                                           ANDROID_NAMESPACE_TYPE_SHARED,
                                           nullptr,nullptr );

    if (!g_ns) {
        ALOGE("Failed to create namespace (lib=%s, mods=%s)", nativeLibDir, modsDir);
        jclass exc = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exc, "Failed to create namespace with provided dirs");
    } else {
        ALOGI("Namespace created successfully.");
    }

    env->ReleaseStringUTFChars(jNativeLibDir, nativeLibDir);
    env->ReleaseStringUTFChars(jModsDir, modsDir);
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_mods_ModNativeLoader_dlopenGlobal(
        JNIEnv *env, jclass, jstring path) {
    if (path == nullptr) {
        jclass exc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(exc, "path is null");
        return 0;
    }

    const char *cpath = env->GetStringUTFChars(path, nullptr);
    std::string final_path = cpath;
    env->ReleaseStringUTFChars(path, cpath);

    if (final_path.find('/') == std::string::npos) {
        if (g_nativeLibDir.empty()) {
            jclass exc = env->FindClass("java/lang/IllegalStateException");
            env->ThrowNew(exc, "nativeLibraryDir not initialized; call initNativeLibraryDirAndModsDir first");
            return 0;
        }

        std::string name = final_path;

        if (!name.starts_with("lib")) {
            name = "lib" + name;
        }

        if (!name.ends_with(".so")) {
            name += ".so";
        }

        final_path = g_nativeLibDir + "/" + name;
    }

    if (!g_ns) {
        ALOGE("Namespace not initialized");
        jclass exc = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exc, "Namespace not initialized; call initNativeLibraryDirAndModsDir first");
        return 0;
    }

    void* handle = linkernsbypass_namespace_dlopen(final_path.c_str(), RTLD_GLOBAL, g_ns);

    if (!handle) {
        const char *err = dlerror();
        if (!err) err = "dlopen/android_dlopen_ext failed";
        ALOGE("load('%s') failed: %s", final_path.c_str(), err);
        jclass exc = env->FindClass("java/lang/UnsatisfiedLinkError");
        env->ThrowNew(exc, err);
        return 0;
    }

    ALOGI("load('%s') succeeded", final_path.c_str());
    return reinterpret_cast<jlong>(handle);
}