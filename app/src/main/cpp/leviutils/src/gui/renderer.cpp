#include "renderer.h"
#include "ImGuiAnsiColor.h"
#include "gui.h"

#include <imgui/imgui.h>
#include <imgui/imgui_internal.h>
#include <vector>

float GetScreenDensity(JNIEnv *env, jobject activity) {
  jclass activityClass = env->GetObjectClass(activity);
  jmethodID getResources = env->GetMethodID(
      activityClass, "getResources", "()Landroid/content/res/Resources;");
  jobject resources = env->CallObjectMethod(activity, getResources);

  jclass resourcesClass = env->GetObjectClass(resources);
  jmethodID getDisplayMetrics = env->GetMethodID(
      resourcesClass, "getDisplayMetrics", "()Landroid/util/DisplayMetrics;");
  jobject displayMetrics = env->CallObjectMethod(resources, getDisplayMetrics);

  jclass metricsClass = env->GetObjectClass(displayMetrics);
  jfieldID densityDpi = env->GetFieldID(metricsClass, "densityDpi", "I");
  jint dpi = env->GetIntField(displayMetrics, densityDpi);

  return (float)dpi;
}

jobject GetGlobalContext(JNIEnv *env) {
  jclass activity_thread = env->FindClass("android/app/ActivityThread");
  jmethodID current_activity_thread =
      env->GetStaticMethodID(activity_thread, "currentActivityThread",
                             "()Landroid/app/ActivityThread;");
  jobject at =
      env->CallStaticObjectMethod(activity_thread, current_activity_thread);
  jmethodID get_application = env->GetMethodID(
      activity_thread, "getApplication", "()Landroid/app/Application;");
  jobject context = env->CallObjectMethod(at, get_application);
  if (env->ExceptionCheck())
    env->ExceptionClear();
  return context;
}

namespace gui {
JNIEnv *env;
static JavaVM *g_jvm = nullptr;

static ImGuiWindow *g_imguiWin = nullptr;
int g_ScreenWidth = 0;
int g_ScreenHeight = 0;
bool g_Initialized = false;

static std::unique_ptr<LogWindow> logWindowPtr = nullptr;

struct LogWindow::Impl {
  std::string title;
  bool open{false};
  ImGuiTextBuffer buf;
  ImGuiTextFilter filter;
  std::vector<int> lineOffsets;
  bool autoScroll{true};

  Impl(std::string_view title) : title(title) { clear(); }

  void clear() {
    buf.clear();
    lineOffsets.clear();
    lineOffsets.push_back(0);
  }

  void addLog(std::string_view log) {
    if (log.empty())
      return;

    int oldSize = buf.size();
    buf.append(log.data(), log.data() + log.size());
    for (int newSize = buf.size(); oldSize < newSize; ++oldSize) {
      if (buf[oldSize] == '\n')
        lineOffsets.push_back(oldSize + 1);
    }
  }

  void renderContent() {
    bool should_clear = ImGui::Button("Clear");
    ImGui::SameLine();
    bool should_copy = ImGui::Button("Copy");
    ImGui::SameLine();
    ImGui::Checkbox("Auto-scroll", &autoScroll);
    ImGui::SameLine();
    filter.Draw("Filter", -100.0f);

    ImGui::Separator();

    ImGui::BeginChild("scrolling", ImVec2(0, 0), false,
                      ImGuiWindowFlags_HorizontalScrollbar);
    if (should_clear)
      clear();
    if (should_copy)
      ImGui::LogToClipboard();

    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0, 0));

    const char *bufBegin = buf.begin();
    const char *bufEnd = buf.end();

    auto lineCount = (int)lineOffsets.size();
    ImGuiListClipper clipper;
    clipper.Begin(lineCount);

    while (clipper.Step()) {
      for (int lineNum = clipper.DisplayStart; lineNum < clipper.DisplayEnd;
           ++lineNum) {
        const char *lineStart = bufBegin + lineOffsets[lineNum];
        const char *lineEnd = (lineNum + 1 < lineCount)
                                  ? bufBegin + lineOffsets[lineNum + 1] - 1
                                  : bufEnd;

        if (filter.IsActive() && !filter.PassFilter(lineStart, lineEnd))
          continue;
        ImGui::PushTextWrapPos(0.0f);
        textAnsiUnformatted(std::string_view(lineStart, lineEnd - lineStart));
        ImGui::PopTextWrapPos();
      }
    }
    clipper.End();

