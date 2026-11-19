.. _web-api:

Web Interface (API)
-------------------
| When the application is executing the API details can be accessed via: http://localhost:8082/swagger-ui.html
| An offline version is available here: INSERT REFERENCE/LINK TO OFFLINE VERSION

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