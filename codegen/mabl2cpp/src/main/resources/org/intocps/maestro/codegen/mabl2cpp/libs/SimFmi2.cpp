#include "SimFmi2.h"
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
#include "uri.h"
}

#ifdef _WIN32
#include <system_error>
#include <memory>
#include <string>
std::string GetLastErrorAsString()
{
    //Get the error message ID, if any.
    DWORD errorMessageID = ::GetLastError();
    if(errorMessageID == 0) {
        return std::string(); //No error message has been recorded
    }

    LPSTR messageBuffer = nullptr;

    //Ask Win32 to give us the string version of that message ID.
    //The parameters we pass in, tell Win32 to create the buffer that holds the message for us (because we don't yet know how long the message string will be).
    size_t size = FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                                 NULL, errorMessageID, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPSTR)&messageBuffer, 0, NULL);

    //Copy the error message into a std::string.
    std::string message(messageBuffer, size);

    //Free the Win32's string's buffer.
    LocalFree(messageBuffer);

    return message;
}

#endif


bool hasEnding (std::string const &fullString, std::string const &ending) {
    if (fullString.length() >= ending.length()) {
        return (0 == fullString.compare (fullString.length() - ending.length(), ending.length(), ending));
    } else {
        return false;
    }
}


FMI2 load_FMI2(const char *guid, const char *in_path) {
    using namespace std;
    using namespace chrono;

    auto path =URIToNativePath(in_path);

    if(!std::filesystem::exists(path))
    {
        std::cerr  << "FMU path not found. Cannot load it. "<<path<<std::endl;
        return nullptr;
    }

    auto t1 = std::chrono::high_resolution_clock::now();

    auto fmuDest = std::filesystem::path(fs::temp_directory_path());

    std::string id=guid;
    std::replace(id.begin(),id.end(),'{','_');
    std::replace(id.begin(),id.end(),'}','_');
    fmuDest =fmuDest/id;
    std::filesystem::remove_all(fmuDest);
    fs::create_directory(fmuDest);


    std::cout << "Unpacked fmu " << path << " to " << fmuDest << std::endl;

    unzip(path, fmuDest.u8string().c_str());

      auto fmu = new Fmi2Impl();
      fmu->resource_path = (fmuDest / "resources").u8string();
      auto library_base = fmuDest / "binaries";


  #ifdef _WIN32
      library_base=library_base/"win64";
  #elif __APPLE__
  #if TARGET_OS_MAC
      library_base = library_base / "darwin64";
  #else
      throwException(env, "Unsupported platform");
  #endif
  #elif __linux
      library_base=library_base/"linux64";
  #endif

      const char *extension = ".dylib";
  #ifdef _WIN32
      extension =".dll";
  #elif __APPLE__
      extension = ".dylib";
  #elif __linux
      extension =".so";
  #endif


      //  std::string firstFile;
      bool modelLibFound = false;
      for (const auto &entry : fs::directory_iterator(library_base)) {
          std::cout << entry.path() << std::endl;
          fmu->library_path = entry.path().u8string();
          if (hasEnding(fmu->library_path, extension)) {
              modelLibFound = true;
              break;
          }
      }

    if(!modelLibFound)
    {
        std::cerr  << "FMU does not contain any suitable library. Cannot load it. " << path << std::endl;
        return nullptr;
    }
    std::cout << "Loading library: "<< fmu->library_path << std::endl;

    fmu->guid = guid;
    auto success = loadDll(fmu->library_path.c_str(), &fmu->fmu);

    #ifdef _WIN32
    if(!success){
        std::cout << "LoadLibraryEx failed with: '" << GetLastErrorAsString()<< "'" <<std::endl;
    }
    #endif

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