cmake_minimum_required(VERSION 3.22)
project(memoryeditor LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(memoryeditor SHARED
    src/memory_editor.cpp
)

target_link_libraries(memoryeditor
    android
    log
)
