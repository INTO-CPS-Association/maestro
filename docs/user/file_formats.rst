.. _sec-config_file_formats:

Configuration File Formats
==========================
.. note::
   These configuration files was in maestro v1 / coe called multi-model (:code:`*.mm.json`) and co-simulation configuration (:code:`*.coe.json`) files. Maestro v2 just accept any json files during its (:code:`sg1`) import phase and merges them into one object for processing.

These properties are new to maestro V2

* :code:`faultInjectInstances`: Used for fault injection
* :code:`faultInjectConfigurationPath`: Used for fault injection
* :code:`faultInjectInstances`: Used for fault injection
* :code:`modelTransfers`: Used for model swapping during simulation
* :code:`modelSwaps`: Used for model swapping during simulation
* :code:`reportProgress`

For more on fault injection see the paper "Fault Injecting Co-simulations for Safety" at https://ieeexplore.ieee.org/document/9660728

For more on model swapping se the paper "fmiSwap: Run-time Swapping of Models for Co-simulation and Digital Twins" at https://arxiv.org/pdf/2304.07328.pdf

.. code-block:: json

    {
      "modelSwaps": {
        "controller": {
          "swapInstance": "crtlInstance3",
          "stepCondition": "(true)",
          "swapCondition": "(controller.valve ==true)",
          "swapConnections": {
            "{x3}.crtlInstance3.valve": [
              "{x2}.tank.valvecontrol"
            ],
            "{x2}.tank.level": [
              "{x3}.crtlInstance3.level"
            ]
          }
        }
      },
      "modelTransfers": {
        "controller": "controller",
        "tank": "tank"
      }
    }


.. _sec-legacy-config-format:

Legacy Configuration / INTO-CPS Multi-model and coe config File Format
--------------------------------

.. note::
   This file is a multi-model (:code:`*.mm.json`) file and originally developed for maestro v1


.. code-block:: json

    {
      "fmus": {
        "{control}": "watertankcontroller-c.fmu",
        "{tank}": "singlewatertank-20sim.fmu"
      },
      "connections": {
        "{control}.c.valve": [
          "{tank}.t.valvecontrol"
        ],
        "{tank}.t.level": [
          "{control}.c.level"
        ]
      },
      "parameters": {
        "{control}.c.maxlevel": 2,
        "{control}.c.minlevel": 1
      }
    }

The multi model shown above contains the following:

* :code:`fmus`: This is a mapping from fmu name specified in :code:`{}` to a either path from the FMUs folder or a URI using a custom format*
* :code:`connections`: The is a mapping between triples to a list of triples. The triples takes the form (fmu name, instance name, scalar variable name). The domain of the map must only contain scalar variables with causality output where the scalar variables in the list in the range of the map all must be of causality input
* :code:`parameters`: The is also a mapping from triples but with causality parameter. The range must contain the value that should be used instead of the value specified in the model description.

.. note::
   This file is a co-simulation configuration (:code:`*.coe.json`) file and originally developed for maestro v1.


.. code-block:: json

    {
      "startTime": 0,
      "endTime": 30,
      "multimodel_path": "Multi-models/mm/mm.mm.json",
      "liveGraphColumns": 1,
      "liveGraphVisibleRowCount": 1,
      "graphs": [],
      "livestreamInterval": 0,
      "livestream": {
        "{Controller}.cont": [
          "valve"
        ],
        "{WaterTank}.wt": [
          "level"
        ]
      },
      "logVariables": {
        "{WaterTank}.wt": [
          "level"
        ],
        "{Controller}.cont": [
          "valve"
        ]
      },
      "visible": false,
      "loggingOn": false,
      "overrideLogLevel": null,
      "enableAllLogCategoriesPerInstance": false,
      "algorithm": {
        "type": "fixed-step",
        "size": 0.1
      },
      "postProcessingScript": "",
      "multimodel_crc": null,
      "parallelSimulation": false,
      "stabalizationEnabled": false,
      "global_absolute_tolerance": 0,
      "global_relative_tolerance": 0.01,
      "simulationProgramDelay": false
    }

The following shows the coe config aka simulation configuration. Note that these properties are only used by the app and not maestro:

* :code:`multimodel_path`
* :code:`liveGraphColumns`
* :code:`liveGraphVisibleRowCount`
* :code:`graphs`
* :code:`postProcessingScript`
* :code:`multimodel_crc`

The following properties are used in maestro using sg1 import/ web api:

* :code:`startTime`: the simulation start time
* :code:`endTime`: the simulation end time (could be left out)
* :code:`livestreamInterval`: the live stream max report interval. This is used to skip updates if the simulation perform many updates withing a very short time frame
* :code:`livestream`: this is a mapping from tuples of fmu name and instance to a list of scalar variables that should be transmitted over the web socket while simulating (similar to the output.csv but just live | only for the webapi)
* :code:`logVariables`: this is a mapping from tuples of fmu name and instance to a list of scalar variables that should be logged (included in the output.csv)
* :code:`visible`: fmu visible flag
* :code:`loggingOn`: fmu logging on flag
* :code:`overrideLogLevel`: override the maestro log level
* :code:`enableAllLogCategoriesPerInstance`: automatically enable all log categories in all fmus
* :code:`algorithm`: the algorithm to be used, default is type='fixed-step' with size=<decimal number>
* :code:`parallelSimulation`: run the simulation steps in parallel when ever possible. Eg in jaccobian all instances can get/set/doStep in parallel
* :code:`stabalizationEnabled`: if algebraic loops exists then attempt stabilisation (will use global_absolute_tolerance, global_relative_tolerance)
* :code:`global_absolute_tolerance`: for stabilisation
* :code:`global_relative_tolerance`: for stabilisation
* :code:`simulationProgramDelay`: slow down simulation doStep to make sure its not faster than the step time itself