    ImGui::PopStyleVar();

    if (autoScroll && ImGui::GetScrollY() >= ImGui::GetScrollMaxY())
      ImGui::SetScrollHereY(1.0f);

    ImGui::EndChild();
  }
};

LogWindow::LogWindow(std::string_view title)
    : impl_(std::make_unique<Impl>(title)) {}

void LogWindow::clear() { impl_->clear(); }
void LogWindow::addLog(std::string_view log) { impl_->addLog(log); }
bool LogWindow::isOpen() const { return impl_->open; }
void LogWindow::setOpen(bool open) { impl_->open = open; }

void LogWindow::draw() {
  if (!impl_->open)
    return;

  ImGui::SetNextWindowSize(ImVec2(1200, 600), ImGuiCond_FirstUseEver);

  bool wasOpen = impl_->open;
  if (!ImGui::Begin(impl_->title.c_str(), &impl_->open)) {
    ImGui::End();
    if (wasOpen && !impl_->open) {
      notifyWindowClosed();
    }
    return;
  }
  
  if (wasOpen && !impl_->open) {
    notifyWindowClosed();
  }
  
  g_imguiWin = ImGui::GetCurrentWindow();
  impl_->renderContent();
  ImGui::End();
}

void SetupRender() {
  if (g_Initialized)
    return;

  IMGUI_CHECKVERSION();
  ImGui::CreateContext();
  ImGuiIO &io = ImGui::GetIO();

  ImGui::StyleColorsDark();

  ImGui_ImplAndroid_Init(nullptr);
  ImGui_ImplOpenGL3_Init("#version 300 es");

  ImFontConfig font_cfg;
  font_cfg.SizePixels = 24.0f;
  io.Fonts->AddFontDefault(&font_cfg);
  ImGui::GetStyle().ScaleAllSizes(4.0f);

  g_Initialized = true;
}

void Init() { logWindowPtr = std::make_unique<LogWindow>("Log"); }

void setJavaVM(JavaVM *jvm) { g_jvm = jvm; }

void Render() {
  if (!logWindowPtr) {
    return;
  }

  ImGuiIO &io = ImGui::GetIO();

  ImGui_ImplOpenGL3_NewFrame();
  ImGui_ImplAndroid_NewFrame(g_ScreenWidth, g_ScreenHeight);
  ImGui::NewFrame();

  logWindowPtr->draw();

  ImGui::Render();
  glClear(GL_COLOR_BUFFER_BIT);
  ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
}

void Resize(int width, int height) {
  g_ScreenWidth = width;
  g_ScreenHeight = height;
  glViewport(0, 0, width, height);
  ImGui::GetIO().DisplaySize = ImVec2((float)width, (float)height);
}

LogWindow *GetLogWindow() {
  assert(logWindowPtr != nullptr && "LogWindow not initialized!");
  return logWindowPtr.get();
}

ImGuiWindow *GetImguiWindow() { return g_imguiWin; }

void notifyWindowClosed() {
  if (g_jvm) {
    JNIEnv *currentEnv = nullptr;
    if (g_jvm->GetEnv(reinterpret_cast<void **>(&currentEnv), JNI_VERSION_1_6) == JNI_OK && currentEnv) {
      jclass logOverlayClass = currentEnv->FindClass("org/levimc/launcher/service/LogOverlay");
      if (logOverlayClass) {
        jmethodID getInstanceMethod = currentEnv->GetStaticMethodID(logOverlayClass, "getInstance", "(Landroid/content/Context;)Lorg/levimc/launcher/service/LogOverlay;");
        if (getInstanceMethod) {
          jobject context = GetGlobalContext(currentEnv);
          if (context) {
            jobject logOverlayInstance = currentEnv->CallStaticObjectMethod(logOverlayClass, getInstanceMethod, context);
            if (logOverlayInstance) {
              jmethodID hideMethod = currentEnv->GetMethodID(logOverlayClass, "hide", "()V");
              if (hideMethod) {
                currentEnv->CallVoidMethod(logOverlayInstance, hideMethod);
              }
            }
          }
        }
      }
      if (currentEnv->ExceptionCheck()) {
        currentEnv->ExceptionClear();
      }
    }
  }
}

} // namespace gui