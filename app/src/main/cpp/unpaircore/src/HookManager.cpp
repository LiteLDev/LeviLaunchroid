#include "HookManager.h"
#include "ArmDecode.h"
#include "pl/Logger.h"
#include "pl/Signature.h"
#include "Hook.h"
#include "minecraftTitle.h"
#include <span>
#include <array>
#include <string_view>

namespace {
    struct Data {
        const char * a1;
        int a2 = 0;
        int a3 = 0;
        void * a4 = nullptr;
        int a5 = 0;
        std::vector<void *> a6;
        int a7 = 0;
        double a8 = 0.0;
        double a9 = 0.0;
        double a10 = 0.0;
        double a11 = 0.0;

        void reset() {
            a1 = "Minecraft";
            a2 = a3 = a5 = a7 = 0;
            a4 = nullptr;
            a6.clear();
            a8 = a9 = a10 = a11 = 0.0;
        }
    };

    pl::log::Logger logger("pairipcore");
    static Data* defaultData = new Data{};
}

SKY_STATIC_HOOK(
        MyHook1,
        memory::HookPriority::Normal,
        "_ZN9Microsoft12Applications6Events19TelemetrySystemBase5startEv",
        "libmaesdk.so",
        void, void *a1
) {
}

SKY_STATIC_HOOK(
        MyHook2,
        memory::HookPriority::Normal,
        "? ? ? D1 ? ? ? A9 ? ? ? A9 ? ? ? A9 ? ? ? A9 ? ? ? A9 ? ? ? A9 ? ? ? 91 ? ? ? D5 ? ? ? F0 F4 03 02 AA",
        "libminecraftpe.so",
        void, void *a1, Data* scopedData, void* threadId
) {
    defaultData->reset();
    origin(a1, defaultData, threadId);
}

namespace core {

    void patchMinecraftLogo() {
        uintptr_t code_addr = [] {
            constexpr std::array<std::string_view, 2> signatures = {
                    "? ? ? ? ? ? ? 91 ? ? ? 91 ? ? ? D1 E1 03 1F 2A ? ? ? 72",
                    "? ? ? D0 ? ? ? 52 ? ? ? 91 ? ? ? D1 ? ? ? 91 ? ? ? 72"
            };
            for (auto sig : signatures) {
                if (auto addr = pl::signature::pl_resolve_signature(sig.data(), "libminecraftpe.so"); addr != 0)
                    return addr;
            }
            return static_cast<uintptr_t>(0);
        }();

        if (code_addr == 0) {
            logger.error("Failed to resolve Minecraft logo signature.");
            return;
        }

        const uint32_t adrp = *reinterpret_cast<const uint32_t*>(code_addr);
        const uintptr_t pc = code_addr;

        uint32_t add = 0;
        for (int i = 1; i <= 3; ++i) {
            uint32_t next = *reinterpret_cast<const uint32_t*>(code_addr + i * 4);
            if ((next & 0xFFC00000) == 0x91000000) {
                add = next;
                break;
            }
        }

        if (add == 0) return;

        const uintptr_t page_base = ArmDecode::decodeADRP(adrp, pc);
        const uintptr_t offset = ArmDecode::decodeADD(add);
        const uintptr_t gMinecraftLogoImage_addr = page_base + offset;

        if (gMinecraftLogoImage_addr == 0) {
            logger.error("Invalid image data or address.");
            return;
        }

        auto img_data = std::span<uint8_t>(
                const_cast<uint8_t*>(reinterpret_cast<const uint8_t*>(gMinecraftLogoImage_addr)),
                sizeof(title_png)
        );
        std::copy_n(title_png, sizeof(title_png), img_data.begin());
    }

    void setupHooks() {
        MyHook1::hook();
        MyHook2::hook();
    }
}