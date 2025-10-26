#include "Runtime.h"

#include <elf.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <dlfcn.h>
#include <android/log.h>
#include <link.h>
#include <unordered_map>
#include <iostream>
#include <memory>
#include <ranges>

#include "LinkerNamespaces.h"
#include "ElfUtils.h"

#define LOG_TAG "LeviInit"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace runtime {

    using elf_util::resolveElfSymbol;
    using elf_util::getAndroidLinkerPath;
    using elf_util::getAndroidSystemVersion;

    using get_realpath_t         = const char* (*)(soinfo*);
    using get_primary_namespace_t = android_namespace_t* (*)(soinfo*);

    namespace {

        struct RuntimeContext {
            std::unordered_map<uintptr_t, soinfo*>* soinfo_map{nullptr};
            get_realpath_t get_realpath{nullptr};
            get_primary_namespace_t get_primary_ns{nullptr};
            android_namespace_t* main_ns{nullptr};
        };

        RuntimeContext& getContext() {
            static RuntimeContext ctx{};
            return ctx;
        }

        inline bool makeWritable(void* addr) {
            constexpr size_t PageSize = 4096;
            auto page_base = reinterpret_cast<uintptr_t>(addr) & ~(PageSize - 1UL);
            if (mprotect(reinterpret_cast<void*>(page_base), PageSize, PROT_READ | PROT_WRITE) != 0) {
                perror("mprotect failed");
                return false;
            }
            return true;
        }
    }


    bool init(std::string_view file_name) {
        auto& ctx = getContext();
        if (ctx.main_ns)
            return true;

        const auto* linker_path = getAndroidLinkerPath();

        auto soinfo_map_sym = resolveElfSymbol(linker_path, "__dl_g_soinfo_handles_map");
        if(!soinfo_map_sym) { // <= __ANDROID_API_N_MR1__
            soinfo_map_sym = resolveElfSymbol(linker_path, "__dl__ZL20g_soinfo_handles_map");
        }
        auto get_realpath_sym = resolveElfSymbol(linker_path, "__dl__ZNK6soinfo12get_realpathEv");
        auto get_primary_ns_sym = resolveElfSymbol(linker_path, "__dl__ZN6soinfo21get_primary_namespaceEv");

        if (!soinfo_map_sym || !get_realpath_sym || !get_primary_ns_sym) {
            LOGD("Failed to resolve linker internal symbols.");
            return false;
        }

        ctx.soinfo_map = reinterpret_cast<std::unordered_map<uintptr_t, soinfo*>*>(soinfo_map_sym);
        ctx.get_realpath = reinterpret_cast<get_realpath_t>(get_realpath_sym);
        ctx.get_primary_ns = reinterpret_cast<get_primary_namespace_t>(get_primary_ns_sym);

        if (!ctx.soinfo_map) {
            LOGD("soinfo map is null.");
            return false;
        }
        for (auto&& [hdl, info] : *ctx.soinfo_map) {
            std::string_view realpath{ctx.get_realpath(info)};
            if (realpath.empty()) continue;
            if (realpath == file_name) {
                ctx.main_ns = ctx.get_primary_ns(info);
                break;
            }
        }
        if (!ctx.main_ns) {
            LOGD("Failed to find main namespace.");
            return false;
        }
        if (!makeWritable(ctx.main_ns)) {
            LOGD("Failed to make main namespace writable.");
            return false;
        }
        ctx.main_ns->set_isolated(false);
        LOGD("Main namespace: %p", ctx.main_ns->get_name());
        return true;
    }

    bool addLdLibraryPaths(std::vector<std::string>&& paths) {
        auto& ctx = getContext();
        if (!ctx.main_ns) {
            return false;
        }

        auto ldPaths = ctx.main_ns->get_ld_library_paths();
        ldPaths.insert(ldPaths.end(),
                       std::make_move_iterator(paths.begin()),
                       std::make_move_iterator(paths.end()));
        ctx.main_ns->set_ld_library_paths(std::move(ldPaths));

        return true;
    }

} // namespace runtime