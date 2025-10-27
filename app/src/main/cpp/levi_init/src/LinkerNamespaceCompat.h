#pragma once

#include <string>
#include <vector>

#include "LinkerNamespaces.h"
#include "LinkerNamespaces_8.h"
#include "LinkerNamespaces_10.h"
#include "LinkerNamespaces_11.h"
#include "LinkerNamespaces_12.h"
#include "ElfUtils.h"

namespace ns_compat {

    inline int android_api() {
        return elf_util::getAndroidSystemVersion();
    }

    inline int pick_version() {
        const int api = android_api();
        if (api >= 31) return 12;      // Android 12+
        if (api >= 30) return 11;      // Android 11
        if (api >= 29) return 10;      // Android 10
        return 8;                       // Android 8/9 fallback
    }

    inline const char* ns_get_name(android_namespace_t* ns) {
        switch (pick_version()) {
            case 12: return reinterpret_cast<android_namespace_t_12*>(ns)->get_name();
            case 11: return reinterpret_cast<android_namespace_t_11*>(ns)->get_name();
            case 10: return reinterpret_cast<android_namespace_t_10*>(ns)->get_name();
            default: return reinterpret_cast<android_namespace_t_8*>(ns)->get_name();
        }
    }

    inline void ns_set_isolated(android_namespace_t* ns, bool isolated) {
        switch (pick_version()) {
            case 12: reinterpret_cast<android_namespace_t_12*>(ns)->set_isolated(isolated); break;
            case 11: reinterpret_cast<android_namespace_t_11*>(ns)->set_isolated(isolated); break;
            case 10: reinterpret_cast<android_namespace_t_10*>(ns)->set_isolated(isolated); break;
            default: reinterpret_cast<android_namespace_t_8*>(ns)->set_isolated(isolated); break;
        }
    }

    inline std::vector<std::string> ns_get_ld_library_paths(android_namespace_t* ns) {
        switch (pick_version()) {
            case 12: return reinterpret_cast<android_namespace_t_12*>(ns)->get_ld_library_paths();
            case 11: return reinterpret_cast<android_namespace_t_11*>(ns)->get_ld_library_paths();
            case 10: return reinterpret_cast<android_namespace_t_10*>(ns)->get_ld_library_paths();
            default: return reinterpret_cast<android_namespace_t_8*>(ns)->get_ld_library_paths();
        }
    }

    inline void ns_set_ld_library_paths(android_namespace_t* ns, std::vector<std::string>&& paths) {
        switch (pick_version()) {
            case 12: reinterpret_cast<android_namespace_t_12*>(ns)->set_ld_library_paths(std::move(paths)); break;
            case 11: reinterpret_cast<android_namespace_t_11*>(ns)->set_ld_library_paths(std::move(paths)); break;
            case 10: reinterpret_cast<android_namespace_t_10*>(ns)->set_ld_library_paths(std::move(paths)); break;
            default: reinterpret_cast<android_namespace_t_8*>(ns)->set_ld_library_paths(std::move(paths)); break;
        }
    }

} // namespace ns_compat