#include <chrono>
#include <iostream>

#include "co-sim.hxx"

int main () {

 auto t1 = std::chrono::high_resolution_clock::now();
simulate();
    auto t2 = std::chrono::high_resolution_clock::now();

    auto duration = std::chrono::duration_cast<std::chrono::microseconds>( t2 - t1 ).count();

    std::cout << duration;
    return 0;



 return 0; }