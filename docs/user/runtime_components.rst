.. _runtime_components:

Runtime Components
==================

The Variable Step Component
---------------------------
The variable step component is used for calculating a variable step size during simulation.
The component has the following methods:

**setFMUs**

:Parameters: string names[], FMI2 fmus[]
:Returns: VariableStepConfig

This method specifies the FMUs that are of relevance for the ports that are to be used for calculating the step size.
An array of fully qualifying FMU names, i.e. `{<name of FMU>}.<name of instance>`, should be passed together with an array of the FMUs.
The method returns a VariableStepConfig object.

.. attention::
   It is expected that the indices of the arrays are matching. I.e. the name found on the first index in the array of names is the name of the first FMU in the array of FMUs.

**initializePortNames**

:Parameters: VariableStepConfig configuration, string portNames[]
:Returns: Void

This method specifies the names of the ports from which values are used to calculate a step.
A VariableStepConfig and an array of port names are expected to be passed.

.. attention::
   It is expected that the port names matches port values that are passed in the function `addDataPoint`. I.e. the first index in the array of port names are the port name of the value that are found on the first index in the array of port values.

**addDataPoint**

:Parameters: VariableStepConfig configuration, real time, ? values
:Returns: Void

This method adds a data point, i.e. port values, to a given time.
A VariableStepConfig, a real value representing the time and an array of port values are expected to be passed.

.. attention::
   It is expected that the port values matches the port names that are passed in the function `initializePortNames`. I.e. the first index in the array of port values are the value of the port name that are found on the first index in the array of port names.

**getStepSize**

:Parameters: VariableStepConfig configuration
:Returns: Real

This method returns a step size as a real.
It is expected that port names have been initialized and that a data point has been added for the given time.
A VariableStepConfig is expected to be passed.


**setEndTime**

:Parameters: VariableStepConfig configuration, real endTime
:Returns: Void

This method sets the end time of the simulation.
A VariableStepConfig and a real representing the end time of the simulation are expected to be passed.


**isStepValid**

:Parameters: VariableStepConfig configuration, real nextTime, ? values
:Returns: bool

This method validates if a given step is valid.
A VariableStepConfig, a real representing the time and an array of port values are expected to be passed.
The method returns a boolean indicating if the step is valid or not.


Following is a snippet of a :download:`MaBL spec <images/wt_example/spec.mabl>` illustrating how to initialize the component and use it during simulation.
On line 98 the VariableStep component is loaded by passing a path to the config.json file.


.. literalinclude:: images/wt_example/variableStepTest.mabl
    :language: c
    :linenos:
    :lineno-start: 93
    :lines: 93-183


.. include:: runtime_components/runtime_plugins.rst
