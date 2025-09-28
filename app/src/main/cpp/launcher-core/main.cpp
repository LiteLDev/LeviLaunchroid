#include <jni.h>
#include <android/native_activity.h>
#include <dlfcn.h>

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM*,void*) {
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL Java_org_levimc_launcher_core_minecraft_pesdk_nativeapi_LibraryLoader_nativeOnLauncherLoaded(JNIEnv*env, jclass thiz, jstring libPath) {
    const char * mNativeLibPath = env->GetStringUTFChars(libPath, 0);
    void* imageMCPE=(void*) dlopen(mNativeLibPath,RTLD_LAZY);
    dlclose(imageMCPE);
    env->ReleaseStringUTFChars(libPath,mNativeLibPath);
}
