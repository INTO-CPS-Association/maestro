cmake_minimum_required (VERSION 3.11)
project (sim C CXX)


include(FetchContent)
include(ExternalProject)

project(zlib-download NONE)

#ExternalProject_Add(zlib
#        GIT_REPOSITORY git://github.com/madler/zlib.git
#        GIT_TAG v1.2.11
#        SOURCE_DIR "${CMAKE_BINARY_DIR}/zlib-src"
#        BINARY_DIR "${CMAKE_BINARY_DIR}/zlib-build"
#        CONFIGURE_COMMAND ""
#        BUILD_COMMAND ""
#        INSTALL_COMMAND ""
#        TEST_COMMAND ""
#        )

FetchContent_Declare(intocpsfmi
  GIT_REPOSITORY git@github.com:INTO-CPS-Association/org.intocps.maestro.fmi.git
        GIT_TAG  Release/1.0.12
 SOURCE_DIR "${CMAKE_BINARY_DIR}/intocpsfmi-src"
)

FetchContent_Populate(intocpsfmi)
set(intocpsfmi-src "intocpsfmi-src")

#ExternalProject_Add(intocpsfmi
#        GIT_REPOSITORY git@github.com:INTO-CPS-Association/org.intocps.maestro.fmi.git
#        GIT_TAG  Release/1.0.12
#        SOURCE_DIR "${CMAKE_BINARY_DIR}/intocpsfmi-src"
#        BINARY_DIR "${CMAKE_BINARY_DIR}/intocpsfmi-build"
#        CONFIGURE_COMMAND ""
#        BUILD_COMMAND ""
#        INSTALL_COMMAND ""
#        TEST_COMMAND ""
#        )

#add_subdirectory(zlib-src)
#add_subdirectory(intocpsfmi-src)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)



include_directories(${PROJECT_SOURCE_DIR}
#    ${intocpsfmi-src}/jnifmuapi/src/main/native/src/external/shared/fmi2/headers
${intocpsfmi-src}/jnifmuapi/src/main/native/src/external/shared/fmi/include
    ${intocpsfmi-src}/jnifmuapi/src/main/native/src/external/shared
    ${intocpsfmi-src}/jnifmuapi/src/main/native/src
    libs)


add_executable(sim
    ${intocpsfmi-src}/jnifmuapi/src/main/native/src/sim_support.c
    libs/DataWriter.cpp
    libs/Logger.cpp
    libs/SimFmi2.cpp
    libs/SimMath.cpp
    co-sim.cxx
    main.cpp)

#target_link_libraries(sim zlibstatic)
target_compile_definitions(sim PRIVATE -DFMI_COSIMULATION)