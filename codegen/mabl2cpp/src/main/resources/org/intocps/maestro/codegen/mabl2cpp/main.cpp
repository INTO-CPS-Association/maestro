#include <chrono>
#include <iostream>
#include <cstring>
#include <algorithm>
#include "MaestroRunTimeException.h"

#include "co-sim.hxx"
int main(int argc, char *argv[]) {

    using namespace std;
    using namespace chrono;

    string runtimeKey = "-runtime";
    string helpKey = "-help";
    string helpQKey = "-?";
    string sourceKey = "-sha1";
    string runtimeConfigPath;
    for (int i = 0; i < argc; i++) {

        string currentArg = argv[i];
        transform(currentArg.begin(), currentArg.end(), currentArg.begin(), ::tolower);
        if (runtimeKey == currentArg) {
            if (i + 1 < argc) {
                runtimeConfigPath = argv[i + 1];
                i++;
            }
        }else if(helpKey == currentArg ||helpQKey == currentArg){
            cout<< "Help\nsim \n\n\t-help, -?\tPrints this message\n\t-runtime\tPath to a runtime json file used for runtime configuration\n\t-sha1\tTo show the pretty printed SHA1 checksum of the input spec"<<endl;
            return 0;
        }else if(sourceKey == currentArg){
            cout << SPEC_SHA1 << " " SPEC_GEN_TIME << endl;

            if (i + 1 < argc) {
                const char* expectedSha1 = argv[i + 1];
                return strcmp(SPEC_SHA1,expectedSha1);
            }

            return 0;
        }
    }
    int exitcode = 0;
    auto t1 = std::chrono::high_resolution_clock::now();
    try {
        simulate(runtimeConfigPath.c_str());
    }catch(MaestroRunTimeException e){
        cerr << "Fatal simulation fault: "<<e.what()<<endl;
        exitcode = -1;
    }
    auto t2 = std::chrono::high_resolution_clock::now();

    auto dur = t2-t1;

    const auto hrs = duration_cast<hours>(dur);
    const auto mins = duration_cast<minutes>(dur - hrs);
    const auto secs = duration_cast<seconds>(dur - hrs - mins);
    const auto ms = duration_cast<milliseconds>(dur - hrs - secs);
    const auto us = duration_cast<microseconds>(dur - hrs - secs-ms);
    const auto ns = duration_cast<nanoseconds>(dur - hrs - secs-ms-us);

    std::cout << "Executed in " << hrs.count() << " [hours] "
              << mins.count() << " [min] "
              << secs.count() << " [seconds] "
              << ms.count() << " [milliseconds] "
              << us.count() << " [microseconds] "
              << ns.count() << " [nanoseconds] " << std::endl;

    std::cout << "Total in nanoseconds: {" << duration_cast<nanoseconds>(dur).count() << "}" << std::endl;

    return exitcode;
 }