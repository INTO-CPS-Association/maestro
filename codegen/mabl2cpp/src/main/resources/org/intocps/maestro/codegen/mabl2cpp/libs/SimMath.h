//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//
#ifndef SIMMATH_H
#define SIMMATH_H


#include <cmath>



class M{
    public:
    inline double min(double a, double b){return fmin(a,b);}
};


#define  Math M*

Math load_Math();

#endif