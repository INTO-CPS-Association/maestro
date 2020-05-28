Developer Overview
===================
This page presents an extended overview of the Maestro 2 approach to co-simulation and the different components.
The figures presented on this page is to outline the implementation and not does not correspond one-to-one.
Furthermore, the figures mainly presents sunny-day scenarios.

An initial MaBL specification is passed to Maestro.
A MaBL specification can be non-expanded in the sense that it relies on expansion plugins to expand statements until
the specification has been fully expanded, in which case it should be executable. This is similar in nature to `macro expansion`.

The example below shows a non-expanded MaBL specification, as it contains statements marked with :code:`external`, which means they have to be expanded.

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

The imports :code:`FixedStep`, :code:`TypeConverter` and :code:`InitializerUsingCOE` refer to expansion plugins.
Expansion plugins export functions as Function Declarations, from which the types can be derived.

The imports :code:`FMI2` and :code:`CSV` refer to interpreter plugins.
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

In this example, there are two statements to expand: :code:`external initialize(components,START_TIME, END_TIME)` and :code:`external fixedStepCsv(components,STEP_SIZE,0.0,END_TIME,"mm.csv")`.
:code:`... initialize(...)` is implemented in the plugin :code:`InitializerUsingCOE`, and :code:`.. fixedStepCsv(...)` is implemented in :code:`FixedStep`.
Upon expanding these statements new :code:`external` statements can appear, and the process repeats.
For example, the :code:`initialize` and :code:`fixedStepCsv` possibly makes use of the :code:`TypeConverter` expansion plugin to convert types.

It is also necessary to supply an environment configuration (:code:`environment.json`) containing the FMUs to use in the given co-simulation and the connections between instances of the FMUs.
Furthermore, if the expansion plugins used in a MaBL specification require configuration in order to perform expansion, then it is supplied within a plugin configuration file (:code:`configuration.json`).

Parsing and Expanding a Specification
-------------------------------------
The process of parsing and expanding is presented below.

.. uml::

    title Parsing a MaBL Specification and an environment
    hide footbox

    actor User #red
    participant Maestro
    participant FMI2FrameworkEnvParser
    participant "MablSpecification\nGenerator" as MablSpecGen

    User -> Maestro: PerformCosimulation(environment.json, \nconfiguration.json, spec.mabl)
    Maestro -> FMI2FrameworkEnvParser: parse(environment.json)
    Maestro <-- FMI2FrameworkEnvParser: environment
    Maestro -> MablSpecGen: parse(spec.mabl)
    MablSpecGen -> ANTLR4 : LexAndParse(spec.mabl)
    MablSpecGen <-- ANTLR4 : parsedNodes
    MablSpecGen -> ParseTree2AstConverter : visit(parsedNodes)
    MablSpecGen <-- ParseTree2AstConverter : AST

Once the MaBL specification has been parsed into an AST it is time to perform the expansion.
The expansion plugins are located via (1) classes that implement the interface :code:`IMaestroExpansionPlugin`,
(2) support a certain Framework (currently only FMI2 is supported) via an annotation :code:`@SimulationFramework(framework = Framework.FMI2)`, and
(3) are imported in the MaBL specification.
The :code:`external` function calls are then matched with the functions exported by the expansion plugins located as described above.

.. uml::

    title Utilizing plugins to expand statements
    hide footbox

    actor User #red
    participant Maestro
    participant "MablSpecification\nGenerator" as MablSpecGen

    MablSpecGen -> PluginFactory: GetPlugins(IMaestroExpansionPlugin.class, framework, imports)
    MablSpecGen <-- PluginFactory: expansionPlugins
    MablSpecGen -> TypeChecker: BuildExportedFunctionsMap(expansionPlugins.exportedFunctions)
    MablSpecGen <-- TypeChecker: exportedExpansionFunctions
        loop externalFunctions in AST.externalFunctionCalls
            loop externalFunc in externalFunctions
                MablSpecGen -> MablSpecGen: expansionPlugin = getCorrespondingExpansionPlugin(exportedExpansionFunctions, externalFunc)
                MablSpecGen -> expansionPlugin: requireConfig()
                alt plugin requires configuration
                    MablSpecGen <-- expansionPlugin: true
                    MablSpecGen -> expansionPlugin: parseConfig(pluginSpecificPartOfConfiguration)
                    MablSpecGen <-- expansionPlugin: parsedConfig
                else plugin does not require configuration
                    MablSpecGen <-- expansionPlugin: false
                end
                MablSpecGen -> expansionPlugin: expand(function, arguments, parsedConfig || null)
                MablSpecGen <-- expansionPlugin: expandedStatements
                MablSpecGen -> MablSpecGen: AST = UpdateAST(Replace externalFunc with expandedStatements)
            end
        end

Verifying a Specification
--------------------------
The verification a specification consists of two concepts: Type checking and verification plugins.
The diagram below continues from where the diagram above ended, where AST represents a fully-expanded AST.

.. uml::

    title Verifying a MaBL Specificatino
    hide footbox

    participant "MablSpecification\nGenerator" as MablSpecGen

    MablSpecGen -> TypeChecker: TypeCheck(AST)
    MablSpecGen <-- TypeChecker: OK
    MablSpecGen -> PluginFactory: GetPlugins(IMaestroVerifier, framework)
    MablSpecGen <-- PluginFactory: verificationPlugins
        loop verificationPlugin in verificationPlugins
            MablSpecGen -> verificationPlugin: verify(AST)
            MablSpecGen <-- verificationPlugin: OK
        end
    MablSpecGen -> MablSpecGen: verifiedSpecification = true


Executing a Specification
--------------------------
The execution is carried out via interpretation of the AST and by utilising the interpretation plugins.
TBD...

