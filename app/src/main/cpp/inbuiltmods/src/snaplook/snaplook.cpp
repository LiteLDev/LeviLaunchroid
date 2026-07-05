#include <android/log.h>
#include <cinttypes>
#include <cstdint>
#include <jni.h>

#include "pl/memory/Hook.hpp"
#include "pl/memory/Vtable.hpp"

#define LOG_TAG "LeviSnaplook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_initialized = false;
static bool g_snaplookActive = false;

static int (*g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig)(void *) =
    nullptr;

static int VanillaCameraAPI_getPlayerViewPerspectiveOption_hook(void *thisPtr) {
  if (g_snaplookActive) {
    return 2;
  }
  if (g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig) {
    return g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig(thisPtr);
  }
  return 0;
}

static bool findAndHookVanillaCameraAPI() {
  const uintptr_t target = pl::memory::resolveVtableFunction(
      "16VanillaCameraAPI", 7, "libminecraftpe.so");
  if (target == 0) {
    LOGE("Failed to resolve VanillaCameraAPI::getPlayerViewPerspectiveOption: "
         "0x%" PRIxPTR,
         target);
    return false;
  }

  if (pl::memory::hook(
          reinterpret_cast<void *>(target),
          reinterpret_cast<void *>(
              VanillaCameraAPI_getPlayerViewPerspectiveOption_hook),
          reinterpret_cast<void **>(
              &g_VanillaCameraAPI_getPlayerViewPerspectiveOption_orig)) != 0) {
    LOGE("Failed to install VanillaCameraAPI::getPlayerViewPerspectiveOption "
         "hook at 0x%" PRIxPTR,
         target);
    return false;
  }

  LOGI("Successfully hooked VanillaCameraAPI::getPlayerViewPerspectiveOption");
  return true;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeInit(
    JNIEnv *env, jclass clazz) {
  if (g_initialized) {
    return JNI_TRUE;
  }

  LOGI("Initializing snaplook mod...");

  if (!findAndHookVanillaCameraAPI()) {
    LOGE("Failed to hook VanillaCameraAPI");
    return JNI_FALSE;
  }

  g_initialized = true;
  LOGI("Snaplook mod initialized successfully");
  return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeOnKeyDown(
    JNIEnv *env, jclass clazz) {
  if (!g_initialized)
    return;
  g_snaplookActive = true;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeOnKeyUp(
    JNIEnv *env, jclass clazz) {
  if (!g_initialized)
    return;
  g_snaplookActive = false;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeIsActive(
    JNIEnv *env, jclass clazz) {
  return g_snaplookActive ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_SnaplookMod_nativeIsInitialized(
    JNIEnv *env, jclass clazz) {
  return g_initialized ? JNI_TRUE : JNI_FALSE;
}
}
