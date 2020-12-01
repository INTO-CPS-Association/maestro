
#include <iostream>
#include <iostream>
#include <fstream>

#define DataWriterConfig void *

class DataWriterImpl {
public:
    void writeDataPoint(const char* fmt, DataWriterConfig, double time...);

    DataWriterConfig writeHeader( int count,std::string[]);

    void close();

    private:
    std::ofstream myfile;
};

#define DataWriter DataWriterImpl*


DataWriter load_DataWriter();



