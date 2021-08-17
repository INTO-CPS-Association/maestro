//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//

#include "VariableStep.h"
#include <cassert>

VariableStep load_VariableStep(const char *path) {
    return new VariableStepImpl();
}


VariableStepConfig VariableStepImpl::setFMUs(const char *names[], FMI2Component fmus[]) {
    assert(("Not implemented yet", false));
}

void VariableStepImpl::initializePortNames(VariableStepConfig configuration, const char *portNames[]) {
    assert(("Not implemented yet", false));
}

void VariableStepImpl::addDataPoint(VariableStepConfig configuration, double time...) {
    assert(("Not implemented yet", false));
}

double VariableStepImpl::getStepSize(VariableStepConfig configuration) {
    assert(("Not implemented yet", false));
}

void VariableStepImpl::setEndTime(VariableStepConfig configuration, double endTime) {
    assert(("Not implemented yet", false));
}

bool VariableStepImpl::isStepValid(VariableStepConfig configuration, double nextTime, bool supportsRollBack ...) {
    assert(("Not implemented yet", false));
}

bool VariableStepImpl::hasReducedStepsize(VariableStepConfig configuration) {
    assert(("Not implemented yet", false));
}

double VariableStepImpl::getReducedStepSize(VariableStepConfig configuration) {
    assert(("Not implemented yet", false));
}