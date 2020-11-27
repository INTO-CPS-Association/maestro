
#include "fmi2.h"
#include <string>
class Fmi2Comp {

//    fmi2GetTypesPlatformTYPE         *getTypesPlatform;
//    fmi2GetVersionTYPE               *getVersion;
    fmi2Status setDebugLogging(fmi2Component c,
                               fmi2Boolean loggingOn,
                               size_t nCategories,
                               const fmi2String categories[]);

    fmi2InstantiateTYPE *instantiate;
    fmi2FreeInstanceTYPE *freeInstance;
    fmi2SetupExperimentTYPE *setupExperiment;
    fmi2EnterInitializationModeTYPE *enterInitializationMode;
    fmi2ExitInitializationModeTYPE *exitInitializationMode;
    fmi2TerminateTYPE *terminate;
    fmi2ResetTYPE *reset;
    fmi2GetRealTYPE *getReal;
    fmi2GetIntegerTYPE *getInteger;
    fmi2GetBooleanTYPE *getBoolean;
    fmi2GetStringTYPE *getString;
    fmi2SetRealTYPE *setReal;
    fmi2SetIntegerTYPE *setInteger;
    fmi2SetBooleanTYPE *setBoolean;
    fmi2SetStringTYPE *setString;
    fmi2GetFMUstateTYPE *getFMUstate;
    fmi2SetFMUstateTYPE *setFMUstate;
    fmi2FreeFMUstateTYPE *freeFMUstate;
    fmi2SerializedFMUstateSizeTYPE *serializedFMUstateSize;
    fmi2SerializeFMUstateTYPE *serializeFMUstate;
    fmi2DeSerializeFMUstateTYPE *deSerializeFMUstate;
    fmi2GetDirectionalDerivativeTYPE *getDirectionalDerivative;
    /***************************************************
    Functions for FMI2 for Co-Simulation
    ****************************************************/
    fmi2SetRealInputDerivativesTYPE *setRealInputDerivatives;
    fmi2GetRealOutputDerivativesTYPE *getRealOutputDerivatives;
    fmi2DoStepTYPE *doStep;
    fmi2CancelStepTYPE *cancelStep;
    fmi2GetStatusTYPE *getStatus;
    fmi2GetRealStatusTYPE *getRealStatus;
    fmi2GetIntegerStatusTYPE *getIntegerStatus;
    fmi2GetBooleanStatusTYPE *getBooleanStatus;
    fmi2GetStringStatusTYPE *getStringStatus;

    //INTO CPS specific
    fmi2GetMaxStepsizeTYPE *getMaxStepsize;

public :
    fmi2Component comp;
    FMU *fmu;
};

#define FMI2Component Fmi2Comp*

class Fmi2Impl {
public:


    FMI2Component instantiate(fmi2String instanceName,
            //    fmi2Type fmuType,
            //  fmi2String fmuGUID,
            //   fmi2String fmuResourceLocation,
            // const fmi2CallbackFunctions* functions,
                              fmi2Boolean visible,
                              fmi2Boolean loggingOn);

    void freeInstance(fmi2Component c);

public:
    FMU fmu;
std::string guid;
};


#define FMI2 Fmi2Impl*


FMI2 load_FMI2(const char *guid, const char *path);