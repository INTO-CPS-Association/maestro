Getting Started
===============
This page presents a getting started guide using the command-line interface of Maestro.

Additional information is available at :ref:`api`.

0. Environment
---------------
Maestro is built with Java 11, but it is expected that Java 8 will work as well.

1. Downloads
-------------
Download the latest coe jar from releases: https://github.com/INTO-CPS-Association/maestro/releases/latest

We will be running a co-simulation of the MassSpringDamper case study, described in https://github.com/INTO-CPS-Association/example-mass_spring_damper.

Download the two Mass Spring Damper FMUs exported from 20-sim: https://github.com/INTO-CPS-Association/example-mass_spring_damper/tree/master/FMUs/20-Sim

Place both jar and FMUs in the same folder.

2. Describe FMU Connections
----------------------------
Create the following :code:`scenario.json` file in the same folder as the jar file:

.. code-block:: JSON
  :linenos:
      
  {
      "fmus":{
          "{msd1}":"MassSpringDamper1.fmu",
          "{msd2}":"MassSpringDamper2.fmu"
      },
      "connections":{
          "{msd1}.msd1i.x1":[
              "{msd2}.msd2i.x1"
          ],
          "{msd1}.msd1i.v1":[
              "{msd2}.msd2i.v1"
          ],
          "{msd2}.msd2i.fk":[
              "{msd1}.msd1i.fk"
          ]
      },
      "logVariables":{
          "{msd2}.msd2i":[
              "x2",
              "v2"
          ]
      },
      "parameters":{
          "{msd2}.msd2i.c2":1.0,
          "{msd2}.msd2i.cc":1.0,
          "{msd2}.msd2i.d2":1.0,
          "{msd2}.msd2i.dc":1.0,
          "{msd2}.msd2i.m2":1.0
      },
      "algorithm":{
          "type":"fixed-step",
          "size":0.001
      },
      "loggingOn":false,
      "overrideLogLevel":"INFO"
  }

4. Run Co-simulation using CLI
------------------------------
Open a terminal in the same folder and execute :code:`java -jar coe-1.0.10-jar-with-dependencies.jar --configuration scenario.json  --oneshot --starttime 0.0 --endtime 10.0`

Afterwards an `outputs.csv` file is available with the co-simulation results.
