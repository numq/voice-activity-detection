cmake_minimum_required(VERSION 3.24)
project(fvadjni VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

add_library(fvadjni SHARED src/com_github_numq_kvad_fvad_FVAD.cpp)

target_link_directories(fvadjni PRIVATE bin)

target_include_directories(fvadjni PRIVATE include)

find_package(JNI)

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    target_include_directories(fvadjni PRIVATE ${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "JNI not found.")
endif ()

target_link_libraries(fvadjni PRIVATE libfvad)