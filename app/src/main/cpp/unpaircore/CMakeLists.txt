cmake_minimum_required(VERSION 3.22)
project(pairipcore LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

file(GLOB_RECURSE UNPAIRCORE_SRC CONFIGURE_DEPENDS "src/*.cpp")

add_library(pairipcore SHARED ${UNPAIRCORE_SRC})

target_include_directories(pairipcore
        PRIVATE
        src
        ${CMAKE_SOURCE_DIR}/preloader/src
)

target_link_libraries(pairipcore
        PRIVATE
        android
        log
        preloader
)