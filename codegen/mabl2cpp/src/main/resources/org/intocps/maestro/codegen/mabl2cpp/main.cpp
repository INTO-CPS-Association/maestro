#include <chrono>
#include <iostream>

#include "co-sim.hxx"

int main () {

    using namespace std;
    using namespace chrono;
    auto t1 = std::chrono::high_resolution_clock::now();
    simulate();
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

    std::cout << "Executed1 in " << hrs.count() << ":"
                << mins.count() << ":"
                << secs.count() << ":"
                << ms.count() << ":"
                << us.count() << ":"
                << ns.count() << ":" << std::endl;

    return 0;
 }