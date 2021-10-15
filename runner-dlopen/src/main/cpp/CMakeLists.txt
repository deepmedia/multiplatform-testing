# https://developer.android.com/studio/projects/configure-cmake
# https://cmake.org/cmake/help/latest/command/project.html?highlight=project
cmake_minimum_required(VERSION 3.4.1)
project(runner-dlopen)

# Add executable
add_executable(runner main.cpp)

# Link with android libs
# find_library(ANDROID_PATH android)
# target_link_libraries(multiplatform_testing dl ${ANDROID_PATH})