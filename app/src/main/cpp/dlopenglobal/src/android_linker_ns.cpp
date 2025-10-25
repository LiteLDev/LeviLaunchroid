// SPDX-License-Identifier: BSD-2-Clause
// Copyright © 2021 Billy Laws

#define LOG_TAG "linkernsbypass"

#include <dlfcn.h>
#include <android/dlext.h>
#include <unistd.h>
#include <jni.h>
#include <android/log_macros.h>
#include <pthread.h>
#include "android_linker_ns.h"
#include <sys/mman.h>

using loader_android_create_namespace_t = android_namespace_t *(*)(const char *, const char *, const char *, uint64_t, const char *, android_namespace_t *, const void *);
static loader_android_create_namespace_t loader_android_create_namespace;

static bool lib_loaded;
static bool dlfunc_loaded;

/* Public API */
bool linkernsbypass_load_status() {
    return lib_loaded;
}
struct android_namespace_t *android_create_namespace_escape(const char *name,
                                                            const char *ld_library_path,
                                                            const char *default_library_path,
                                                            uint64_t type,
                                                            const char *permitted_when_isolated_path,
                                                            android_namespace_t *parent_namespace) {
    auto caller{reinterpret_cast<void *>(&dlopen)};
    return loader_android_create_namespace(name, ld_library_path, default_library_path, type,
                                           permitted_when_isolated_path, parent_namespace,
                                           caller);
}

void *linkernsbypass_namespace_dlopen(const char *filename, int flags, android_namespace_t *ns) {
    android_dlextinfo extInfo{
            .flags = ANDROID_DLEXT_USE_NAMESPACE,
            .library_namespace = ns
    };

    return android_dlopen_ext(filename, flags, &extInfo);
}

void linkernsbypass_load(JNIEnv* env) {
    if (lib_loaded)
        return;

    if (!dlfunc_loaded && dlfunc_init(env) != JNI_OK) {
        ALOGE("dlfunc init failed");
        return;
    }
    dlfunc_loaded = true;

    if (android_get_device_api_level() < 26)
        return;

    void* libdlAndroidHandle = dlopen( "libdl_android.so", RTLD_NOW);
    if (!libdlAndroidHandle) {
        libdlAndroidHandle = dlopen( "libdl.so", RTLD_NOW);
        if (!libdlAndroidHandle) {
            ALOGE("dlfunc_dlopen of libdl_android.so failed: %s", dlerror());
            return;
        }
    }

    loader_android_create_namespace = reinterpret_cast<loader_android_create_namespace_t>(dlsym(
            libdlAndroidHandle, "__loader_android_create_namespace"));
    if (!loader_android_create_namespace) {
        ALOGE("dlsym of __loader_android_create_namespace in libdl_android.so failed: %s", dlerror());
        return;
    }

    // Lib is now safe to use
    lib_loaded = true;
}