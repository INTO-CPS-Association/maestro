![Build Status](https://github.com/INTO-CPS-Association/maestro/workflows/Maestro2/badge.svg?branch=development)
[![Documentation Status](https://readthedocs.org/projects/maestro/badge/?version=latest)](https://into-cps-maestro.readthedocs.io/en/latest/)

# Maestro 2

Maestro 2 is the next version of the Maestro Co-simulation Orchestration Engine.

#### Development Environment

You need Java 11 and maven 3.6 to build the project. For the external / integration tests it also needs a functioning
cpp environment with CMake (on windows MSYS)

The project can be built from CLI using the maven command:.

```
mvn package
```

IntelliJ IDEA provides an inbuilt support for all the steps involved in the development of the project. There is some
Scala code in this project. As long as you keep this in mind, you are welcome to use any development IDE.

If IntelliJ fails to build the project after import, then it might be resolved by executing `mvn install` in the
terminal and reimporting in the maven control panel of IntelliJ.

## Release the tool

Replace ${RELEASE_VER} with the release (e.g. 2.0.0) and ${NEW_DEV_VER} with dev version (e.g. 2.0.1-SNAPSHOT). Press
enter when prompted for SCM

```bash
mvn -Dmaven.repo.local=repository release:clean
mvn -Dmaven.repo.local=repository release:prepare -DreleaseVersion=${RELEASE_VER} -DdevelopmentVersion=${NEW_DEV_VER}
mvn -Dmaven.repo.local=repository release:perform
```

Now go to the master branch, merge with the newly created tag, and push. REMEMBER TO GO BACK TO DEVELOPMENT BRANCH!

for more see https://github.com/overturetool/overture/wiki/Release-Process

* Sonatype link: https://oss.sonatype.org/#stagingRepositories

## Upload the release

remember to create github release from the released tag and
upload: `target/checkout/webapi/target/webapi-2.0.0-SNAPSHOT.jar` and
`target/checkout/maestro/target/maestro-2.0.0-SNAPSHOT-jar-with-dependencies.jar`.

Furthermore, remember to update the download.json file in the development branch of INTO-CPS/github.io with both files
above. The file webapi should be the new coe.jar

## Building the documentation

The documentation is automatically on push by readthedocs.io.

Local build is carried out through the make.bat/Makefile within the docs repository. It requires Sphinx and
sphinxcontrib-plantuml
