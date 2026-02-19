#include "MEnv.h"

MEnv load_MEnv(const char *runtimeConfigPath) {
    return new MEnvImpl(runtimeConfigPath);

}

MEnvImpl::MEnvImpl(const char *runtimeConfigPath) {
    this->runtimeConfigPath = runtimeConfigPath;
    namespace fs = std::filesystem;
    if (fs::exists(this->runtimeConfigPath)) {
        //config exists so lets parse it to memory
        using namespace std;

        ifstream ifs(this->runtimeConfigPath);

        auto d = nj::parse(ifs);

        if (d.is_object()) {
            if (d.contains("environment_variables") && d["environment_variables"].is_object()) {
                this->json["environment_variables"]=d["environment_variables"];
            }
        }
    }
}

std::string toEnvName(const char *name) {
    std::string str = name;
    str.erase(std::remove(str.begin(), str.end(), '{'), str.end());
    str.erase(std::remove(str.begin(), str.end(), '}'), str.end());
    std::replace(str.begin(), str.end(), '.', '_');
    return str;
}

fmi2Real MEnvImpl::getReal(const char *id) {
    auto value = std::getenv(toEnvName(id).c_str());
    if (value == nullptr && this->json.is_object()) {
        if (this->json.contains(id)) {
            if (this->json[id].is_number()) {
                if (this->json[id].is_number_float()) {
                    return this->json[id].get<double>();
                } else if (this->json[id].is_number_integer()) {
                    return this->json[id].get<int>();
                }
            }
        }
    }

    if (value == nullptr) {
        std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
        throw -1;
    }
    return atof(value);
}


fmi2String MEnvImpl::getString(const char *id) {
    auto value = std::getenv(toEnvName(id).c_str());

    if (value == nullptr && this->json.is_object()) {
        if (this->json.contains(id)) {
            if (this->json[id].is_string()) {
                return strdup(this->json[id].get<std::string>().c_str());
            }
        }
    }

    if (value == nullptr) {
        std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
        throw -1;

    }
    return value;
}

fmi2Boolean MEnvImpl::getBool(const char *id) {
    auto value = std::getenv(toEnvName(id).c_str());

    if (value == nullptr && this->json.is_object()) {
        if (this->json.contains(id)) {
            if (this->json[id].is_number_integer() || this->json[id].is_boolean()) {
                if (this->json[id].is_boolean()) {
                    return this->json[id].get<double>();
                } else if (this->json[id].is_number_integer()) {
                    return this->json[id].get<int>();
                }
            }
        }
    }

    if (value == nullptr) {
        std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
        throw -1;

    }

    return to_bool(value);
}

fmi2Integer MEnvImpl::getInt(const char *id) {
    auto value = std::getenv(toEnvName(id).c_str());

    if (value == nullptr && this->json.is_object()) {
        if (this->json.contains(id)) {
            if (this->json[id].is_number()) {
                if (this->json[id].is_number_integer()) {
                    return this->json[id].get<int>();
                }
            }
        }
    }

    if (value == nullptr) {
        std::cerr << "Environment variable '" << id << "' was not found" << std::endl;
        throw -1;

    }
    return std::stoi(value, nullptr, 0);

}
