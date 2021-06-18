#include <chrono>
#include <iostream>

#include "co-sim.hxx"
int main(int argc, char *argv[]) {

    using namespace std;
    using namespace chrono;

    string runtimeKey = "-runtime";
    string helpKey = "-help";
    string helpQKey = "-?";
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
            cout<< "Help\nsim \n\n\t-help, -?\tPrints this message\n\t-runtime\tPath to a runtime json file used for runtime configuration"<<endl;
        }
    }
    auto t1 = std::chrono::high_resolution_clock::now();
    simulate(runtimeConfigPath.c_str());
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

    return 0;
 }