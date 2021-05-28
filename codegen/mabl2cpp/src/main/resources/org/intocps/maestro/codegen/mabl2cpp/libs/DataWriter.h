
#include <iostream>
#include <iostream>
#include <fstream>
#include "DataWriterConfig.h"


class DataWriterImpl {
public:
    void writeDataPoint(const char* fmt, DataWriterConfig, double time...);

    DataWriterConfig writeHeader( int count,const char**);

    void close();

    private:
    std::ofstream myfile;
};

#define DataWriter DataWriterImpl*


DataWriter load_DataWriter();



