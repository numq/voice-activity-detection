cmake_minimum_required(VERSION 3.24)
project(voice-activity-detection VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

set(CMAKE_SHARED_LIBRARY_PREFIX "")

add_library(voice-activity-detection SHARED src/Java_com_github_numq_voiceactivitydetection_fvad_NativeFvadVoiceActivityDetection.cpp)

find_package(JNI)

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    target_include_directories(voice-activity-detection PRIVATE ${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "JNI not found.")
endif ()

target_include_directories(voice-activity-detection PRIVATE include)

target_link_directories(voice-activity-detection PRIVATE bin)

target_link_libraries(voice-activity-detection PRIVATE libfvad)