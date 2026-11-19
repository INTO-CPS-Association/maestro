.. _web-api:

Web Interface (API)
-------------------

The reference below is available without running Maestro. It is generated from
the same controller and model annotations used by the live API.
Download the :download:`OpenAPI JSON document <api/openapi.json>` for use with
API clients and documentation tools.

On a running server (replace the host and port as needed), interactive Swagger
UI is at ``http://localhost:8082/swagger-ui.html`` and the machine-readable
description is at ``http://localhost:8082/v3/api-docs``.
The older ``/v2/api-docs`` address is no longer used.

Session workflow
^^^^^^^^^^^^^^^^

1. Call ``GET /createSession`` and retain the returned ``sessionId``.
2. Optionally upload FMUs or other files with ``POST /upload/{sessionId}``
   using a multipart field named ``file``. Paths in configurations refer to
   files on the server, not on the client.
3. Send a multi-model configuration to ``POST /initialize/{sessionId}``.
   It contains FMU locations, connections, parameters, and algorithm options.
   The response reports any discovered FMU logging categories.
4. Send start/end times and logging options to ``POST /simulate/{sessionId}``,
   or supply a master model to ``POST /sigverSimulate/{sessionId}``.
   These requests are synchronous and may take the duration of the simulation.
5. Download ``GET /result/{sessionId}/plain`` (CSV) or
   ``GET /result/{sessionId}/zip`` (the session archive).
6. Call ``GET /destroy/{sessionId}`` when results are no longer needed.
   This removes the session and its files.

For example, after initializing a session, submit simulation times with:

.. code-block:: shell

    curl -X POST http://localhost:8082/simulate/SESSION_ID \
      -H 'Content-Type: application/json' -H 'Accept: application/json' \
      -d '{"startTime":0,"endTime":10,"logLevels":{},"reportProgress":false,"liveLogInterval":0.1}'

``GET /status`` lists active sessions; ``GET /status/{sessionId}`` reports one
session. ``POST /stopsimulation/{sessionId}`` requests a cooperative stop, not
forced process termination. The ``Scenario`` operations below generate,
verify, visualize, or execute master models without the same session workflow.

Content types
^^^^^^^^^^^^^

JSON endpoints, including ``/status``, ``/status/{sessionId}``, and ``/version``,
return ``application/json`` with structured response schemas in the OpenAPI
document at ``/v3/api-docs``. Their JSON body shapes are unchanged; clients should
request ``application/json`` rather than ``text/plain``.

Send initialization and simulation request bodies as ``application/json``.
The ``/initialize/{sessionId}`` and ``/simulate/{sessionId}`` endpoints also
continue to accept JSON encoded as ``text/plain`` for legacy clients.
The ``/ping`` endpoint is plain text, and ``/result/{sessionId}/plain`` returns
``text/csv`` rather than JSON.

Errors and current limitations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Thrown exceptions normally produce a non-2xx response through the controller
exception handler (usually HTTP 500). Some exceptions have different mappings:
``IllegalArgumentException`` maps to 404 and
``UnsupportedOperationException`` maps to 422. Error responses include diagnostic
information and, for internal failures, a log reference. These mappings reflect
current behavior and do not imply that every malformed request returns 400.

.. warning::

   Failure reporting is not yet consistent (issue #189). In CLI execution mode,
   an unsuccessful child-process exit can still return HTTP 200 with a
   ``Simulation completed`` message. The session state is selected using stderr
   rather than the exit code. In-process exceptions can also leave the recorded
   state as ``Simulating``. Do not treat HTTP 200 or session state alone as
   definitive proof of successful execution.

Algorithm verification has a separate result contract: inspect ``correct`` and
``errorMessage`` in the response even when HTTP status is 200.
Verification and trace visualization depend on external tools, including
UPPAAL ``verifyta``; they are not supplied by the REST API.

Maintaining the reference
^^^^^^^^^^^^^^^^^^^^^^^^^

Endpoint descriptions use OpenAPI 3 ``@Operation`` annotations and model
descriptions use ``@Schema``. Existing Jackson ``@JsonPropertyDescription``
annotations are retained for JSON-schema consumers. When changing endpoints or
models, regenerate the checked-in
reference from the repository root:

.. code-block:: shell

    mvn -B -ntp -pl maestro-webapi -am \
      -Dtest=OpenApiDocumentationTests,ContentTypeTests \
      -Dsurefire.failIfNoSpecifiedTests=false \
      -Dmaestro.openapi.update=true package

Commit ``docs/user/api/openapi.json`` with the source changes.
``OpenApiDocumentationTests`` checks the reference against the generated API
on subsequent test runs. Sphinx renders that local snapshot, so Read the Docs
does not need a running Java server, FMUs, or UPPAAL to build this page.

Endpoint reference
^^^^^^^^^^^^^^^^^^

.. openapi:: api/openapi.json