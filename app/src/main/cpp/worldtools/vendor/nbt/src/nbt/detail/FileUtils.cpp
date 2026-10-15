// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#include "nbt/detail/FileUtils.hpp"
#include <fstream>

#ifdef _WIN32
#include <windows.h>
#else
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#endif

namespace nbt::detail {

void readFileMMap(std::filesystem::path const& path, std::string& content) {
#ifdef _WIN32
    HANDLE hFile = CreateFileA(
        path.string().c_str(),
        GENERIC_READ,
        FILE_SHARE_READ,
        NULL,
        OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL,
        NULL
    );
    if (hFile == INVALID_HANDLE_VALUE) { return; }
    DWORD size = GetFileSize(hFile, NULL);
    if (size == INVALID_FILE_SIZE) {
        CloseHandle(hFile);
        return;
    }
    HANDLE hMapping = CreateFileMapping(hFile, NULL, PAGE_READONLY, 0, 0, NULL);
    if (!hMapping) {
        CloseHandle(hFile);
        return;
    }
    LPVOID mapped = MapViewOfFile(hMapping, FILE_MAP_READ, 0, 0, 0);
    if (!mapped) {
        CloseHandle(hMapping);
        CloseHandle(hFile);
        return;
    }
    content.assign(static_cast<char*>(mapped), size);
    UnmapViewOfFile(mapped);
    CloseHandle(hMapping);
    CloseHandle(hFile);
#else
    int fd = ::open(path.string().c_str(), O_RDONLY);
    if (fd == -1) { return; }
    struct stat sb;
    if (::fstat(fd, &sb) == -1) {
        ::close(fd);
        return;
    }
    void* mapped = ::mmap(nullptr, static_cast<size_t>(sb.st_size), PROT_READ, MAP_PRIVATE, fd, 0);
    if (mapped == MAP_FAILED) {
        ::close(fd);
        return;
    }
    content.assign(static_cast<char*>(mapped), static_cast<size_t>(sb.st_size));
    ::munmap(mapped, static_cast<size_t>(sb.st_size));
    ::close(fd);
#endif
}

void readFile(std::filesystem::path const& path, std::string& content, bool fileMemoryMap) {
    if (fileMemoryMap) {
        readFileMMap(path, content);
    } else {
        std::ifstream fRead(path, std::ios::ate | std::ios::binary);
        if (fRead.is_open()) {
            auto size = fRead.tellg();
            fRead.seekg(0);
            content.resize(static_cast<size_t>(size));
            fRead.read(content.data(), size);
        }
    }
}

} // namespace nbt::detail
