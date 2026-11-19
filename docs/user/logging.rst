.. _logging:

Logging
=======

Maestro's Java logs, FMU diagnostic messages, and recorded simulation variables
are configured separately.

Maestro logging
---------------

Maestro uses Log4j2 for its Java logs. When using Maestro as a dependency, place
``log4j2.xml`` in the host application's classpath, normally by adding it to
``src/main/resources`` (or ``src/test/resources`` for tests). The filename is
``log4j2.xml``, not the Log4j 1.x filename ``log4j.xml``. This assumes the host
application uses the Log4j2 backend; if it supplies a different SLF4J backend,
configure that backend instead.

An external configuration can be selected explicitly with a JVM option, placed
before ``-jar`` or the main class:

.. code-block:: bash

   java -Dlog4j.configurationFile=/absolute/path/log4j2.xml -jar maestro.jar ...

For example, this configuration sends all Maestro Java log levels to the console:

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <Configuration status="WARN">
     <Appenders>
       <Console name="Console" target="SYSTEM_OUT">
         <PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger - %msg%n"/>
       </Console>
     </Appenders>
     <Loggers>
       <Root level="ALL">
         <AppenderRef ref="Console"/>
       </Root>
     </Loggers>
   </Configuration>

The bundled CLI configuration writes to the console and ``coe.log`` in the
process's working directory, with size-based rotation. ``-output`` does not
relocate this Java log file. The host application's configuration determines the
destinations when Maestro is embedded; the example above creates no log file.

SG1 JSON override
~~~~~~~~~~~~~~~~~

SG1 import and web API initialization accept ``overrideLogLevel``:

.. code-block:: json

   {
     "overrideLogLevel": "TRACE"
   }

Supported values are ``OFF``, ``FATAL``, ``ERROR``, ``WARN``, ``INFO``, ``DEBUG``,
``TRACE``, and ``ALL``. Omit the field or use ``null`` to retain the configured
root level. For SG1 import, the override takes effect after the input JSON files
have been merged and deserialized, before specification generation and any
``--interpret`` execution. It does not affect startup or JSON parsing messages.

This changes the Log4j2 root logger for the process, not just one FMU or
simulation. It remains in effect for subsequent calls in the same JVM until
changed again. Explicit levels on individual loggers and appender filters still
apply; use ``log4j2.xml`` to configure these and the output destinations.
The override is not stored in the generated MaBL specification, so a separate
``interpret`` invocation uses that process's logging configuration.

FMU diagnostic logging
----------------------

The SG1 JSON fields ``loggingOn`` and ``logLevels`` configure FMI logging:

.. code-block:: json

   {
     "loggingOn": true,
     "logLevels": {
       "{controller}.controllerInstance": ["logAll"],
       "{tank}.tankInstance": []
     }
   }

Replace the instance keys and category names with those from your multi-model
and FMU. Categories are FMU-defined (see its ``modelDescription.xml``), not Java
levels such as ``DEBUG``. An empty category list requests all categories for
that instance. Omitting ``logLevels`` while enabling ``loggingOn`` requests all
categories for all instances; an empty map selects no instances for explicit
category configuration.

Maestro can only report diagnostics that the FMU emits. Enabling logging cannot
recover an internal stack trace or error cause that the FMU does not expose.
For the FMI2 interpreter, callback messages are also written to
``<instanceName>.log`` when an interpreter working/output directory is supplied
(for CLI import with ``--interpret``, use ``-output``). The file uses the
generated MaBL instance name. Messages forwarded to the Java logger are logged
at ``INFO``, so the Java logging configuration must allow that level to display
them there.

Simulation variable recording
-----------------------------

``logVariables`` selects scalar variables to record in simulation output such
as ``outputs.csv``. It does not enable FMU diagnostics or change Java log
severity. See :doc:`file_formats` for the multi-model fields.
