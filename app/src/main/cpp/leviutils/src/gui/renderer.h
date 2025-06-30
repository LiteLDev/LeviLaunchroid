#pragma once
#include <jni.h>
#include <memory>
#include <string_view>

namespace gui {

extern int g_ScreenWidth;
extern int g_ScreenHeight;
extern bool g_Initialized;
extern JNIEnv *env;

void setJavaVM(JavaVM *jvm);

class LogWindow {
public:
  explicit LogWindow(std::string_view title);

  void clear();
  void addLog(std::string_view log);
  void draw();
  bool isOpen() const;
  void setOpen(bool open);

private:
  void renderContent();
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

void SetupRender();
void Render();
void Resize(int width, int height);
void Init();
void notifyWindowClosed();

LogWindow *GetLogWindow();

} // namespace gui