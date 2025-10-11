#include "logger/logger.h"
#include <jni.h>

extern "C" JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_util_Logger_nativeCreateLogger(JNIEnv *env,
                                                          jobject thiz,
                                                          jstring name) {
  const char *nativeName = env->GetStringUTFChars(name, nullptr);
  if (nativeName == nullptr) {
    return 0;
  }
  auto *logger = new Logger(nativeName);
  env->ReleaseStringUTFChars(name, nativeName);
  return reinterpret_cast<jlong>(logger);
}

extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_util_Logger_nativeDestroyLogger(
    JNIEnv *env, jobject thiz, jlong native_logger_ptr) {
  auto *logger = reinterpret_cast<Logger *>(native_logger_ptr);
  delete logger;
}

extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_util_Logger_nativeInfo(JNIEnv *env, jobject thiz,
                                                  jlong native_logger_ptr,
                                                  jstring msg) {
  auto *logger = reinterpret_cast<Logger *>(native_logger_ptr);
  const char *nativeMsg = env->GetStringUTFChars(msg, nullptr);
  logger->info("%s", nativeMsg);
  env->ReleaseStringUTFChars(msg, nativeMsg);
}

extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_util_Logger_nativeError(JNIEnv *env, jobject thiz,
                                                   jlong native_logger_ptr,
                                                   jstring msg) {
  auto *logger = reinterpret_cast<Logger *>(native_logger_ptr);
  const char *nativeMsg = env->GetStringUTFChars(msg, nullptr);
  logger->error("%s", nativeMsg);
  env->ReleaseStringUTFChars(msg, nativeMsg);
}

extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_util_Logger_nativeWarn(JNIEnv *env, jobject thiz,
                                                  jlong native_logger_ptr,
                                                  jstring msg) {
  auto *logger = reinterpret_cast<Logger *>(native_logger_ptr);
  const char *nativeMsg = env->GetStringUTFChars(msg, nullptr);
  logger->warn("%s", nativeMsg);
  env->ReleaseStringUTFChars(msg, nativeMsg);
}

extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_util_Logger_nativeDebug(JNIEnv *env, jobject thiz,
                                                   jlong native_logger_ptr,
                                                   jstring msg) {
  auto *logger = reinterpret_cast<Logger *>(native_logger_ptr);
  const char *nativeMsg = env->GetStringUTFChars(msg, nullptr);
  logger->debug("%s", nativeMsg);
  env->ReleaseStringUTFChars(msg, nativeMsg);
}
