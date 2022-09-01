#include "SimulationControl.h"


SimulationControl load_SimulationControl(){
    return new SimulationControlImpl();

}

bool SimulationControlImpl::m_stopRequested = false;