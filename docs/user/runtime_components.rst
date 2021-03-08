.. _runtime_components:

Runtime Components
==================

The Variable Step Component
---------------------------
The variable step component has the following methods:

**setFMUs**

This method specifies the FMUs that are of relevance for the ports that are to be used for calculating the step size.
An array of fully qualifying FMU names, i.e. {<name of FMU>}.<name of instance>, should be passed together with an array of the FMUs.
The method returns a VariableStepConfig object.

.. attention::
   It is expected that the indices of the arrays are matching. I.e. the name found on the first index in the array of names is the name of the first FMU in the array of FMUs.

:Parameters: string names[], FMI2 fmus[]
:Returns: VariableStepConfig

**initializePortNames**

This method specifies the names of the ports from which values are used to calculate a step.
A VariableStepConfig and an array of port names are expected to be passed.

.. attention::
   It is expected that the indices of the port name array matches those of the port values that are passed in the function `addDataPoint`. I.e. the first index in the array of port names are the port name of the value that are found on the first index of the port value array.

:Parameters: VariableStepConfig configuration, string portNames[]
:Returns: Void

**addDataPoint**

This method adds a data point, i.e. port values, to a given time.
A VariableStepConfig, a real value representing the time and an array of port values are expected to be passed.

.. attention::
   It is expected that the indices of the port values array matches those of the port names that are passed in the function `initializePortNames`. I.e. the first index in the array of port values are the value of the port name that are found on the first index of the port names array.

:Parameters: VariableStepConfig configuration, real time, ? values
:Returns: Void

**getStepSize**

This method returns a step size as a real.
It is expected that port names have been initialized and that a data point has been added for the given time.
A VariableStepConfig is expected to be passed.

:Parameters: VariableStepConfig configuration
:Returns: Real

**setEndTime**

This method sets the end time of the simulation.
A VariableStepConfig and a real representing the end time of the simulation are expected to be passed.

:Parameters: VariableStepConfig configuration, real endTime
:Returns: Void

**isStepValid**

This method validates if a given step is valid.
A VariableStepConfig, a real representing the time and an array of port values are expected to be passed.
The method returns a boolean indicating if the step is valid or not.

:Parameters: VariableStepConfig configuration, real nextTime, ? values
:Returns: bool

.. include:: runtime_components/runtime_plugins.rst
