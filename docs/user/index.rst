.. INTO-CPS Maestro documentation master file, created by
   sphinx-quickstart on Tue May 26 14:23:21 2020.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

.. _user-documentation:

User documentation
==================
The Maestro2 approach to co-simulation is presented and described in :ref:`overview`. 
Overall, the approach consists of creating a specification in the domain specific language MaBL, verifying the specification and finally, executing the specification.
There is a particular focus on making the FMI-commands explicit in the specification, but it is possible to define runtime behaviour that is not limited to MaBL.

The ideal place to get a first impression of co-simulation with Maestro2 is :ref:`getting_started`.

.. toctree::
   :numbered:
   :maxdepth: 3
   :caption: Table of Contents:

   overview
   getting-started
   cli
   web-api
   specification_components
   runtime_components
   file_formats
