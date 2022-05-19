cmake_minimum_required(VERSION 3.21)
project(MinaCalc_Native_FNUtils)

set(CMAKE_CXX_STANDARD 20)

# JNI Directory stuff
include_directories(
        "lib/jdk-include/include"
        "lib/jdk-include/include/win32"
)

# Creates the .dll for the native sources
add_library(
        MinaCalc_Native_FNUtils
        SHARED
        lib/0.71.0/MinaCalc/MinaCalc.h
        lib/0.71.0/MinaCalc/MinaCalc.cpp
        src/MinaCalc.cpp
        src/Util.h)
