.. _cli:

Command Line Interface
======================

The command line interface options is available via the cli option :code:`--help`.


.. code-block:: bash

  Usage:
  
  
  maestro [-hV] [COMMAND]
  
  Description:
  
  Mable for co-simulating models
  
  Options:
    -h, --help      Show this help message and exit.
    -V, --version   Print version information and exit.
  Commands:
    interpret  Interpret a specification using the build in Java interpreter. Remember to place all necessary runtime extensions in the classpath
    export     Specification export
    import     Created a specification from various import types. Remember to place all necessary plugin extensions in the classpath.
  
               Hint for sg1 import where menv should be enabled. Use the following to generate the extra input file:'jq '.parameters|keys|{"environmentParameters":.}' mm.json > menv.json'
    sigver     Utilise the scenario verifier tool to generate and verify algorithms. It is also possible to execute scenarios and extended multi-models.

Sub command: import
----------------------

.. code-block:: bash

  Usage: maestro import [-hivV] [-di] [-if] [-nop] [-pa] [-output=<output>]
                        [-vi=<verify>] [-fsp=<fmuSearchPaths>]... <type>
                        [<files>...]
  Created a specification from various import types. Remember to place all
  necessary plugin extensions in the classpath.
  
  Hint for sg1 import where menv should be enabled. Use the following to generate
  the extra input file:'jq '.parameters|keys|{"environmentParameters":.}' mm.json
  > menv.json'
        <type>             The valid import formats: Sg1
        [<files>...]       One or more specification files
        -di, --[no-]dump-intermediate
                           Dump all intermediate expansions
        -fsp, --fmu-search-path=<fmuSearchPaths>
                           One or more search paths used to resolve relative FMU
                             paths.
    -h, --help             Show this help message and exit.
    -i, --interpret        Interpret spec after import
        -if, --[no-]inline-framework-config
                           Inline all framework configs
        -nop, --[no-]disable-optimize
                           Disable spec optimization
        -output=<output>   Path to a directory where the imported spec will be
                             stored
        -pa, --[no-]preserve-annotations
                           Preserve annotations
    -v, --verbose          Verbose
    -V, --version          Print version information and exit.
        -vi, --verify=<verify>
                           Verify the spec according to the following verifier
                             groups: FMI2, Any


Sub command: interpret
----------------------

.. code-block:: bash

  Usage: maestro interpret [-hvV] [-di] [--[no-]expand] [--[no-]typecheck] [-nop]
                           [-pa] [-output=<output>] [-runtime=<runtime>]
                           [-thz=<transitionCheckFrequency>]
                           [-tms=<transitionMinStep>]
                           [-transition=<transitionPath>] [-vi=<verify>]
                           [-wait=<wait>] [<files>...]
  Interpret a specification using the build in Java interpreter. Remember to
  place all necessary runtime extensions in the classpath
        [<files>...]         One or more specification files
        -di, --[no-]dump-intermediate
                             Dump all intermediate expansions
    -h, --help               Show this help message and exit.
        --[no-]expand        Perform expand
        --[no-]typecheck     Perform type check
        -nop, --[no-]disable-optimize
                             Disable spec optimization
        -output=<output>     Path to a directory where the export will be stored
        -pa, --[no-]preserve-annotations
                             Preserve annotations
        -runtime=<runtime>   Path to a runtime file which should be included in
                               the export
        -thz, --transition-check-frequency=<transitionCheckFrequency>
                             The interval which transition spec will be checked
                               at.
        -tms, --transition-minimum-step=<transitionMinStep>
                             The minimum step per for each none empty offering of
                               candidates. It reset once a candidate is
                               removedchecked at.
        -transition=<transitionPath>
                             Path to a directory with a transition specification
    -v, --verbose            Verbose
    -V, --version            Print version information and exit.
        -vi, --verify=<verify>
                             Verify the spec according to the following verifier
                               groups: FMI2, Any
        -wait=<wait>         Wait the specified seconds before processing.
                               Intended for allowing a debugger or profiler to be
                               attached before the processing starts.

Sub command: export
----------------------

.. code-block:: bash

  Usage: maestro export [-hvV] [-output=<output>] [-runtime=<runtime>]
                        [-vi=<verify>] <type> [<files>...]
  Specification export
        <type>               The valid exporters: Cpp
        [<files>...]         One or more specification files
    -h, --help               Show this help message and exit.
        -output=<output>     Path to a directory where the export will be stored
        -runtime=<runtime>   Path to a runtime file which should be included in
                               the export
    -v, --verbose            Verbose
    -V, --version            Print version information and exit.
        -vi, --verify=<verify>
                             Verify the spec according to the following verifier
                               groups: FMI2, Any
