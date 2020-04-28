# Initializer plugin
The initializer plugin creates an FMI2 initialization sequence based on information on a multimodel.

The initialization sequence involves all initialization from loading the FMUs till and including fmi2ExitInitializationMode for all the FMUs of the multimodel.

The overall flow is the following:

```
For each FMU:
    Load FMU
    Instantiate each instance
    For each instance:
        SetupExperiment
        Set each scalar where 
            variability != constant &&
            (
                (initial == exact || Approx) ||
                initial == null && causality == parameter
            ) && 
            type != enumeration
        EnterInitializationMode
        Set each scalar where
            Not set previously &&
            (
                (   causality == output && 
                    initial != calculated
                ) ||
                (   causality = parameter && initial != calculated
                )
            )
```
After this the topologicalsorting plugins arranges the connected inputs and outputs both internally and externally
```
Set inputs and outputs according to topologicalsorting result
For all instances in all FMUs:
    fmi2ExitInitializationMode
```     
                   

       
        
        
        
        
        
```
        
    