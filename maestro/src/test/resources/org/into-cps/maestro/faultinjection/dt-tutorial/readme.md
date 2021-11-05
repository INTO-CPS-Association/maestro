# Current status of the test

This test runs a cosimulation that includes the RMQFMU, kalmanFilter fmu, and anomaly detector Fmu, that receives data from the incubator emulator, through the rabbitmq server.
Since the RMQFMU does not handle nested json, a bridge script should be run that flattens the json message from the incubator, and forwards it to the RMQFMU. 
This is a temporary solution until we have this fixed (hopefully) in the RMQFMU.

# Setup Environment

Before running the test:

1. Run the incubator emulator in a separate terminal. Follow the instructions at https://github.com/INTO-CPS-Association/example_digital-twin_incubator/tree/master/software.

2. Run the incubator rmqfmu bridge in a separate terminal (found in the rmqfmu_scenarios folder of this repo):
```bash
$ python rmqfmu_scenarios/rmqfmu_incubator_interface.py
```

3. Wait until the emulator and bridge are up and running, i.e. until the incubator state is printed continuously on the screen of the bridge terminal. 
   
4. Finally, run the rmqfmu test, from the ```scenario_verifier_test``` folder:
```bash
$ python rmqfmu_scenarios/test.py
```

# Related Info

- This test is not part of the integration tests. 
- The co-sim uses ```faultinject-incubator-1.0.0.jar``` for the co-orchestration. This is the FI plugin coupled to Maestro 2.1.5.
- The outputs and logs are saved in the ```dump``` folder.

# Next steps

1. Setup the anomalyDetector fmu (with unifmu) and implement the behaviour, and include it together with the kalman filter in the cosim. At this point we will have a meaningful cosimulation setup, and the first part of the demo is complete.
    - the anomaly detector fmu has been setup with proper inputs/outputs, and kalman filter is included, now the cosimulation setup is meaningful.
    - __NOTE__ that the behaviour of the anomaly detector has to be implemented still.
    - when the anomaly detector sends a disable message, this has to be received and interpreted properly from the incubator side. This still needs to be done. This will be needed in point 2.
2. Setup the experiments with the fault injection, and update as necessary ```emptyfaultEvents.xml``` file. The cosim setup, i.e. the involved FMUs are the same as in step 1. After this, the second demo is complete.
__NOTE__ here that the FI plugin has been extended to allow for the evaluation of expressions for the calculation of the injected values, depending on the simulation time, and the values of the inputs and outputs of the FMU in question. 
3. Add unity FMU for the visualisation. This will provide a nice touch for both step 1. and 2.



