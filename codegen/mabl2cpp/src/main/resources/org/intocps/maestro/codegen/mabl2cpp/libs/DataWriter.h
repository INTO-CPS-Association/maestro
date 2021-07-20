
#include <iostream>
#include <iostream>
#include <fstream>
#include "DataWriterConfig.h"


class DataWriterImpl {
public:
    DataWriterImpl(const char *runtimeConfigPath);

    void writeDataPoint(const char *fmt, DataWriterConfig, double time...);

    DataWriterConfig writeHeader(int count, const char **);

    void close();

private:
    std::ofstream myfile;
    std::string filePath;
};

#define DataWriter DataWriterImpl*


DataWriter load_DataWriter(const char *runtimeConfigPath);



