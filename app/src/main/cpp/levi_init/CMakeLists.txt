cmake_minimum_required(VERSION 3.22)
project(levi_init LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(levi_init SHARED
        src/ElfUtils.cpp
        src/Init.cpp
        src/Runtime.cpp
)

target_include_directories(levi_init PRIVATE src)

target_link_libraries(levi_init
        PRIVATE
        android
        log
        dl
)