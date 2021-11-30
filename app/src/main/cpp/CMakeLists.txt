cmake_minimum_required(VERSION 3.12)
project(src)


IF (WIN32)
   include_directories(jni_include/inc_win)
ELSEIF (APPLE)
    include_directories(jni_include/inc_mac)
ELSEIF (UNIX)
    include_directories(jni_include/inc_linux)
ELSE()
    set(JAVA_AWT_LIBRARY NotNeeded)
    set(JAVA_JVM_LIBRARY NotNeeded)
    find_package(JNI REQUIRED)
ENDIF ()

AUX_SOURCE_DIRECTORY(src SRC_DIR)
AUX_SOURCE_DIRECTORY(bind SRC_DIR)

include_directories(src interface javah)

set(CMAKE_ANDROID_ARM_MODE ON)
set(CMAKE_ANDROID_ARM_NEON ON)
set(CMAKE_ANDROID_STL_TYPE system )

add_library(silk SHARED ${SRC_DIR} bind/JNIEncoder.c bind/coder.h bind/coder.c)
