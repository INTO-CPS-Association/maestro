.. _overview:

Overview
=========
This section presents an overview of the Maestro 2 approach to co-simulation. Roughly, the approach is to generate a specification expressed in the Domain Specfic Language called Maestro Base Language (MaBL) and then execute it in order to conduct a co-simulation.
This specification is a recipe of what is going to be executed in order to conduct a co-simulation.
By seperating the specification and execution it is possible to verify the specification prior to executing it.

Creating a specification
------------------------
A specification can be written completely by hand if so desired, but Maestro2 also features a `Specification Generator` that can assist in creating a co-simulation according to a given configuration. For more information on how to use the `Specification Generator` please see :ref:`sec-specification_generator`.

Maestro2 also contains a plugin system of `expansion plugins` that can assist in creating a specification. An expansion plugin offers one or more functions that can be invoked in the specification. If a specifiation makes use of expansion plugins, then Maestro2 replaces `expand` function calls with the behaviour of the function call, which is provided by a given expansion plugin. For more information on `expansion plugins`, please see :ref:`sec-expansion`.

Expansion of a Specification
----------------------------
Once a specification has been created it goes through an expansion phase, where :code:`expand` statements related to the aforementioned `Expansion Plugins` are replaced with their corresponding behaviour.

For example, if a specification passed to Maestro2 contains the statement :code:`expand convertBoolean2Real(booleanVariable, realVariable)`, then it will be replaced by the behaviour of the function :code:`convertBoolean2Real`::

    if( booleanVariable )
        {
            realVariable = 1.0;
        }
    else
        {
            realVariable = 0.0;
        }

Once a specification is free of :code:`expand` statements it is ready for execution.

Executing a specification
--------------------------
Maestro2 has an interpreter capable of executing a MaBL specification.
It is possible to define runtime plugins that offer functionality to be used in a MaBL specification. The interpreter will then invoke these functions during execution of the specification.
One such example is the CSV plugin, which writes values to a CSV file.

Outline of the Co-simulation Process with Maestro2
--------------------------------------------------
There are two main ways of creating a specification using Maestro2: Write it by hand or use the Specification Generator. However, several of the processes are the same.


The sections outlines the process of how Maestro2 generates a MaBL specification and executes it.

.. uml:: 
    
    title Co-Simulation with Maestro 2.
    hide footbox
    
    actor User #red
    participant Maestro
    participant "Specification\nGenerator" as SpecGen
    participant MaestroExpand
    participant "ExpansionPlugin : \n IMaestroExpansionPlugin" as expPlugin
    participant Interpreter
    participant "RuntimePlugin : \n ModuleValue" as RuntimePlugin


    alt Using Specification Generator

        User -> Maestro: WithSpecificationGenerator(\nconfiguration.json)
        Maestro -> SpecGen: GenerateSpecification(\nconfiguration)
        SpecGen --> Maestro: Specification

    else Provides MaBL specification

        User -> Maestro: WithSpecification(specification.mabl)

    end


    alt Specification contains expand statements

        Maestro -> MaestroExpand: Expand(Specification)

        loop until all Expand\nstatements has been expanded
            MaestroExpand -> expPlugin: invoke expPlugin with \n configuration and \nfunctional call with arguments
            expPlugin --> MaestroExpand: More expanded Specification
        end

        MaestroExpand --> Maestro: ExpandedSpecification

    end

    Maestro -> Interpreter: Execute(ExpandedSpecification)

    alt Specification contains runtime plugins

        Interpreter -> RuntimePlugin: function(args)
        RuntimePlugin --> Interpreter: Result

    end

    Interpreter -> User: co-simulation results


:configuration.json: Configuration for the co-simulation
:specification.mabl: Specification written in Maestro Base Language (MaBL).
:SpecificationGenerator: Controls the process of creating a MaBL Specification from configuration files. For more information, see :ref:`sec-specification_generator`
:Expand: Expand refers to the process of expansion. Expansion is where a :code:`expand function(args)` statements are replaced by the behaviour of the given function based on an expansion plugin. For more information, see :ref:`sec-expansion`.
:IMaestroExpansion: A plugin that provides one or more functions that can be used in context of expansion.
:ExpandedSpecification: A MaBL Specification that is free from :code:`expand` statements.
:Interpreter: Can execute a MaBL Specification.
:RuntimePlugin: A plugin that is executed by the interpreter during the interpretation of a MaBL Specification.
:results: An expanded MaBL specification and other results of the co-simulation efforts, i.e. a CSV file with results of the co-simulation.
