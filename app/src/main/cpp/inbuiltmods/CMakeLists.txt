cmake_minimum_required(VERSION 3.22)
project(inbuiltmods LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(inbuiltmods SHARED
    src/common/transition.cpp
    src/zoom/zoom.cpp
    src/fps/fps.cpp
    src/snaplook/snaplook.cpp
)

target_include_directories(inbuiltmods PRIVATE 
    src
    ${CMAKE_CURRENT_SOURCE_DIR}/../preloader/src
)

find_library(log-lib log)
find_library(egl-lib EGL)

target_link_libraries(inbuiltmods
    PRIVATE
    preloader
    ${log-lib}
    ${egl-lib}
)
