.. _runtime_components:

Runtime Components
==================

.. _menv_runtime:

Execution Environment Component (MEnv)
--------------------------------------

``MEnv`` reads named values from the interpreter's runtime JSON configuration.
These are not operating-system environment variables and are distinct from the
generation-time configuration describing FMUs, connections, and parameters.
Lookups happen during execution, so values can change between runs without
regenerating the MaBL specification.

Runtime configuration
^^^^^^^^^^^^^^^^^^^^^

Put the values in a top-level ``environment_variables`` object:

.. literalinclude:: resources/menv/environment.runtime.json
    :language: json

Names are literal, case-sensitive strings. For example, ``sample.count`` is one
key, not a lookup inside a nested ``sample`` object. Other runtime settings, such
as ``DataWriter``, can be sibling keys in the same JSON file.

The module exposes these methods:

.. list-table::
    :header-rows: 1

    * - Method
      - JSON value
    * - ``int getInt(string id)``
      - Integer within the signed 32-bit range
    * - ``real getReal(string id)``
      - Number (integer or decimal)
    * - ``bool getBool(string id)``
      - Boolean: ``true`` or ``false``
    * - ``string getString(string id)``
      - String

Use JSON booleans rather than numeric encodings for ``getBool``. Missing keys or
incompatible types cause an interpreter error; these methods do not provide
default values. Loading ``MEnv`` without the ``environment_variables`` runtime
key fails. MaBL type checking checks the method calls, not the contents of the
runtime JSON.

MaBL and command-line execution
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Import the module, load it without arguments, read the values, and unload it:

.. literalinclude:: resources/menv/environment.mabl
    :language: c#

Download :download:`environment.mabl <resources/menv/environment.mabl>` and
:download:`environment.runtime.json <resources/menv/environment.runtime.json>`
into your working directory. With a current Maestro CLI JAR named
``maestro.jar``, run:

.. code-block:: shell

    java -jar maestro.jar interpret environment.mabl -runtime environment.runtime.json

The example prints ``count=3 step=0.10 enabled=true label=water tank``.
The ``-runtime`` option accepts a path relative to the process working directory,
or an absolute path. The file does not have to be beside the JAR or have a
particular name. Pass it explicitly: the current ``interpret`` command does not
automatically discover sibling ``*.runtime.json`` files. An explicitly supplied
file is the runtime configuration, not an overlay, so include any other required
runtime settings in it as well.

Using MablApiBuilder
^^^^^^^^^^^^^^^^^^^^

The builder's execution-environment API emits the corresponding runtime lookups:

.. code-block:: java

    MablApiBuilder builder = new MablApiBuilder(new MablApiBuilder.MablSettings());
    var scope = builder.getDynamicScope();
    var count = scope.copy("count", builder.getExecutionEnvironment().getInt("sample.count"));
    var step = scope.copy("step", builder.getExecutionEnvironment().getReal("step_size"));
    var enabled = scope.copy("enabled", builder.getExecutionEnvironment().getBool("enabled"));
    var label = scope.copy("label", builder.getExecutionEnvironment().getString("label"));

Provide the same ``environment_variables`` JSON when interpreting the generated
specification. For embedded Java execution, pass the runtime JSON input stream
to ``DefaultExternalValueFactory`` along with the working directory and module
resolver. See ``EnvTest`` in ``frameworks/fmi2api`` and ``WatertankApiTest`` in
``maestro`` for executable examples.

Console Printer Component
---------------------------
The console printer component is used for printing directly to the system console utilizing the Java format string syntax and conversions. The component has the following methods:

.. topic:: print

    :Parameters: String message, ? values
    :Returns: Void
    :Description: This method prints the message formatted with the values to the console WITHOUT a new line.

.. topic:: println

    :Parameters: String message, ? values
    :Returns: Void
    :Description: This method prints the message formatted with the values to the console WITH a new line.

:ref:`Following is a MaBL spec <ConsolePrinter_mabl-spec>` ( :download:`MaBL spec <resources/consolePrinterComponent/consolePrinterSpec.mabl>` ) that demonstrates how to initialize the component and use it.

To utilize the component the following steps are necessary:

- Import the component at the top of the MaBL spec (line 3).
- Load the ConsolePrinter component by calling :code:`load(...)` with the component name (line 6).

.. _ConsolePrinter_mabl-spec:
.. literalinclude:: resources/consolePrinterComponent/consolePrinterSpec.mabl
    :language: c#
    :linenos:
    :caption: MaBL spec showcasing the console printer component.

Variable Step Component
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
    :Description: This method adds a data point, i.e. port values, to a given time. A VariableStepConfig, a real value representing the time and a collection of port values are expected to be passed.

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
    :Description: This method validates if a given step is valid. A VariableStepConfig, a real representing the time and an a collection of port values are expected to be passed. The method returns a boolean indicating if the step is valid or not.


:ref:`Following is a snippet <VariableStep_mabl-snippet>` of a :download:`MaBL spec <images/wt_example/spec.mabl>` that demonstrates how to initialize the component and use it.

To initialize the component the following steps are necessary:

- Import the component at the top of the MaBL spec.
- Load the VariableStep component by calling :code:`load(...)` with the component name and a path to the config.json file (line 98).
- Retrieve a VariableStepConfig object by calling :code:`setFMUs(...)` with FMUs and their corresponding names (line 99).
- Specify the port names of values that are to be passed subsequently by calling :code:`initializePortNames(...)` (line 101).
- Specify the end time of the simulation by calling :code:`setEndTime(...)` (102).

To calculate a step-size after initialization of the component the following steps are necessary:

- Add a datapoint for the current simulation time by calling :code:`addDataPoint(...)` (line 135).
- Get the step-size by calling :code:`getStepSize(...)` (line 136).

It is also possible to validate a step-size by calling :code:`isStepValid(...)` with the new port values and simulation time after stepping each FMU (line 157).

Lastly the component should be unloaded by calling :code:`unload(varaibleStep)` (line 183).

.. _VariableStep_mabl-snippet:
.. literalinclude:: images/wt_example/variableStepTest.mabl
    :language: c#
    :linenos:
    :caption: Snippet of a MaBL spec showcasing the variable step component.
    :lineno-start: 93
    :lines: 93-183


.. include:: runtime_components/runtime_plugins.inc
