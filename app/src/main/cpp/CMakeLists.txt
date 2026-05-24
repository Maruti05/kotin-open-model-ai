cmake_minimum_required(VERSION 3.22)
project(llama_jni)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp source — clone into app/src/main/cpp/llama.cpp/
add_subdirectory(llama.cpp EXCLUDE_FROM_ALL)

add_library(llama_jni SHARED llama_jni.cpp)

target_include_directories(llama_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/ggml/include
)

target_link_libraries(llama_jni
    llama
    ggml
    ggml-base
    ggml-cpu
    log
    m
    z
)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(llama_jni PRIVATE GGML_USE_LLAMAFILE=1)
endif()
