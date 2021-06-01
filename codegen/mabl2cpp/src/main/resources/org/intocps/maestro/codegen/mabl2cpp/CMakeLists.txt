cmake_minimum_required (VERSION 3.11)
project (sim C CXX)


include(FetchContent)
include(ExternalProject)

project(zlib-download NONE)

FetchContent_Declare(intocpsfmi
  GIT_REPOSITORY git@github.com:INTO-CPS-Association/org.intocps.maestro.fmi.git
        GIT_TAG  Release/1.0.12
 SOURCE_DIR "${CMAKE_BINARY_DIR}/intocpsfmi-src"
)

FetchContent_Populate(intocpsfmi)
set(intocpsfmi-src "intocpsfmi-src")

FetchContent_Declare(libzip
        GIT_REPOSITORY https://github.com/nih-at/libzip.git
        GIT_TAG  v1.7.3
        SOURCE_DIR "${CMAKE_BINARY_DIR}/libzip"

)

FetchContent_MakeAvailable(libzip )

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
    libs/BooleanLogic.cpp
    libs/Logger.cpp
    libs/SimFmi2.cpp
    libs/SimMath.cpp
    libs/MEnv.cpp
    libs/unzip.cpp
    co-sim.cxx
    main.cpp)

target_link_libraries(sim zip)
target_compile_definitions(sim PRIVATE -DFMI_COSIMULATION)