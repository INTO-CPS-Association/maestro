#include "SimFmi2.h"
//#include "zlib.h"
#include <iostream>
#include <string>
#include <iostream>
#include <filesystem>
#include <zip.h>
#include <filesystem>
#include <algorithm> //Not part of string, use "algorithms" replace()
#include <cstring> //memcopy

namespace fs = std::filesystem;
extern "C" {
#include "sim_support.h"
}


FMI2 load_FMI2(const char *guid, const char *path) {
    using namespace std;
    using namespace chrono;

    if(!std::filesystem::exists(path))
    {
        std::cerr  << "FMU path not found. Cannot load it. "<<path<<std::endl;
        return nullptr;
    }

    auto t1 = std::chrono::high_resolution_clock::now();

    std::string fmuDest = fs::temp_directory_path();

    std::string id=guid;
    std::replace(id.begin(),id.end(),'{','_');
    std::replace(id.begin(),id.end(),'}','_');
    fmuDest.append(id);
    std::filesystem::remove_all(fmuDest.c_str());
    fs::create_directory(fmuDest);

    std::cout << "Unpacked fmu " << path << " to " << fmuDest << std::endl;

    unzip(path, fmuDest.c_str());

    auto fmu = new Fmi2Impl();
    fmu->resource_path = fmuDest;
    fmu->resource_path.append("/resources");
    std::string library_base = fmuDest;

    library_base.append("/binaries");

    #ifdef _WIN32
        library_base.append("/win64/");
    #elif __APPLE__
    #if TARGET_OS_MAC
        library_base.append("/darwin64/");
    #else
        throwException(env, "Unsupported platform");
    #endif
    #elif __linux
        library_base.append("/linux64/");
    #endif

    //  std::string firstFile;
    for (const auto &entry : fs::directory_iterator(library_base.c_str())) {
        //std::cout << entry.path() << std::endl;
        fmu->library_path = entry.path();
        break;
    }


    fmu->guid = guid;
    auto success = loadDll(fmu->library_path.c_str(), &fmu->fmu);
    auto t2 = std::chrono::high_resolution_clock::now();

    auto dur = t2 - t1;
    std::cout << "Load in nanoseconds: {" << duration_cast<nanoseconds>(dur).count() << "}" << std::endl;

    if (!success)
        return nullptr;
    return fmu;

}


void Fmi2Impl::freeInstance(fmi2Component c) {
    this->fmu.freeInstance(c);
}

void stepFinished(fmi2ComponentEnvironment, fmi2Status) {}

Fmi2Comp *Fmi2Impl::instantiate(fmi2String instanceName, fmi2Boolean visible, fmi2Boolean loggingOn) {
    fmi2CallbackFunctions callback = {
            .logger = &fmuLogger, .allocateMemory = calloc, .freeMemory = free, .stepFinished=&stepFinished, .componentEnvironment = this};

    auto *comp = new Fmi2Comp();
    comp->fmu = &this->fmu;
    memcpy(&comp->callback, &callback, sizeof(fmi2CallbackFunctions));

    std::string resource_uri = "file://";
    resource_uri.append(this->resource_path);
    comp->comp =
            this->fmu.instantiate(instanceName, fmi2CoSimulation, this->guid.c_str(), resource_uri.c_str(),
                                  &comp->callback, visible,
                                  loggingOn);

    if (comp->comp == nullptr)
        return nullptr;

    return comp;

}

Fmi2Impl::~Fmi2Impl() {
#ifdef _WIN32
    FreeLibrary(this->fmu.dllHandle);
#elif __APPLE__

#include "TargetConditionals.h"

#if TARGET_OS_MAC
    // Other kinds of Mac OS
    dlclose(this->fmu.dllHandle);
#else
    throwException(env, "Unsupported platform");
#endif
#elif __linux
    dlclose(this->fmu.dllHandle);
#endif


}