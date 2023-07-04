.. _sec-config_file_formats:

Configuration File Formats
==========================
.. note::
   This section will contain the file formats and their structure.

.. _sec-legacy-config-format:

Legacy Configuration / INTO-CPS Multi-model and coe config File Format
--------------------------------

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
* fmus: This is a mapping from fmu name specified in '{}' to a either path from the FMUs folder or a URI using a custom format*
* connections: The is a mapping between triples to a list of triples. The triples takes the form (fmu name, instance name, scalar variable name). The domain of the map must only contain scalar variables with causality output where the scalar variables in the list in the range of the map all must be of causality input
* parameters: The is also a mapping from triples but with causality parameter. The range must contain the value that should be used instead of the value specified in the model description.

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

* multimodel_path
* liveGraphColumns
* liveGraphVisibleRowCount
* graphs
* postProcessingScript
* multimodel_crc

The following properties are used in maestro using sg1 import/ web api:

* startTime: the simulation start time
* endTime: the simulation end time (could be left out)
* livestreamInterval
* livestream: this is a mapping from tuples of fmu name and instance to a list of scalar variables that should be transmitted over the web socket while simulating (similar to the output.csv but just live | only for the webapi)
* logVariables: this is a mapping from tuples of fmu name and instance to a list of scalar variables that should be logged (included in the output.csv)
* visible: fmu visible flag
* loggingOn: fmu logging on flag
* overrideLogLevel: override the maestro log level
* enableAllLogCategoriesPerInstance: automatically enable all log categories in all fmus
* algorithm: the algorithm to be used, default is type='fixed-step' with size=<decimal number>
* parallelSimulation: run the simulation steps in parallel when ever possible. Eg in jaccobian all instances can get/set/doStep in parallel
* stabalizationEnabled: if algebraic loops exists then attempt stabilisation (will use global_absolute_tolerance, global_relative_tolerance)
* global_absolute_tolerance: for stabilisation
* global_relative_tolerance: for stabilisation
* simulationProgramDelay: slow down simulation doStep to make sure its not faster than the step time itself
