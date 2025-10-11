
#include "Hook.h"

#include <dlfcn.h>
#include <jni.h>
#include "minecraftTitle.h"
#include "pl/Logger.h"
#include "pl/Signature.h"

 pl::log::Logger logger("pairipcore");

typedef unsigned char stbi_uc;

SKY_STATIC_HOOK(
        MyHook, memory::HookPriority::Normal,
        "_ZN9Microsoft12Applications6Events19TelemetrySystemBase5startEv",
        "libmaesdk.so", void, void *a1) {
}


SKY_STATIC_HOOK(
        MyHook2, memory::HookPriority::Normal,
        "? ? ? D1 ? ? ? A9 ? ? ? 91 ? ? ? F9 ? ? ? D5 ? ? ? F9 ? ? ? F8 08 C0 21 8B E1 03 02 AA E2 03 03 AA E3 03 04 AA E4 03 05 2A ? ? ? A9 ? ? ? A9 E0 03 00 91 ? ? ? F9 ? ? ? B9 ? ? ? 97",
        "libminecraftpe.so",stbi_uc *,stbi_uc const *buffer, int len, int *x, int *y, int *comp, int req_comp) {
    if(len == 86796){
        return origin(title_png, len, x, y, comp, req_comp);
    }
    return origin(buffer, len, x, y, comp, req_comp);
}

extern "C" jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_6;
}

extern "C" void
ExecuteProgram(void) {
    MyHook::hook();
    MyHook2::hook();
}




