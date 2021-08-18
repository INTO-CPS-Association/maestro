#include "DataWriter.h"
#include <filesystem>

#include <rapidjson/document.h>
#include <fstream>
#include <rapidjson/istreamwrapper.h>

using namespace rapidjson;

DataWriter load_DataWriter(const char *runtimeConfigPath) {

    std::string csvPath = "output.csv";

    namespace fs = std::filesystem;
    if (fs::exists(runtimeConfigPath)) {
        //config exists so lets parse it to memory
        using namespace std;

        ifstream ifs(runtimeConfigPath);
        IStreamWrapper isw(ifs);

        Document d;
        d.ParseStream(isw);

        if (d.IsObject()) {
            if (d.HasMember("DataWriter") && d["DataWriter"].IsArray()) {

                for (auto &v : d["DataWriter"].GetArray()) {
                    if (v.IsObject()) {

                        if(v.HasMember("type")&& strcmp(v["type"].GetString(),"CSV")==0 && v.HasMember("filename"))
                        {
                            csvPath=v["filename"].GetString();
                            break;
                        }
                    }
                }
            }
        }
    }

    return new DataWriterImpl(csvPath.c_str());
}

DataWriterImpl::DataWriterImpl(const char *runtimeConfigPath) {
    this->filePath = runtimeConfigPath;
}

void DataWriterImpl::close() {
    myfile.close();

}

DataWriterConfig DataWriterImpl::writeHeader(int size, const char **headers) {


    myfile.open(this->filePath.c_str());
    myfile.precision(17);
    myfile << "\"time\",";
    for (int i = 0; i < size; i++) {
        myfile << "\"" << headers[i] << "\"";
        if (i + 1 < size)
            myfile << " , ";
    }

    myfile << std::endl;
    return nullptr;

}

void DataWriterImpl::writeDataPoint(const char *fmt, DataWriterConfig, double time, ...) {
    va_list args;
    va_start(args, time);

    myfile << time << " , ";
    bool first = true;
    while (*fmt != '\0') {

        if (!first && (*fmt == 'i' || *fmt == 'b' || *fmt == 'r' || *fmt == 's')) {
            myfile << " , ";
        }
        first = false;


        if (*fmt == 'i') {
            int i = va_arg(args, int);
            myfile << i;
        } else if (*fmt == 'b') {
            int i = va_arg(args, int);
            if (i == 0)
                myfile << "false";
            else
                myfile << "true";
        } else if (*fmt == 's') {
            char* c = va_arg(args, char*);
            myfile << static_cast<char*>(c);
        } else if (*fmt == 'r') {
            double d = va_arg(args, double);
            myfile << d;
        }
        ++fmt;
    }


    va_end(args);
    myfile << std::endl;
}