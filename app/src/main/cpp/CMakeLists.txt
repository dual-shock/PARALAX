cmake_minimum_required(VERSION 3.18.1)
project(eccodes_jni C CXX)


add_library(eccodes STATIC IMPORTED)
set_target_properties(eccodes PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/prebuilt/${ANDROID_ABI}/libeccodes.a
)

add_library(eccodes_memfs STATIC IMPORTED)
set_target_properties(eccodes_memfs PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/prebuilt/${ANDROID_ABI}/libeccodes_memfs.a
)


add_library(openjp2 STATIC IMPORTED)
set_target_properties(openjp2 PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/prebuilt/${ANDROID_ABI}/libopenjp2.a
)


add_library(gribbridge SHARED gribbridge.c)
set_target_properties(gribbridge PROPERTIES LINKER_LANGUAGE CXX)
target_link_options(gribbridge PRIVATE "-Wl,-z,max-page-size=16384")

target_include_directories(gribbridge PRIVATE
    ${CMAKE_SOURCE_DIR}/include/eccodes
    ${CMAKE_SOURCE_DIR}/include/openjpeg-2.5
)


target_link_libraries(gribbridge
    eccodes
    eccodes_memfs
    openjp2
    m       # math
    log     # Android logging
)