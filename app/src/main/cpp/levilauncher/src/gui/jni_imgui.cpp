#include "gui.h"

namespace gui {
extern ImGuiWindow *GetImguiWindow();
}

extern "C" {
JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_onDrawFrame(JNIEnv *env,
                                                                 jclass clazz) {
  gui::Render();
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_onSurfaceCreated(
    JNIEnv *env, jclass clazz) {
  gui::SetupRender();
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_onSurfaceChanged(
    JNIEnv *env, jclass clazz, jint width, jint height) {
  gui::Resize(width, height);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_onDetachedFromWindow(
    JNIEnv *env, jclass clazz) {
  if (!gui::g_Initialized) {
    return;
  }
  // Cleanup
  ImGui_ImplOpenGL3_Shutdown();
  ImGui_ImplAndroid_Shutdown();
  ImGui::DestroyContext();
  gui::g_Initialized = false;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_MotionEventClick(
    JNIEnv *env, jclass clazz, jboolean down, jfloat pos_x, jfloat pos_y) {
  ImGuiIO &io = ImGui::GetIO();
  io.MouseDown[0] = down;
  io.MousePos = ImVec2(pos_x, pos_y);
}

JNIEXPORT jstring JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_GetWindowRect(
    JNIEnv *env, jclass clazz) {
  char result[256] = "0|0|0|0";

  ImGuiWindow *win = gui::GetImguiWindow();
  if (win) {
    sprintf(result, "%d|%d|%d|%d", (int)win->Pos.x, (int)win->Pos.y,
            (int)win->Size.x, (int)win->Size.y);
  }
  return env->NewStringUTF(result);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_SetWindowSize(JNIEnv *env,
                                                                   jclass clazz,
                                                                   jint w,
                                                                   jint h) {
  gui::g_ScreenWidth = w;
  gui::g_ScreenHeight = h;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_AddLog(JNIEnv *env,
                                                            jclass clazz,
                                                            jstring msg) {

  const char *nativeMsg = env->GetStringUTFChars(msg, nullptr);
  gui::GetLogWindow()->addLog(nativeMsg);
  env->ReleaseStringUTFChars(msg, nativeMsg);
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_service_imgui_NativeMethods_SetOpen(JNIEnv *env,
                                                             jclass clazz,
                                                             jboolean isopen) {

  gui::GetLogWindow()->setOpen(isopen);
}
}
