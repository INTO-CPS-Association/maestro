.. _getting_started:

Getting Started
===============
This section takes you through a the entire process of conducting a co-simulation with Maestro2.

This guide is based on the command line interface (CLI) of Maestro2. Maestro2 also features a web API presented in :ref:`web-api`.

Setup
-----
The first step is to install the dependencies and the tools that are to be used

- Download and install Java 11
- Download `maestro-webapi-2.x.x.jar` from the newest `Maestro2 2.x.x` release on the Maestro release page https://github.com/INTO-CPS-Association/maestro/releases

Generate a specification
------------------------
Maestro2 features a specification generator that can assist in generating a specification. Its input is a configuration file in the :code:`json` format.
It can be utilised with the following command:

.. code-block:: json

    $ java -jar maestro2.jar -sg -c pathToConfigurationFile.json -o build
    where 
        -sg specifies to use the specification generator 
        -c specifies the path to the configuration file for the specification generator
        -o specifies the output directory for the resulting specification file

The configuration file is:

.. code-block::

    CONFIGURATION FILE HERE TBD

.. WARNING::
    TBD: Write the configuration file for the code-block

The resulting files in the directory build_interpretation are:

.. WARNING::
   TBD: Write the files of the build folder


Expanding a specification
-------------------------
Once a specification has been generated it has to be expanded before it can be interpreted. This is carried out with the following command:

.. code-block::

    > java -jar maestro2.jar -e -m build -d build_interpretation -di
    where
        -e specifies to perform expansion
        -m specifies the folder with the MaBL specification
        -d specifies the folder for the resulting fully expanded specification file
        -di specifies that a new file shall be created on every expanion.

The resulting files in the directory build_interpretation are:

.. WARNING::
   TBD: Write the files of the build_interpretation folder

Interpreting a specification
----------------------------
Finally, it is time for interpreting the specification. Execute the following command to do so:

.. code-block::

    > java -jar maestro2.jar -i -m build_interpretation -r results
    where
        -i specifies to perform interpretation
        -m specifies the folder with the MaBL specification
        -r specifies the folder for the resulting co-simulation results

The resulting files in the folder results are:

.. WARNING::
   TBD: Write the resulting files of the results folder

Combining the steps above
-------------------------
It is also possible to create a specification, expand it and interpret it with one command:

.. code-block::

    COMMAND HERE TBD

.. WARNING::
    TBD: Write the single command for the code-block