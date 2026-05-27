cmake_minimum_required(VERSION 3.22)
project(levi LANGUAGES CXX)

set(CMAKE_BUILD_TYPE RelWithDebInfo CACHE STRING "Build type" FORCE)
set(BUILD_SHARED_LIBS ON CACHE BOOL "Build shared libraries" FORCE)

if(ANDROID)
    set(CMAKE_POSITION_INDEPENDENT_CODE ON)
    add_link_options("-Wl,-z,max-page-size=16384")
endif()

add_subdirectory(preloader)
add_subdirectory(inbuiltmods)
add_subdirectory(libHttpClient/Build/libHttpClient.Android)
