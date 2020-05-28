Developer Overview
===================
This page presents an extended overview of the Maestro 2 approach to co-simulation and the different components.
The figures presented on this page does not match one-to-one with the implementation.

An initial MaBL specification is passed to Maestro.
A MaBL specification can be folded in the sense that it relies on unfolding plugins to unfold statements until it has been fully unfolded.
The example below shows a folded MaBL specification and will be explained below.

.. code-block:: none

    simulation
    import FixedStep;
    import TypeConverter;
    import InitializerUsingCOE;
    import FMI2;
    import CSV;
    {
        real START_TIME = 10.0;
        real END_TIME = 10.0;
        real STEP_SIZE = 0.1;

        FMI2 tankcontroller = load("FMI2", "{8c4e810f-3df3-4a00-8276-176fa3c9f000}", "src/test/resources/watertankcontroller-c.fmu");
        FMI2 SingleWatertank = load("FMI2", "{cfc65592-9ece-4563-9705-1581b6e7071c}",  "src/test/resources/singlewatertank-20sim.fmu");
        FMI2Component crtlInstance = tankcontroller.instantiate("crtlInstance", false, false);;
        FMI2Component wtInstance = SingleWatertank.instantiate("wtInstance", false, false);;

        IFmuComponent components[2]={wtInstance,crtlInstance};

        external initialize(components,START_TIME, END_TIME);

        external fixedStepCsv(components,STEP_SIZE,0.0,END_TIME,"mm.csv");

        tankcontroller.freeInstance(crtlInstance);
        SingleWatertank.freeInstance(wtInstance);

        unload(tankcontroller);
        unload(SingleWatertank);
    }

The imports :code:`FixedStep`, :code:`TypeConverter` and :code:`InitializerUsingCOE` refer to unfolding plugins.
Unfolding plugins export functions as Function Declarations, from which the types can be derived.

The imports :code:`FMI2` refer and :code:`CSV` refer to interpreter plugins.
Interpreter plugins come with a companion MaBL specification with type definitions.
Example:

.. code-block:: none

    module FMI2 {
        FMI2Component instantiate(string name, bool logging);
        void freeInstance(FMI2Component comp);
    }

    module FMI2Component {
        int setupExperiment( bool toleranceDefined, real tolerance, real startTime, bool stopTimeDefined, real stopTime);
        int doStep(real currentCommunicationPoint, real communicationStepSize, bool noSetFMUStatePriorToCurrentPoint);
        ...
    }

In this case, there are two folded statements: :code:`xternal initialize(components,START_TIME, END_TIME)` and :code:`external fixedStepCsv(components,STEP_SIZE,0.0,END_TIME,"mm.csv")`.
:code:`... initialize(...)` is implemented in the plugin :code:`InitializerUsingCOE`, and :code:`.. fixedStepCsv(...)` is implemented in :code:`FixedStep`.
Upon unfolding these statements, where unfolding refers to replacing the function call with one or more MaBL statements, new folded statements can appear, and the process repeats.
For example, the :code:`initialize` and :code:`fixedStepCsv` possibly makes use of the :code:`TypeConverter` unfolding plugin to convert types.

It is also necessary to supply an environment configuration (:code:`environment.json`) containing the FMUs to use in the given co-simulation and the connections between instances of the FMUs.
Furthermore, if the unfolding plugins used in a MaBL specification require configuration in order to perform unfolding, then it is supplied within a plugin configuration file (:code:`configuration.json`).

Parsing and Unfolding
----------------------
The process of parsing and unfolding is presented below.

.. uml::

    title Parsing a MaBL Specification and an environment
    hide footbox

    actor User #red
    participant Maestro
    participant "MablSpecification\nGenerator" as MablSpecGen

    User -> Maestro: PerformCosimulation(environment.json, \nconfiguration.json, spec.mabl)
    Maestro -> UnitRelationShip: parse(environment.json)
    Maestro <-- UnitRelationShip: environment
    Maestro -> MablSpecGen: parse(spec.mabl)
    MablSpecGen -> ANTLR4 : LexAndParse(spec.mabl)
    MablSpecGen <-- ANTLR4 : parsedNodes
    MablSpecGen -> ParseTree2AstConverter : visit(parsedNodes)
    MablSpecGen <-- ParseTree2AstConverter : AST

Once the MaBL specification has been parsed into an AST it is time to perform the unfolding.
The unfolding plugins are located via classes that implement the interface :code:`IMaestroUnfoldPlugin` and support a certain Framework (currently only FMI2 is supported) via an annotation :code:`@SimulationFramework(framework = Framework.FMI2)`.
The plugins are then matched with the imports of the MaBL specification and the function calls are matched with functions exported by the plugins.

.. uml::

    title Utilizing plugins to unfold statements
    hide footbox

    actor User #red
    participant Maestro
    participant "MablSpecification\nGenerator" as MablSpecGen

    MablSpecGen -> PluginFactory: GetPlugins(IMaestroUnfoldPlugin.class, framework)
    MablSpecGen <-- PluginFactory: unfoldingPlugins
    MablSpecGen -> TypeChecker: BuildExportedFunctionsMap(unfoldingPlugins.exportedFunctions)
    MablSpecGen <-- TypeChecker: exportedFunctions
        loop externalFunctions in AST.externalFunctionCalls
            loop externalFunc in externalFunctions
                MablSpecGen -> MablSpecGen: unfoldingPlugin = getCorrespondingUnfoldingPlugin(exportedFunctions, externalFunc)
                MablSpecGen -> unfoldingPlugin: requireConfig()
                alt plugin requires configuration
                    MablSpecGen <-- unfoldingPlugin: true
                    MablSpecGen -> unfoldingPlugin: parseConfig(pluginSpecificPartOfConfiguration)
                    MablSpecGen <-- unfoldingPlugin: parsedConfig
                else plugin does not require configuration
                    MablSpecGen <-- unfoldingPlugin: false
                end
                MablSpecGen -> unfoldingPlugin: unfold(function, arguments, parsedConfig || null)
                MablSpecGen <-- unfoldingPlugin: unfoldedStatements
                MablSpecGen -> MablSpecGen: AST = UpdateAST(Replace externalFunc with unfoldedStatements)
            end
        end
    MablSpecGen -> TypeChecker: TypeCheck(AST)