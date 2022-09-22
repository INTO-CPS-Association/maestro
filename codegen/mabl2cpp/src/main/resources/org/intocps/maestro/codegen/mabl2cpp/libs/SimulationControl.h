//
// Created by Kenneth Guldbrandt Lausdahl on 01/09/2022.
//
#ifndef SIMULATIONCONTROL_H
#define SIMULATIONCONTROL_H


#include <iostream>
#include <stdarg.h>



class SimulationControlImpl{
public:
    static bool m_stopRequested;
    bool stopRequested()
    {
        return m_stopRequested;
    }
};


#define SimulationControl SimulationControlImpl*


SimulationControl load_SimulationControl();

#endif