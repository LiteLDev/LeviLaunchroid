#include "gui/gui.h"
#include <android/native_activity.h>

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  gui::Init();
  gui::setJavaVM(vm);

  JNIEnv *env = nullptr;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_VERSION_1_6;
  }
  gui::env = env;
  return JNI_VERSION_1_6;
}