.. _getting_startedv2:

Getting Started V2
==================
This section takes you through a the entire process of conducting a co-simulation with Maestro2.

| This guide is based on the command line interface (CLI) of Maestro2 and consists of two parts: 
| The first part concerns writing a MaBL Specification by hand and executing it.
| The second part concerns using the specification generation and expansion capabilities of Maestro.

Note that Maestro2 also features a web API presented in :ref:`web-api`.

Setup
-----
The first step is to install the dependencies along with the tools and resources that are to be used

- Download and install Java 11
- Download the :download:`example resources part1 <images/watertank-example-part1.zip>` and unzip into a directory.
- Download the :download:`example resources part2 <images/watertank-example-part2.zip>` and unzip into a directory.
- Download `maestro-2.x.x-jar-with-dependencies.jar` from the newest `Maestro2 2.x.x` release on the Maestro release page https://github.com/INTO-CPS-Association/maestro/releases and place it in the same directory as the example resources were unzipped into.

Part 1: First MaBL Specification
------------------------
The example below concerns a water tank. The tank (Continuous Time component) has a constant inflow of water and a valve that is controlled by a controller (Discrete-Event component). When the valve is open, the water level within the tank decreases, and whe the valve is closed, the water level increases.
The controller determines when to open and close the valve based on a maximum and a minimum water level.

.. _fig-watertankexample:
.. figure:: images/wt-example.png
    :align: center

    Water Tank Example

The corresponding MaBL code, including descriptive comments, to execute this co-simulation is presented below:

.. literalinclude:: images/wt-example.mabl
    :language: c

To execute this (one can use the file corresponding wt-example.mabl from the unzipped example resouces) run the following command from the terminal:

.. code-block:: none

    > java -jar maestro-2.x.x-jar-with-dependencies.jar --interpret wt-example.mabl
        where --interpret is to interpret a specification

The result is available in `outputs.csv`. This can be plotted with `pythoncsvplotter.py`, and the result should look like the figure below.

.. _fig-watertankexample-result:
.. figure:: images/wt-example-result.png
    :align: center

    Result from co-simulation

Part 2: Specification Generation and Expansion
----------------------------------------------
This part also concerns the water tank but uses the capabilities of specification generation and expansion to create the specification. Thus, the user does not have to write MaBL by hand. Specification generation and Expansion are not treated in detail in this guide, but more information is available in :ref:`Specification Generation` and :ref:`Expansion`.

The specification generator is based on JSON configuration files with the same structure as the ones used in Maestro1. For this reason, it is also possible to use the `INTO-CPS Application <https://into-cps-association.readthedocs.io/projects/desktop-application/en/latest/>` to create the specification files.

The content of the configuration file is briefly described after the example below. A more detailed description is available at :ref:`sec:legacy_config_format`. The configuration for this example is:

.. literalinclude:: images/wt-example-config.json
   :language: json

| :code:`fmus` contains mappings between a key, enclosed in :code:`{}` and the corresponding FMU.
| :code:`connections` contains mappings from a single FMU output port to one or more FMU input ports. The format of a port is :code:`{fmuKey}.instance.scalarVariableName`
| :code:`parameters` contains mappings from a single FMU parameter port and a value.
| :code:`algorithm` describes that the fixed-step algorithm should be used with the size 0.001. Currently only fixed-step is supported.
| :code:`end_time` is the end-time of the simulation

The command below generates a specification based on this configuration file:

.. code-block:: none

    > java -jar maestro-2.x.x-jar-with-dependencies.jar --spec-generate1 wt-example-config.json
        where --spec-generate1 uses a specification generator build for legacy configuration files (i.e. configuration files for maestro1).
