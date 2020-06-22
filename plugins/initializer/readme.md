# Initializer Plugin
##Introduction
To obtain a trustworthy result of a co-simulation, all of the FMUs must be initialized in the correct order. The system is not allowed to contain any circular dependencies.
The connections both internal in an FMU and externally between FMU creates certain precedence constraints on the initialization order. 
Therefore, the initialization order should be calculated based on these constraints and the predicates described by the FMI specification.
It is essential to ensure the absence of cycles in the system being simulated.

##What the Plugin does
This plugin generates the initialization-phase of a co-simulation specification expressed in MaBL.
The plugin uses the internal and external connections in the system along with the predicates described by FMI to calculate the correct order of initialization. The plugin ensures the absence of cycles and presents the user with problems related to circular dependencies.
The approach used by the plugin does not put any constraints on choosing a master algorithm to be used to carry out the simulation.

##How to use it:
The plugin is used as described at https://into-cps-maestro.readthedocs.io/en/latest/overview.html . 
It is a part of the Maestro2 plugin system of unfolding plugins that can assist in creating a co-simulation specification. 
The plugin works based on some argument (the FMUs in the system), and an environment (the connections between instances of FMUs).

###MaBL
Maestro Base Langauge (MaBL) is a domain-specific to express the specification of co-simulation. Such specifications are then interpreted and executed, resolving in the execution of a co-simulation inside the Maestro 2 tool.

###Configuration
The plugin requires a configuration file containing the values of parameters of FMU variables with _causality=parameter_, which the user would like to overwrite in the initialization phase.
The format of the file should be a JSON-file.
Example:
```json
[
   {
     "identification": {
       "name": "Initializer",
       "version": "x.x.x"
     },
     "config": {
       "parameters": {
         "{crtl}.crtlInstance.maxlevel": 2,
         "{crtl}.crtlInstance.minlevel": 1
       }
     }
   }
 ]
```


##References
A work-shop paper about the plugin: _An FMI-Based initialization plugin for INTO-CPS Maestro 2_ - link will be provided later.  
More information about MaBL and INTO-CPS:  _The INTO-CPS Co-simulation Framework_  
Information about Maestros plugin structure: 

