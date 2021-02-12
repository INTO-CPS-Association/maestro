
#include "fmi2TypesPlatform.h"
#include <iostream>
#include <cstdlib>

#include <sstream>
#include <string>
#include <iomanip>
#include <algorithm>
#include <cctype>

#include <stdlib.h>

class MEnvImpl {
private:
    bool to_bool(std::string str) {
        std::transform(str.begin(), str.end(), str.begin(), ::tolower);
        std::istringstream is(str);
        bool b;
        is >> std::boolalpha >> b;
        return b;
    }

public:
    fmi2Integer getInt(const char *id) {
        auto value = std::getenv(id);

        if (value == nullptr) {
            std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
            throw -1;

        }
        return std::stoi(value, nullptr, 0);

    }

    fmi2Boolean getBool(const char *id) {
        auto value = std::getenv(id);
        if (value == nullptr) {
            std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
            throw -1;

        }

        return to_bool(value);
    }

    fmi2String getString(const char *id) {
        auto value = std::getenv(id);
        if (value == nullptr) {
            std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
            throw -1;
           
        }
        return value;
    }

    fmi2Real getReal(const char *id) {
        auto value = std::getenv(id);

        if (value == nullptr) {
            std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
            throw -1;
        }
        return atof(value);
    }


};


#define MEnv MEnvImpl*


MEnv load_MEnv();