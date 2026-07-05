#include <android/log.h>
#include <cinttypes>
#include <cstdint>
#include <jni.h>

#include "common/transition.h"
#include "pl/memory/Hook.hpp"
#include "pl/memory/Vtable.hpp"

#define LOG_TAG "LeviZoom"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_initialized = false;
static bool g_zoomKeyDown = false;
static bool g_animated = true;
static uint64_t g_zoomLevel = 5345000000ULL;
static uint64_t g_lastClientZoom = 0;
static Transition g_transition;
static int g_transitionDuration = 150;

static uint64_t (*g_CameraAPI_tryGetFOV_orig)(void *) = nullptr;

static uint64_t CameraAPI_tryGetFOV_hook(void *thisPtr) {
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
  const uintptr_t target =
      pl::memory::resolveVtableFunction("9CameraAPI", 7, "libminecraftpe.so");
  if (target == 0) {
    LOGE("Failed to resolve CameraAPI::tryGetFOV: 0x%" PRIxPTR, target);
    return false;
  }

  if (pl::memory::hook(
          reinterpret_cast<void *>(target),
          reinterpret_cast<void *>(CameraAPI_tryGetFOV_hook),
          reinterpret_cast<void **>(&g_CameraAPI_tryGetFOV_orig)) != 0) {
    LOGE("Failed to install CameraAPI::tryGetFOV hook at 0x%" PRIxPTR, target);
    return false;
  }

  LOGI("Successfully hooked CameraAPI::tryGetFOV");
  return true;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeInit(
    JNIEnv *env, jclass clazz) {
  if (g_initialized) {
    return JNI_TRUE;
  }

  LOGI("Initializing zoom mod...");

  if (!findAndHookCameraAPI()) {
    LOGE("Failed to hook CameraAPI");
    return JNI_FALSE;
  }

  g_initialized = true;
  LOGI("Zoom mod initialized successfully");
  return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeOnKeyDown(
    JNIEnv *env, jclass clazz) {
  if (!g_initialized || g_zoomKeyDown)
    return;

  g_zoomKeyDown = true;

  if (g_animated && g_transitionDuration > 0) {
    g_transition.startTransition(g_lastClientZoom, g_zoomLevel,
                                 g_transitionDuration);
  } else {
    g_transition.startTransition(g_zoomLevel, g_zoomLevel,
                                 0); // instantly stop transition
  }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeOnKeyUp(
    JNIEnv *env, jclass clazz) {
  if (!g_initialized || !g_zoomKeyDown)
    return;

  g_zoomKeyDown = false;

  if (g_animated && g_transitionDuration > 0) {
    g_transition.startTransition(g_zoomLevel, g_lastClientZoom,
                                 g_transitionDuration);
  } else {
    g_transition.startTransition(g_lastClientZoom, g_lastClientZoom,
                                 0); // instantly stop transition
  }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeOnScroll(
    JNIEnv *env, jclass clazz, jfloat delta) {
  if (!g_initialized || !g_zoomKeyDown)
    return;

  uint64_t scrollAmount = 25000000ULL;

  if (delta > 0) {
    if (g_zoomLevel > 5110000000ULL + scrollAmount) {
      if (g_animated && g_transitionDuration > 0) {
        g_transition.startTransition(g_zoomLevel, g_zoomLevel - scrollAmount,
                                     g_transitionDuration);
      }
      g_zoomLevel -= scrollAmount;
    } else if (g_zoomLevel > 5110000000ULL) {
      if (g_animated && g_transitionDuration > 0) {
        g_transition.startTransition(g_zoomLevel, 5110000000ULL,
                                     g_transitionDuration);
      }
      g_zoomLevel = 5110000000ULL;
    }
  } else if (delta < 0) {
    if (g_zoomLevel < 5410000000ULL - scrollAmount) {
      if (g_animated && g_transitionDuration > 0) {
        g_transition.startTransition(g_zoomLevel, g_zoomLevel + scrollAmount,
                                     g_transitionDuration);
      }
      g_zoomLevel += scrollAmount;
    } else if (g_zoomLevel < 5410000000ULL) {
      if (g_animated && g_transitionDuration > 0) {
        g_transition.startTransition(g_zoomLevel, 5410000000ULL,
                                     g_transitionDuration);
      }
      g_zoomLevel = 5410000000ULL;
    }
  }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeSetAnimated(
    JNIEnv *env, jclass clazz, jboolean animated) {
  g_animated = animated;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeIsZooming(
    JNIEnv *env, jclass clazz) {
  return g_zoomKeyDown ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeSetZoomLevel(
    JNIEnv *env, jclass clazz, jlong level) {
  g_zoomLevel = static_cast<uint64_t>(level);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeSetTransitionDuration(
    JNIEnv *env, jclass clazz, jint duration) {
  g_transitionDuration = duration;
  g_animated = duration > 0;
}

JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ZoomMod_nativeGetZoomLevel(
    JNIEnv *env, jclass clazz) {
  return static_cast<jlong>(g_zoomLevel);
}
}
