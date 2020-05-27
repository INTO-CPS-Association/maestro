Overview
=========
This page presents an overview of the Maestro 2 approach and the different components.


Example
-------
The introduction begins with an example of how a co-simulation is conducted using Maestro2.
The various elements in the example are briefly described afterwards and extended upon elsewhere.

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


:environment.json: FMUs to use and the connections between instances of FMUs
:configuration.json: Configuration for the plugins.
:spec.mabl: Specification written in Maestro Base Language (MaBL). In this example, it contains two folded statements: :code:`initialize(arguments)` and :code:`fixedStep(arguments)` which are unfolded by plugins. This is furthermore described in the subsequent fields.
:MablSpecificationGenerator: Controls the process of creating a MaBL Specification from MaBL specifications and plugins.
:Unfold: Unfold refers to the process of unfolding. Unfolding is where a single statement is converted to multiple statements.
:IMaestroUnfoldPlugin: A plugin that is executed by the MablSpecificationGenerator during generation of a MaBL Specification. 
    A plugin that inherits from IMaestroUnfoldPlugin is capable of unfolding one or more MaBL statements.
:InitializePlugin \: IMaestroUnfoldPlugin: The initialize plugins unfolds the statementment :code:`initialize(arguments)` into MaBL statements that initializes the FMI2 instances passed via arguments
:FixedStepPlugin \: IMaestroUnfoldPlugin: The FixedStep plugins unfolds the statementment :code:`fixedStep(arguments)` into MaBL statements that creates the simulation statements required to execute a fixed step size algorithm based on the arguments. Note, it does not contain initialization. Initialization is taken care of by the InitializePlugin.
:UnfoldedSpec: A MaBL Specification that has been fully unfolded. 
:Interpreter: Can execute a MaBL Specification.
:IMaestroInterpreterPlugin: A plugin that is executed by the interpreter during the interpretation of a MaBL Specification.
:CSVPlugin \: IMaestroInterpreterPlugin: An interpreter plugin that logs values to a CSV file.
:results: A fully unfolded MaBL Specification and a CSV results file of the simulation.
