#include <jni.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <chrono>
#include <atomic>

#include "pl/Gloss.h"

#define LOG_TAG "LeviFPS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_initialized = false;
static std::atomic<int> g_frameCount{0};
static std::atomic<int> g_currentFps{0};
static std::chrono::steady_clock::time_point g_lastFpsTime;
static EGLContext g_targetContext = EGL_NO_CONTEXT;
static EGLSurface g_targetSurface = EGL_NO_SURFACE;

static EGLBoolean (*g_orig_eglSwapBuffers)(EGLDisplay, EGLSurface) = nullptr;

static EGLBoolean hook_eglSwapBuffers(EGLDisplay dpy, EGLSurface surf) {
    if (!g_orig_eglSwapBuffers) return EGL_FALSE;

    EGLContext ctx = eglGetCurrentContext();
    if (ctx == EGL_NO_CONTEXT) return g_orig_eglSwapBuffers(dpy, surf);

    EGLint w = 0, h = 0;
    eglQuerySurface(dpy, surf, EGL_WIDTH, &w);
    eglQuerySurface(dpy, surf, EGL_HEIGHT, &h);

    if (w < 500 || h < 500) return g_orig_eglSwapBuffers(dpy, surf);

    if (g_targetContext == EGL_NO_CONTEXT) {
        EGLint buf = 0;
        eglQuerySurface(dpy, surf, EGL_RENDER_BUFFER, &buf);
        if (buf == EGL_BACK_BUFFER) {
            g_targetContext = ctx;
            g_targetSurface = surf;
        }
    }

    if (ctx == g_targetContext && surf == g_targetSurface) {
        g_frameCount++;

        auto now = std::chrono::steady_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - g_lastFpsTime).count();

        if (elapsed >= 1000) {
            g_currentFps = static_cast<int>(g_frameCount * 1000 / elapsed);
            g_frameCount = 0;
            g_lastFpsTime = now;
        }
    }

    return g_orig_eglSwapBuffers(dpy, surf);
}

static bool hookEglSwapBuffers() {
    GHandle hEGL = GlossOpen("libEGL.so");
    if (!hEGL) {
        LOGE("Failed to open libEGL.so");
        return false;
    }

    void* swap = (void*)GlossSymbol(hEGL, "eglSwapBuffers", nullptr);
    if (!swap) {
        LOGE("Failed to find eglSwapBuffers");
        return false;
    }

    GHook h = GlossHook(swap, (void*)hook_eglSwapBuffers, (void**)&g_orig_eglSwapBuffers);
    if (!h) {
        LOGE("Failed to hook eglSwapBuffers");
        return false;
    }

    LOGI("Successfully hooked eglSwapBuffers");
    return true;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_FpsMod_nativeInit(JNIEnv* env, jclass clazz) {
    if (g_initialized) {
        return JNI_TRUE;
    }

    LOGI("Initializing FPS mod...");

    GlossInit(true);

    if (!hookEglSwapBuffers()) {
        LOGE("Failed to hook eglSwapBuffers");
        return JNI_FALSE;
    }

    g_lastFpsTime = std::chrono::steady_clock::now();
    g_initialized = true;
    LOGI("FPS mod initialized successfully");
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_FpsMod_nativeGetFps(JNIEnv* env, jclass clazz) {
    return g_currentFps.load();
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_FpsMod_nativeIsInitialized(JNIEnv* env, jclass clazz) {
    return g_initialized ? JNI_TRUE : JNI_FALSE;
}

}
