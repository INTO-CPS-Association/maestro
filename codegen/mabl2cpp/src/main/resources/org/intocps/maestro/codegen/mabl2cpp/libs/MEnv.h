//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//
#ifndef MENV_H
#define MENV_H

#include "fmi2TypesPlatform.h"
#include <iostream>
#include <cstdlib>

#include <sstream>
#include <string>
#include <iomanip>
#include <algorithm>
#include <cctype>
#include <filesystem>

#include <rapidjson/document.h>
#include <fstream>
#include <rapidjson/istreamwrapper.h>



using namespace rapidjson;
class MEnvImpl {
private:
    bool to_bool(std::string str) {
        std::transform(str.begin(), str.end(), str.begin(), ::tolower);
        std::istringstream is(str);
        bool b;
        is >> std::boolalpha >> b;
        return b;
    }

    const char* runtimeConfigPath;
    Document json;

public:
    MEnvImpl(const char* runtimeConfigPath) ;

    fmi2Integer getInt(const char *id) ;

    fmi2Boolean getBool(const char *id) ;

    fmi2String getString(const char *id);

    fmi2Real getReal(const char *id) ;


};


#define MEnv MEnvImpl*


MEnv load_MEnv(const char* runtimeConfigPath);

#endif