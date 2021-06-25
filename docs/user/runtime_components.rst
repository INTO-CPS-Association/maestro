.. _runtime_components:

Runtime Components
==================

The Variable Step Component
---------------------------
The variable step component is used for calculating a variable step size during simulation. The component has the following methods:

.. topic:: setFMUs

    :Parameters: string names[], FMI2 fmus[]
    :Returns: VariableStepConfig
    :Description: This method specifies the FMUs that are of relevance for the ports that are to be used for calculating the step size. An array of fully qualifying FMU names, i.e. `{<name of FMU>}.<name of instance>`, should be passed together with an array of the FMUs. The method returns a VariableStepConfig object.

    .. attention::
        It is expected that the indices of the arrays are matching. I.e. the name found on the first index in the array of names is the name of the first FMU in the array of FMUs.

.. topic:: initializePortNames

    :Parameters: VariableStepConfig configuration, string portNames[]
    :Returns: Void

    :Description: This method specifies the names of the ports from which values are used to calculate a step. A VariableStepConfig and an array of port names are expected to be passed.

    .. attention::
        It is expected that the port names matches port values that are passed in the function `addDataPoint`. I.e. the first index in the array of port names are the port name of the value that are found on the first index in the array of port values.

.. topic:: addDataPoint

    :Parameters: VariableStepConfig configuration, real time, ? values
    :Returns: Void
    :Description: This method adds a data point, i.e. port values, to a given time. A VariableStepConfig, a real value representing the time and an array of port values are expected to be passed.

    .. attention::
        It is expected that the port values matches the port names that are passed in the function `initializePortNames`. I.e. the first index in the array of port values are the value of the port name that are found on the first index in the array of port names.

.. topic:: getStepSize

    :Parameters: VariableStepConfig configuration
    :Returns: Real
    :Description: This method returns a step size as a real. It is expected that port names have been initialized and that a data point has been added for the given time. A VariableStepConfig is expected to be passed.


.. topic:: setEndTime

    :Parameters: VariableStepConfig configuration, real endTime
    :Returns: Void
    :Description: This method sets the end time of the simulation. A VariableStepConfig and a real representing the end time of the simulation are expected to be passed.


.. topic:: isStepValid

    :Parameters: VariableStepConfig configuration, real nextTime, ? values
    :Returns: bool
    :Description: This method validates if a given step is valid. A VariableStepConfig, a real representing the time and an array of port values are expected to be passed. The method returns a boolean indicating if the step is valid or not.


:ref:`Following is a snippet <mabl-snippet>` of a :download:`MaBL spec <images/wt_example/spec.mabl>` illustrating how to initialize the component and use it during simulation.
To initialize the component the following steps are necessary:

- Load the VariableStep component by calling :code:`load(...)` with a path to the config.json file (line 98).
- Retrieve a VariableStepConfig object by calling :code:`setFMUs(...)` with FMUs and their corresponding names (line 99).
- Specify the port names of values that are to be passed subsequently by calling :code:`initializePortNames(...)` (line 101).
- Specify the end time of the simulation by calling :code:`setEndTime(...)` (102).

To calculate a step-size after initialization of the component the following steps are necessary:

- Add a datapoint for the current simulation time by calling :code:`addDataPoint(...)` (line 135).
- Get the step-size by calling :code:`getStepSize(...)` (line 136).

It is also possible to validate a step-size by calling :code:`isStepValid(...)` with the new port values and simulation time after stepping each FMU (line 157).

Lastly the component should be unloaded by calling :code:`unload(varaibleStep)` (line 183).

.. _mabl-snippet:
.. literalinclude:: images/wt_example/variableStepTest.mabl
    :language: c
    :linenos:
    :caption: Snippet of a mabl spec showcasing the variable step component.
    :lineno-start: 93
    :lines: 93-183


.. include:: runtime_components/runtime_plugins.inc
