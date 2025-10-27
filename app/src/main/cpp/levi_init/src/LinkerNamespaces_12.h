/*
 * Copyright (C) 2016 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#pragma once
#include "LinkerNamespaces.h"

std::vector<std::string> fix_lib_paths(std::vector<std::string> paths) {
    return paths;
}

struct android_namespace_t_12 : public android_namespace_t {
public:
    android_namespace_t_12() :
            is_isolated_(false),
            is_exempt_list_enabled_(false),
            is_also_used_as_anonymous_(false) {}

    const char* get_name() const { return name_.c_str(); }
    void set_name(const char* name) { name_ = name; }

    bool is_isolated() const { return is_isolated_; }
    void set_isolated(bool isolated) { is_isolated_ = isolated; }

    bool is_exempt_list_enabled() const { return is_exempt_list_enabled_; }
    void set_exempt_list_enabled(bool enabled) { is_exempt_list_enabled_ = enabled; }

    bool is_also_used_as_anonymous() const { return is_also_used_as_anonymous_; }
    void set_also_used_as_anonymous(bool yes) { is_also_used_as_anonymous_ = yes; }

    const std::vector<std::string>& get_ld_library_paths() const {
        return ld_library_paths_;
    }
    void set_ld_library_paths(std::vector<std::string>&& library_paths) {
        ld_library_paths_ = std::move(library_paths);
    }

    const std::vector<std::string>& get_default_library_paths() const {
        return default_library_paths_;
    }
    void set_default_library_paths(std::vector<std::string>&& library_paths) {
        default_library_paths_ = fix_lib_paths(std::move(library_paths));
    }
    void set_default_library_paths(const std::vector<std::string>& library_paths) {
        default_library_paths_ = fix_lib_paths(library_paths);
    }

    const std::vector<std::string>& get_permitted_paths() const {
        return permitted_paths_;
    }
    void set_permitted_paths(std::vector<std::string>&& permitted_paths) {
        permitted_paths_ = std::move(permitted_paths);
    }
    void set_permitted_paths(const std::vector<std::string>& permitted_paths) {
        permitted_paths_ = permitted_paths;
    }

    const std::vector<std::string>& get_allowed_libs() const { return allowed_libs_; }
    void set_allowed_libs(std::vector<std::string>&& allowed_libs) {
        allowed_libs_ = std::move(allowed_libs);
    }
    void set_allowed_libs(const std::vector<std::string>& allowed_libs) {
        allowed_libs_ = allowed_libs;
    }

private:
    std::string name_;
    bool is_isolated_;
    bool is_exempt_list_enabled_;
    bool is_also_used_as_anonymous_;
    std::vector<std::string> ld_library_paths_;
    std::vector<std::string> default_library_paths_;
    std::vector<std::string> permitted_paths_;
    std::vector<std::string> allowed_libs_;
    // Loader looks into linked namespace if it was not able
    // to find a library in this namespace. Note that library
    // lookup in linked namespaces are limited by the list of
    // shared sonames.
    //std::vector<android_namespace_link_t> linked_namespaces_;
    //soinfo_list_t soinfo_list_;

    DISALLOW_COPY_AND_ASSIGN(android_namespace_t_12);
};