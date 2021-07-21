//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//

#ifndef VARIABLESTEP_H
#define VARIABLESTEP_H
#include "VariableStepConfig.h"
#include "SimFmi2.h"
#include <stdarg.h>



class VariableStepImpl {

public:
    VariableStepConfig setFMUs(const char* names[], FMI2Component fmus[]);
    void initializePortNames(VariableStepConfig configuration, const char* portNames[]);
    void addDataPoint(VariableStepConfig configuration, double time...);
    double getStepSize(VariableStepConfig configuration);
    void setEndTime(VariableStepConfig configuration, double endTime);
    bool isStepValid(VariableStepConfig configuration, double nextTime, bool supportsRollBack ...);
    bool hasReducedStepsize(VariableStepConfig configuration);
    double getReducedStepSize(VariableStepConfig configuration);

};





#define VariableStep VariableStepImpl*


VariableStep load_VariableStep(const char* config);
#endif //VARIABLESTEP_H
