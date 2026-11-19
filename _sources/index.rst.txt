.. INTO-CPS Maestro documentation master file, created by
   sphinx-quickstart on Tue May 26 14:23:21 2020.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

INTO-CPS Maestro2
==================

Maestro2 is a Co-simulation Orchestration Engine based on The Functional Mockup Interface 2.0 for Co-simulation.
It consists of a domain specific language (DSL) called Maestro Base Language (MaBL), an interpreter of the language and utilities to assist in specifying co-simulations in MaBL.

It is cross-platform as it is based on the JVM and offers interaction through a web interface and a command line interface. 
Furthermore, Maestro 2 has a companion application in the INTO-CPS Application, which offers a graphical user interface and 
utilises the web interface of Maestro 2. This documentation concerns Maestro 2 and not the INTO-CPS Application. 
For more information on the INTO-CPS Application, please see `<https://into-cps-association.github.io/simulation/app.html>`_.

Maestro2 Releases are available at the `Maestro Repository <https://github.com/INTO-CPS-Association/maestro/releases/tag/Release%2F2.0.2>`_.

| If you are interested in Maestro2 from a user's perspective, then please see :ref:`user-documentation`
| If you are interested in Maestro2 as a contributor in the software development sense,  please see :ref:`developer-documentation`

.. _feature_overview:

Feature Overview
----------------

- Supports The Functional Mockup Interface 2 for Co-simulation
- Supports Maestro Base Language (MaBL) for specifying Co-simulations
- Interpreter for Maestro Base Language for executing co-simulations
- Auto generation of co-simulation specifications in MaBL from configuration files (from INTO-CPS Application)
- Expansion plugins to assist with writing MaBL specifications
- Supports iterative initialization of FMU connections forming strongly connected components
- Supports Jacobi fixed step iteration with rollback
- Supports Variable Step functionality without rollback
- Provides CLI and Web-API with legacy (Maestro1) support
- Provides live-logging via Web-API and websockets

.. _features_in_progress:

Features in Progress
--------------------
- Stabilisation throughout simulation
- Distributed FMI capabilities
- Slow-down simulation to real time
- JVM FMU
- Maestro as co-simulation worker instaed of leader.
- C-code generation
- Digital Twin Capabilities
- Formal Verification of MaBL specifications
- Auto-generation of MaBL from Simulator Contracts


.. toctree::
   :maxdepth: 4
   :hidden:

   user/index
   dev/index
   https://github.com/INTO-CPS-Association/Documentation

..
   Indices and tables
   ==================

   * :ref:`genindex`
   * :ref:`modindex`
   * :ref:`search`
