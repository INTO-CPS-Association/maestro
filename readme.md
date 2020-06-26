# Maestro 2.0.0 Alpha
Maestro 2 is the next version of the Maestro Co-simulation Orchestration Engine.

Please see the [docs](https://into-cps-maestro.readthedocs.io/) and the [chat](https://gitter.im/INTO-CPS/maestro2) for more information.

#### Development Environment
You need Java 11 and maven 3.6 to build the project.
The project can be built from CLI using the maven commands.
```
mvn compile
mvn test
mvn package
mvn clean install site site:stage
```
IntelliJ IDEA provides an inbuilt support for all the steps involved in the development 
of the project. There is some Scala code in this project. 
As long as you keep this in mind, you are welcome to use any development IDE.

If IntelliJ fails to build the project after import, then it might be resolved by executing `mvn install` in the terminal and reimporting in the maven control panel of IntelliJ. 
