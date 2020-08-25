Command Line Interface
----------------------
The command line interface is avaialble via the maestro jar file::
    
    usage: coe
    -c,--configuration <path>   Path to configuration file
    -e,--endtime <time>         The start time of the simulation
    -h,--help                   Show this description
    -l,--load <path>            Attempt to load a single FMU
    -o,--oneshot                Run a single simulation and shutdown
    -p,--port <port>            The port where the REST interface will be
                                served
    -r,--result <path>          Path where the csv data should be writting to
    -s,--starttime <time>       The start time of the simulation
    -v                          Verbose
    -version,--version          Version
    -x,--extract <type>         Extract values: 'script'