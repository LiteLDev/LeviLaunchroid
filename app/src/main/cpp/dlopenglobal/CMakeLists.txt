cmake_minimum_required(VERSION 3.22)
project(dlopenglobal LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)


find_package(dlfunc REQUIRED CONFIG)


file(GLOB_RECURSE DLOPEN_SRC CONFIGURE_DEPENDS "src/*.cpp")
add_library(dlopenglobal SHARED ${DLOPEN_SRC})

target_include_directories(dlopenglobal PRIVATE src)

target_link_libraries(dlopenglobal
        PRIVATE
        android
        log
        dl
        dlfunc::dlfunc
)