#include <android/log.h>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <jni.h>
#include <mutex>

#include "pl/memory/Hook.hpp"
#include "pl/memory/Signature.hpp"

#define LOG_TAG "LeviGyro"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_initialized = false;
static bool g_enabled = false;

static std::atomic<float> g_pendingDeltaYaw{0.0f};
static std::atomic<float> g_pendingDeltaPitch{0.0f};

static float g_sensitivityX = 1.0f;
static float g_sensitivityY = 1.0f;
static bool g_invertX = false;
static bool g_invertY = false;
static float g_deadzone = 0.005f;

struct Vec2 {
  float x, y;
};

static void (*g_applyTurnDelta_orig)(void *, Vec2 *) = nullptr;

static void applyTurnDelta_hook(void *thisPtr, Vec2 *rotationDelta) {
  if (g_enabled && rotationDelta) {
    float deltaYaw = g_pendingDeltaYaw.exchange(0.0f);
    float deltaPitch = g_pendingDeltaPitch.exchange(0.0f);

    if (std::abs(deltaYaw) < g_deadzone) {
      deltaYaw = 0.0f;
    }
    if (std::abs(deltaPitch) < g_deadzone) {
      deltaPitch = 0.0f;
    }

    float yawContrib = deltaYaw * g_sensitivityX * (g_invertX ? -1.0f : 1.0f);
    float pitchContrib =
        deltaPitch * g_sensitivityY * (g_invertY ? -1.0f : 1.0f);

    const float RAD_TO_DEG = 57.2957795f;
    yawContrib *= RAD_TO_DEG;
    pitchContrib *= RAD_TO_DEG;

    rotationDelta->x += pitchContrib;
    rotationDelta->y += yawContrib;
  }

  if (g_applyTurnDelta_orig) {
    g_applyTurnDelta_orig(thisPtr, rotationDelta);
  }
}

static constexpr const char *APPLY_TURN_DELTA_SIG =
    "? ? ? D1 ? ? ? FD ? ? ? A9 ? ? ? A9 ? ? ? A9 ? ? ? A9 ? ? ? 91 ? ? ? D5 "
    "F3 03 00 AA F4 03 01 AA ? ? ? F9 ? ? ? F8 ? ? ? F9 ? ? ? F9 ? ? ? F9";

static uintptr_t g_applyTurnDeltaTarget = 0;

static bool findAndHookApplyTurnDelta() {
  uintptr_t target = g_applyTurnDeltaTarget;
  if (target == 0) {
    target = pl::memory::resolveSignature(APPLY_TURN_DELTA_SIG, "libminecraftpe.so");
  }

  if (target == 0) {
    LOGE("Failed to resolve LocalPlayer::applyTurnDelta");
    return false;
  }

  if (pl::memory::hook(
          reinterpret_cast<void *>(target),
          reinterpret_cast<void *>(applyTurnDelta_hook),
          reinterpret_cast<void **>(&g_applyTurnDelta_orig)) != 0) {
    LOGE("Failed to install applyTurnDelta hook at 0x%lx",
         (unsigned long)target);
    return false;
  }

  LOGI("Successfully hooked LocalPlayer::applyTurnDelta at 0x%lx",
       (unsigned long)target);
  return true;
}

extern "C" {

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativePreResolve(
    JNIEnv *env, jclass clazz) {
  if (g_applyTurnDeltaTarget == 0) {
    g_applyTurnDeltaTarget = pl::memory::resolveSignature(APPLY_TURN_DELTA_SIG, "libminecraftpe.so");
  }
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeInit(
    JNIEnv *env, jclass clazz) {
  if (g_initialized) {
    return JNI_TRUE;
  }

  LOGI("Initializing gyro mod...");

  if (!findAndHookApplyTurnDelta()) {
    LOGE("Failed to hook applyTurnDelta");
    return JNI_FALSE;
  }

  g_initialized = true;
  LOGI("Gyro mod initialized successfully");
  return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeSetEnabled(
    JNIEnv *env, jclass clazz, jboolean enabled) {
  g_enabled = enabled;
  if (!enabled) {
    g_pendingDeltaYaw.store(0.0f);
    g_pendingDeltaPitch.store(0.0f);
  }
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeUpdateDelta(
    JNIEnv *env, jclass clazz, jfloat deltaYaw, jfloat deltaPitch) {
  if (!g_initialized || !g_enabled)
    return;

  float oldYaw = g_pendingDeltaYaw.load();
  while (!g_pendingDeltaYaw.compare_exchange_weak(oldYaw, oldYaw + deltaYaw))
    ;

  float oldPitch = g_pendingDeltaPitch.load();
  while (
      !g_pendingDeltaPitch.compare_exchange_weak(oldPitch, oldPitch + deltaPitch))
    ;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeSetSensitivityX(
    JNIEnv *env, jclass clazz, jfloat sensitivity) {
  g_sensitivityX = sensitivity;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeSetSensitivityY(
    JNIEnv *env, jclass clazz, jfloat sensitivity) {
  g_sensitivityY = sensitivity;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeSetInvertX(
    JNIEnv *env, jclass clazz, jboolean invert) {
  g_invertX = invert;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeSetInvertY(
    JNIEnv *env, jclass clazz, jboolean invert) {
  g_invertY = invert;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeSetDeadzone(
    JNIEnv *env, jclass clazz, jfloat deadzone) {
  g_deadzone = deadzone;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeIsInitialized(
    JNIEnv *env, jclass clazz) {
  return g_initialized ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_GyroMod_nativeIsEnabled(
    JNIEnv *env, jclass clazz) {
  return g_enabled ? JNI_TRUE : JNI_FALSE;
}
}
