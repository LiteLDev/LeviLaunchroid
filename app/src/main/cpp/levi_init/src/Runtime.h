#pragma once

#include <string>
#include <vector>

namespace runtime {
    bool init(std::string_view modloaderFile);
    bool addLdLibraryPaths(std::vector<std::string>&& paths);
};


