/*
 * (C) Copyright 2011- ECMWF.
 *
 * This software is licensed under the terms of the Apache Licence Version 2.0
 * which can be obtained at http://www.apache.org/licenses/LICENSE-2.0.
 * In applying this licence, ECMWF does not waive the privileges and immunities
 * granted to it by virtue of its status as an intergovernmental organisation nor
 * does it submit to any jurisdiction.
 */

#ifndef ECCODES_ecbuild_config_h
#define ECCODES_ecbuild_config_h

/* ecbuild info */

#ifndef ECBUILD_VERSION_STR
#define ECBUILD_VERSION_STR "3.12.0"
#endif
#ifndef ECBUILD_VERSION
#define ECBUILD_VERSION "3.12.0"
#endif
#ifndef ECBUILD_MACROS_DIR
#define ECBUILD_MACROS_DIR  "/home/nokken/eccodes-build-arm64/ecbuild-src/cmake"
#endif

/* config info */

#define ECCODES_OS_NAME          "Android-1"
#define ECCODES_OS_BITS          64
#define ECCODES_OS_BITS_STR      "64"
#define ECCODES_OS_STR           "UNKNOWN.64"
#define ECCODES_OS_VERSION       "1"
#define ECCODES_SYS_PROCESSOR    "aarch64"

#define ECCODES_BUILD_TIMESTAMP  "20260420233829"
#define ECCODES_BUILD_TYPE       "Release"

#define ECCODES_C_COMPILER_ID      "Clang"
#define ECCODES_C_COMPILER_VERSION "18.0.4"

#define ECCODES_CXX_COMPILER_ID      "Clang"
#define ECCODES_CXX_COMPILER_VERSION "18.0.4"

#define ECCODES_C_COMPILER       "/home/nokken/android-ndk-r27d/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
#define ECCODES_C_FLAGS          "-g -DANDROID -fdata-sections -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -D_FORTIFY_SOURCE=2 -Wformat -Werror=format-security  -pipe -O3 -DNDEBUG"

#define ECCODES_CXX_COMPILER     "/home/nokken/android-ndk-r27d/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
#define ECCODES_CXX_FLAGS        "-g -DANDROID -fdata-sections -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -D_FORTIFY_SOURCE=2 -Wformat -Werror=format-security   -pipe -O3 -DNDEBUG"

/* Needed for finding per package config files */

#define ECCODES_INSTALL_DIR       "/home/nokken/eccodes-android-arm64"
#define ECCODES_INSTALL_BIN_DIR   "/home/nokken/eccodes-android-arm64/bin"
#define ECCODES_INSTALL_LIB_DIR   "/home/nokken/eccodes-android-arm64/lib"
#define ECCODES_INSTALL_DATA_DIR  "/home/nokken/eccodes-android-arm64/share/eccodes"

#define ECCODES_DEVELOPER_SRC_DIR "/home/nokken/eccodes"
#define ECCODES_DEVELOPER_BIN_DIR "/home/nokken/eccodes-build-arm64"

/* Fortran support */

#if 0

#define ECCODES_Fortran_COMPILER_ID      ""
#define ECCODES_Fortran_COMPILER_VERSION ""

#define ECCODES_Fortran_COMPILER ""
#define ECCODES_Fortran_FLAGS    ""

#endif

#endif /* ECCODES_ecbuild_config_h */
