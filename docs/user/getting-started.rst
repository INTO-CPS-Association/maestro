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

4a. Running a Co-simulation using CLI
-------------------------------------
Open a terminal in the same folder and execute :code:`java -jar coe-1.0.10-jar-with-dependencies.jar --configuration scenario.json  --oneshot --starttime 0.0 --endtime 10.0`

Afterwards an `outputs.csv` file is available with the co-simulation results.

4b. Running a Co-simulation with Master Web Interface for Co-Simulation
------------------------------------------------------------------------
This requires a bit more than execting a co-simulation using the CLI. 

For this reason, a Python scrpt will be used as reference and explained in bits. :download:`Full Python Script <../resources/getting-started-python-master.py>`

4b.1 Launch the COE
-------------------
Launch the COE with a single argument, which makes it start up as a web server on port 8082: :code:`java -jar coe-1.0.10-jar-with-dependencies.jar -p 8082`.

.. literalinclude:: ../resources/getting-started-python-master.py
    :language: python
    :lines: 28

4b.2 Create a Session
----------------------
It is necessary to create a session before conducting a co-simulation.

Example response: :code:`{'sessionId': '5f439916-23f8-4609-9ff8-5f81408b9046'}`.

Python code:

.. literalinclude:: ../resources/getting-started-python-master.py
    :language: python
    :lines: 30-38


4b.3 Initialize the co-simulation
---------------------------------
Send the `scenario.json` to the server.

Example response: :code:`[{"status":"Initialized","sessionId":"5f439916-23f8-4609-9ff8-5f81408b9046","lastExecTime":0,"avaliableLogLevels":{"{msd2}.msd2i":[],"{msd1}.msd1i":[]}}]`.

Python code:

.. literalinclude:: ../resources/getting-started-python-master.py
    :language: python
    :lines: 40-45

4b.4 Optional: Connect Web Socket
---------------------------------
At this stage, one can connect a web socket if so desired.
This is currently not part of the scenario. See the :ref:`api` for more information.

4b.5 Run the Co-Simulation
--------------------------
The information passed as CLI arguments are now part of a `simulate.json` file:

.. code-block:: JSON
  :linenos:
      
  {
    "startTime": 0,
    "endTime": 10
  }

Send the `simulate.json` file to the server.

Example response: :code:`{"status":"Finished","sessionId":"5f439916-23f8-4609-9ff8-5f81408b9046","lastExecTime":1752}'`.

Python code: 

.. literalinclude:: ../resources/getting-started-python-master.py
    :language: python
    :lines: 18-25, 47-52


4b.6 Get the results
---------------------
Retrieve the csv results and store in `result.csv`

Example response: CSV content (too large to show).

Python code:

.. literalinclude:: ../resources/getting-started-python-master.py
    :language: python
    :lines: 55-66

4b.7 Destroy the session
------------------------
Destroy the session and allow Maestro to clean up session data

Example reponse: 'Session 5f439916-23f8-4609-9ff8-5f81408b9046 destroyed'

Python code:

.. literalinclude:: ../resources/getting-started-python-master.py
    :language: python
    :lines: 71-77