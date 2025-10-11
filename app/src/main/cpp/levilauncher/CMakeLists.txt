cmake_minimum_required(VERSION 3.22)
project(levilauncher LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

file(GLOB_RECURSE SRC_FILES "src/*.cpp")
add_library(levilauncher SHARED ${SRC_FILES})

target_include_directories(levilauncher PRIVATE src)

find_library(log-lib log)
find_library(android-lib android)
find_library(egl-lib EGL)
find_library(gles-lib GLESv3)

target_link_libraries(levilauncher
        PRIVATE
        ${log-lib}
        ${android-lib}
        ${egl-lib}
        ${gles-lib}
)