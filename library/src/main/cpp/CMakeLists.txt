cmake_minimum_required(VERSION 3.24)
project(vad VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

add_library(vad SHARED src/Java_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection.cpp)

find_package(JNI)

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    target_include_directories(vad PRIVATE ${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "JNI not found.")
endif ()

target_include_directories(vad PRIVATE include)

target_link_directories(vad PRIVATE bin)

target_link_libraries(vad PRIVATE libfvad)