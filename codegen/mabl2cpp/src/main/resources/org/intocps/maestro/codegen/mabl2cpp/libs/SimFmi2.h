//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//
#ifndef SIMFMI2_H
#define SIMFMI2_H


#include "fmi2.h"
#include <string>
#include "unzip.h"



class Fmi2Comp {


public :
    fmi2Component comp;
    FMU *fmu;
    fmi2CallbackFunctions callback={};
};

#define FMI2Component Fmi2Comp*

class Fmi2Impl {
public:


    FMI2Component instantiate(fmi2String instanceName,
            //    fmi2Type fmuType,
            //  fmi2String fmuGUID,
            //   fmi2String fmuResourceLocation,
            // const fmi2CallbackFunctions* functions,
                              fmi2Boolean visible,
                              fmi2Boolean loggingOn);

    void freeInstance(fmi2Component c);

    virtual ~Fmi2Impl();

public:
    FMU fmu;
std::string guid;
    std::string library_path;
    std::string resource_path;
};


#define FMI2 Fmi2Impl*


FMI2 load_FMI2(const char *guid, const char *path);

#endif