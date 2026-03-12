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

#if(NOT MINGW)

    FetchContent_Declare(libzip
            GIT_REPOSITORY https://github.com/nih-at/libzip.git
            GIT_TAG  v1.11.4
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
#else()


#find_package(PkgConfig REQUIRED)
#set(PKG_CONFIG_USE_STATIC_LIBS ON)
#pkg_check_modules(LIBZIP REQUIRED libzip)

# Use static libs



  #  find_library(LIBZIP_LIBRARY
  #               NAMES libzip.a zip
  #               PATHS "${MINGW_PREFIX}/lib"
  #               NO_DEFAULT_PATH)
  #  add_library(zip STATIC IMPORTED)
  #  set_target_properties(zip PROPERTIES IMPORTED_LOCATION ${LIBZIP_LIBRARY})


#endif()



FetchContent_Declare(json URL https://github.com/nlohmann/json/releases/download/v3.12.0/json.tar.xz
    DOWNLOAD_EXTRACT_TIMESTAMP FALSE
)
FetchContent_MakeAvailable(json)



#file(CHMOD_RECURSE "${CMAKE_BINARY_DIR}/rapidjson" PERMISSIONS OWNER_READ OWNER_WRITE  GROUP_READ GROUP_WRITE WORLD_READ WORLD_WRITE )

include_directories(${PROJECT_SOURCE_DIR}
    ${intocpsfmi_SOURCE_DIR}/jnifmuapi/src/main/native/src/external/shared/fmi/include
    ${intocpsfmi_SOURCE_DIR}/jnifmuapi/src/main/native/src/external/shared
    ${intocpsfmi_SOURCE_DIR}/jnifmuapi/src/main/native/src
    ${PROJECT_SOURCE_DIR}/libs)


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

target_link_libraries(sim nlohmann_json::nlohmann_json)
#if (MINGW)

#    target_include_directories(sim PUBLIC ${LIBZIP_INCLUDE_DIRS})
#    target_link_libraries(sim  ${LIBZIP_STATIC_LIBRARIES})
#    target_compile_definitions(sim PRIVATE _HAS_QUICK_EXIT=0)

#else()
    target_link_libraries(sim zip )
#endif()

target_compile_definitions(sim PRIVATE -DFMI_COSIMULATION)

if (UNIX)
	if(APPLE)

	else()
		target_link_libraries(sim rt dl)
	endif()
endif()


install(TARGETS sim)