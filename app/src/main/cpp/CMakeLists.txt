cmake_minimum_required(VERSION 3.22)
project(levi LANGUAGES CXX)
set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)

add_subdirectory(unpaircore)
add_subdirectory(preloader)
add_subdirectory(levi_init)
