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
        IStreamWrapper isw(ifs);

        Document d;
        d.ParseStream(isw);

        if (d.IsObject()) {
            if (d.HasMember("environment_variables") && d["environment_variables"].IsObject()) {
                this->json.CopyFrom(d["environment_variables"], this->json.GetAllocator());
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
    if (value == nullptr && this->json.IsObject()) {
        if (this->json.HasMember(id)) {
            if (this->json[id].IsNumber()) {
                if (this->json[id].IsDouble()) {
                    return this->json[id].GetDouble();
                } else if (this->json[id].IsInt()) {
                    return this->json[id].GetInt();
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

    if (value == nullptr && this->json.IsObject()) {
        if (this->json.HasMember(id)) {
            if (this->json[id].IsString()) {
                return this->json[id].GetString();
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

    if (value == nullptr && this->json.IsObject()) {
        if (this->json.HasMember(id)) {
            if (this->json[id].IsInt() || this->json[id].IsBool()) {
                if (this->json[id].IsBool()) {
                    return this->json[id].GetBool();
                } else if (this->json[id].IsInt()) {
                    return this->json[id].GetInt();
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

    if (value == nullptr && this->json.IsObject()) {
        if (this->json.HasMember(id)) {
            if (this->json[id].IsNumber()) {
                if (this->json[id].IsInt()) {
                    return this->json[id].GetInt();
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
