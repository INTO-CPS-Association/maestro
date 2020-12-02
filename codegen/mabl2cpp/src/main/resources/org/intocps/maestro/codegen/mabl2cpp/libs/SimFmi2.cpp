#include "SimFmi2.h"
//#include "zlib.h"
#include <iostream>
#include <string>
#include <iostream>
#include <filesystem>

namespace fs = std::filesystem;
extern "C" {
#include "sim_support.h"
}

/*
unsigned long file_size(char *filename) {
    FILE *pFile = fopen(filename, "rb");
    fseek(pFile, 0, SEEK_END);
    unsigned long size = ftell(pFile);
    fclose(pFile);
    return size;
}

int decompress_one_file(const char *infilename, const char *outfilename) {
    gzFile infile = gzopen(infilename, "rb");
    FILE *outfile = fopen(outfilename, "wb");
    if (!infile || !outfile) return -1;

    char buffer[128];
    int num_read = 0;
    while ((num_read = gzread(infile, buffer, sizeof(buffer))) > 0) {
        fwrite(buffer, 1, num_read, outfile);
    }

    gzclose(infile);
    fclose(outfile);
}*/

FMI2 load_FMI2(const char *guid, const char *path) {
  using namespace std;
    using namespace chrono;
    auto t1 = std::chrono::high_resolution_clock::now();


    std::string fmuDest = "/tmp/";
    fmuDest.append(guid);
    // decompress_one_file(path, fmuDest.c_str());
    std::string cmd = "unzip -o ";
    cmd.append(path);
    cmd.append(" -d ");
    cmd.append(fmuDest);
    system(cmd.c_str());
    std::cout << cmd << std::endl;
    std::cout << "Unpacked fmu " << path << " to " << fmuDest << std::endl;

    fmuDest.append("/binaries/darwin64/");

    std::string firstFile;
    for (const auto &entry : fs::directory_iterator(fmuDest.c_str())) {
        std::cout << entry.path() << std::endl;
        firstFile = entry.path();
        break;
    }

    auto fmu = new Fmi2Impl();
    fmu->guid = guid;
    loadDll(firstFile.c_str(), &fmu->fmu);
auto t2 = std::chrono::high_resolution_clock::now();

    auto dur = t2-t1;
        std::cout << "Load in nanoseconds: {" << duration_cast<nanoseconds>(dur).count() << "}" << std::endl;

    return fmu;

}


void Fmi2Impl::freeInstance(fmi2Component c) {
    this->fmu.freeInstance(c);
}

Fmi2Comp *Fmi2Impl::instantiate(fmi2String instanceName, fmi2Boolean visible, fmi2Boolean loggingOn) {
    fmi2CallbackFunctions callback = {
            .componentEnvironment = this, .logger = &fmuLogger, .allocateMemory = calloc, .freeMemory = free};

    Fmi2Comp *comp = new Fmi2Comp();
    comp->fmu = &this->fmu;
    comp->comp =
            this->fmu.instantiate(instanceName, fmi2CoSimulation, this->guid.c_str(), "", &callback, visible,
                                  loggingOn);

    return comp;

}