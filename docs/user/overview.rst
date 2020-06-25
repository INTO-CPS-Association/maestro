Overview
=========
This page presents an overview of the Maestro 2 approach to co-simulation and the different components.

The Maestro2 approach is to generate a specification expressed in the DSL Maestro Base Language (MaBL) and then execute it in order to conduct a co-simulation.
This specification is a recipe of what is going to be executed in order to conduct a co-simulation. 
By seperating the specification and execution it is possible to verify the specification prior to executing it.



Creating a specification
------------------------
A specification can be written completely by hand if so desired, but Maestro2 also features a plugin system of `unfolding plugins` that can assist in creating a specification.
An unfolding plugin offer a function that can be called in the specification. If a specifiation makes use of unfolding plugins, 
then Maestro2 `unfolds` function calls to plugins with the behaviour of the function call, which is provided by the unfolding plugin.
As an example, consider a specification where a type conversion function from the TypeConverterPlugin is used in the initial specification passed to Maestro2:
:code:`convertBoolean2Real(booleanVariable, realVariable)`.
Maestro2 will then invoke the TypeConverterPlugin to get the unfolded MaBL code and replace the function call with the unfolded MaBL code provided by the TypeConverterPlugin::

    if( booleanVariable )
        {
            realVariable = 1.0;
        }
    else
        {
            realVariable = 0.0;
        }

Executing a specification
--------------------------
Maestro2 has an interpreter capable of executing a MaBL specification.
It is possible to define interpreter plugins that offer functions to be used in a MaBL specification. The interpreter will then invoke these functions during execution of the specification.
One such example is the CSV plugin, which writes values to a CSV file.

Outline of the Co-simulation Process with Maestro2
------------------------------------------------------
The sections outlines the process of how Maestro2 generates a MaBL specification and executes it.
The elements in the diagram are briefly described afterwards and extended upon elsewhere (TBD).

.. uml:: 
    
    title Co-Simulation with Maestro 2.
    hide footbox
    
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
