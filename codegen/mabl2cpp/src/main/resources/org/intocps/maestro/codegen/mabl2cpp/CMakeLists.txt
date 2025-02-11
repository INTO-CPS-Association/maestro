cmake_minimum_required (VERSION 3.11)
project (sim C CXX)


include(FetchContent)
include(ExternalProject)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

FetchContent_Declare(intocpsfmi
  GIT_REPOSITORY https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi.git
        GIT_TAG  Release/1.0.12
        GIT_SHALLOW ON
 SOURCE_DIR "${CMAKE_BINARY_DIR}/intocpsfmi-src"
)

FetchContent_MakeAvailable(intocpsfmi)
set(intocpsfmi-src "intocpsfmi-src")


SET(BUILD_TOOLS OFF CACHE BOOL "Build tools in the src directory (zipcmp, zipmerge, ziptool)")
SET(BUILD_REGRESS OFF CACHE BOOL "Build regression tests")
SET(BUILD_EXAMPLES OFF CACHE BOOL "Build examples")
SET(BUILD_DOC OFF CACHE BOOL "Build documentation")
SET(BUILD_SHARED_LIBS OFF CACHE BOOL "Build shared libraries")

FetchContent_Declare(libzip
        GIT_REPOSITORY https://github.com/nih-at/libzip.git
        GIT_TAG  v1.11
        GIT_SHALLOW ON
        SOURCE_DIR "${CMAKE_BINARY_DIR}/libzip"
        EXCLUDE_FROM_ALL
)


set(ENABLE_GNUTLS OFF CACHE BOOL "")
set(ENABLE_MBEDTLS OFF CACHE BOOL "")
set(ENABLE_OPENSSL OFF CACHE BOOL "")
set(ENABLE_WINDOWS_CRYPTO OFF CACHE BOOL "")

set(BUILD_TOOLS OFF CACHE BOOL "")
set(BUILD_REGRESS OFF CACHE BOOL "")
set(BUILD_EXAMPLES OFF CACHE BOOL "")
set(BUILD_DOC OFF CACHE BOOL "")
set(BUILD_SHARED_LIBS OFF CACHE BOOL "")

FetchContent_MakeAvailable(libzip )

SET(RAPIDJSON_BUILD_EXAMPLES OFF CACHE BOOL "Build rapidjson examples" FORCE)
SET(RAPIDJSON_BUILD_TESTS OFF CACHE BOOL "Build rapidjson perftests and unittests" FORCE)
FetchContent_Declare(rapidjson
        GIT_REPOSITORY https://github.com/Tencent/rapidjson.git
        GIT_TAG 7c73dd7de7c4f14379b781418c6e947ad464c818 # this hash seems to make it work on windows (nix works with v1.1.0)
     #   GIT_SHALLOW ON
        SOURCE_DIR "${CMAKE_BINARY_DIR}/rapidjson"
        BUILD_COMMAND ""
        CONFIGURE_COMMAND ""
        INSTALL_COMMAND ""
        TEST_COMMAND ""
        EXCLUDE_FROM_ALL
        )

FetchContent_MakeAvailable(rapidjson )

#file(CHMOD_RECURSE "${CMAKE_BINARY_DIR}/rapidjson" PERMISSIONS OWNER_READ OWNER_WRITE  GROUP_READ GROUP_WRITE WORLD_READ WORLD_WRITE )

include_directories(${PROJECT_SOURCE_DIR}
    ${intocpsfmi_SOURCE_DIR}/jnifmuapi/src/main/native/src/external/shared/fmi/include
    ${intocpsfmi_SOURCE_DIR}/jnifmuapi/src/main/native/src/external/shared
    ${intocpsfmi_SOURCE_DIR}/jnifmuapi/src/main/native/src
    ${PROJECT_SOURCE_DIR}/libs
    ${rapidjson_SOURCE_DIR}/include)


add_executable(sim
    ${intocpsfmi-src}/jnifmuapi/src/main/native/src/sim_support.c
    libs/uri.c
    libs/DataWriter.cpp
    libs/BooleanLogic.cpp
    libs/Logger.cpp
    libs/SimFmi2.cpp
    libs/SimMath.cpp
    libs/MEnv.cpp
    libs/unzip.cpp
    libs/VariableStep.cpp
    libs/SimulationControl.cpp
    co-sim.cxx
    main.cpp)

target_link_libraries(sim zip )
target_compile_definitions(sim PRIVATE -DFMI_COSIMULATION)

if (UNIX)
	if(APPLE)

	else()
		target_link_libraries(sim rt dl)
	endif()
endif()


install(TARGETS sim)