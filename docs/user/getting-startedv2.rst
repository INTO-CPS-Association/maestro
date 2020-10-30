.. _getting_startedv2:

Getting Started V2
==================
This section takes you through a the entire process of conducting a co-simulation with Maestro2.

This guide is based on the command line interface (CLI) of Maestro2. Maestro2 also features a web API presented in :ref:`web-api`.

Setup
-----
The first step is to install the dependencies along with the tools and resources that are to be used

- Download and install Java 11
- Download the :download:`example resources <images/watertank-example.zip>` and unzip into a directory.
- Download `maestro-2.x.x-jar-with-dependencies.jar` from the newest `Maestro2 2.x.x` release on the Maestro release page https://github.com/INTO-CPS-Association/maestro/releases and place it in the same directory as the example resources were unzipped into.

First MaBL Specification
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

    TO BE DONE

The `pythoncsvplotter.py` can be used to plot the co-simulation results, which should look like below:

.. _fig-watertankexample-result:
.. figure:: images/wt-example-result.png
    :align: center

    Result from co-simulation

