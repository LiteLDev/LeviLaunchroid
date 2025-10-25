//
// Created by GuXiang on 2025/10/25.
//

#ifndef LEVILAUNCHER_ANDROID_LINKER_NS_H
#define LEVILAUNCHER_ANDROID_LINKER_NS_H


class android_linker_ns {

};


#endif //LEVILAUNCHER_ANDROID_LINKER_NS_H
// SPDX-License-Identifier: BSD-2-Clause
// Copyright © 2021 Billy Laws

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

// https://cs.android.com/android/platform/superproject/+/0a492a4685377d41fef2b12e9af4ebfa6feef9c2:art/libnativeloader/include/nativeloader/dlext_namespaces.h;l=25;bpv=1;bpt=1
enum {
    ANDROID_NAMESPACE_TYPE_SHARED = 2,
};

/**
 * @brief Checks if linkernsbypass loaded successfully and is safe to use
 * @note IMPORTANT: This should be called before any calls to the rest of the library are made
 * @return true if loading succeeded
 */
bool linkernsbypass_load_status();

void linkernsbypass_load(JNIEnv* env);

struct android_namespace_t *android_create_namespace_escape(const char *name,
                                                            const char *ld_library_path,
                                                            const char *default_library_path,
                                                            uint64_t type,
                                                            const char *permitted_when_isolated_path,
                                                            struct android_namespace_t *parent_namespace);

/**
 * @brief Loads a library into a namespace
 * @note IMPORTANT: If `filename` is compiled with the '-z global' linker flag and RTLD_GLOBAL is supplied in `flags` the library will be added to the namespace's LD_PRELOAD list
 * @param filename The name of the library to load
 * @param flags The rtld flags for `filename`
 * @param ns The namespace to dlopen into
 */
void *linkernsbypass_namespace_dlopen(const char *filename, int flags, struct android_namespace_t *ns);

#ifdef __cplusplus
}
#endif