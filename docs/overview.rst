Overview
=========
This page presents an overview of the Maestro 2 approach and the different components.


Example
-------
The introduction begins with an example of how a co-simulation is conducted using Maestro2.
The various elements in the example are briefly described herein and extended upon elsewhere.

.. uml:: 
    
    actor User #red
    participant Maestro
    participant "MablSpecification\nGenerator" as MablSpecGen
    participant "InitializePlugin  : \n IMaestroUnfoldPlugin" as InitializePlugin
    participant "FixedStepPlugin : \n IMaestroUnfoldPlugin" as FixedStepPlugin


    User -> Maestro: PerformCosimulation(environment.json, \nconfiguration.json, spec.mabl)
    Maestro -> MablSpecGen: GenerateSpecification(\nenvironment, configuration, spec)
    MablSpecGen -> InitializePlugin: unfold(environment, config, \nfunctionName, functionArguments)
    InitializePlugin -> MablSpecGen: unfoldedInitializeSpec
    MablSpecGen -> FixedStepPlugin: unfold(environment, \nfunctionName, functionArguments)
    FixedStepPlugin -> MablSpecGen: unfoldedFixedStepSpec
    MablSpecGen -> Maestro: unfoldedSpec
    Maestro -> Interpreter: Execute(unfoldedSpec)
    Interpreter -> "CSVPlugin : \n(TBD)\nIMaestroInterpreterPlugin": Log 
    Interpreter -> User: results

