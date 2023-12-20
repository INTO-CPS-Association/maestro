![Build Status](https://github.com/INTO-CPS-Association/maestro/workflows/Maestro2/badge.svg?branch=development)
[![Documentation Status](https://readthedocs.org/projects/maestro/badge/?version=latest)](https://into-cps-maestro.readthedocs.io/en/latest/)

# Maestro 2

Maestro 2 is the next version of the Maestro Co-simulation Orchestration Engine.

## Documentation and Getting started

Plese see our documentation here: https://into-cps-maestro.readthedocs.io/en/latest/user/getting-started.html

## Development Environment

You need Java 11 and maven 3.6 to build the project. For the external / integration tests it also needs a functioning
cpp environment with CMake (on windows MSYS). See guide below under CPP environment installation guide

The project can be built from CLI using the maven command:.

```
mvn package
```

IntelliJ IDEA provides an inbuilt support for all the steps involved in the development of the project. There is some
Scala code in this project. As long as you keep this in mind, you are welcome to use any development IDE.

If IntelliJ fails to build the project after import, then it might be resolved by executing `mvn install` in the
terminal and reimporting in the maven control panel of IntelliJ.

### CPP environment installation guide

#### Windows
1. Install msys with mingw64 (https://www.msys2.org/)
2. Follow the guide at https://www.msys2.org/
3. Add C:\msys64\mingw64\bin to Path
4. Add C:\msys64\usr\bin to Path
5. Run pacman -S mingw-w64-x86_64-cmake in the MSYS terminal

#### Linux (ubuntu)

```bash
apt install git libzip-dev build-essential cmake
```

## Release procedure
The release procedure roughly follows the [Overture release procedure](https://github.com/overturetool/overture/wiki/New-Release-Procedure).
### Prerequisites
1. Acquire login access to http://oss.sonatype.org by signing up to their [issue tracker](https://issues.sonatype.org/secure/Dashboard.jspa). 
Then ask @CThuleHansen to submit a ticket there to have your user id granted rights to publish to the org.overturetool repository on the Maven Central. 
This [ticket](https://issues.sonatype.org/browse/OSSRH-35910) is a reasonable template for this.
2. [Setup GPG](https://central.sonatype.org/publish/requirements/gpg/)*
3. Make sure that you are connected to the repository with SSH and can authenticate successfully: https://docs.github.com/en/github/authenticating-to-github/connecting-to-github-with-ssh
4. Make sure you can access [Aarhus University's Gitlab server](https://gitlab.au.dk/overture/certificates) and download the certificate store to sign the release. 
5. Configure your Maven settings (stored in the settings.xml file) correctly. 
In particular, make sure your local ~/.m2/settings.xml has the filled-in version of the skeleton settings in the file 
[overture_settings.xml](https://github.com/overturetool/overture/blob/development/overture_settings.xml) from the Overture repository.

*__Note:__ On Windows it is easiest to use Git Bash for steps 2, 3 and running the following release commands.

### Release the tool
Make sure to be on the development branch. 
Then replace ${RELEASE_VER} with the release (e.g. 2.0.0) and ${NEW_DEV_VER} with dev version (e.g. 2.0.1-SNAPSHOT) in the following commands and run them. Press
enter when prompted for SCM

```bash
mvn -Dmaven.repo.local=repository release:clean
mvn -Dmaven.repo.local=repository release:prepare -DreleaseVersion=${RELEASE_VER} -DdevelopmentVersion=${NEW_DEV_VER}
mvn -Dmaven.repo.local=repository release:perform
```
__Note:__ If the release fails during _release:prepare_ a rollback should be performed with _release:rollback_ before running the above commands again.

After successfully running the commands check that the release is present in the [sonatype staging repository](https://oss.sonatype.org/#stagingRepositories).
Then press Close in the sonatype ui. Sonatype will then run a number of checks and if these succeed press the Release button.

### Upload the release

[Create a release on Github](https://github.com/INTO-CPS-Association/maestro/tags) from the newly created tag and include both the Maestro jar and the Maestro Web API jar.
In the release description note significant changes and the jars MD5 checksums.
Now go to the master branch, merge with the newly created tag, and push. REMEMBER TO GO BACK TO DEVELOPMENT BRANCH!

Furthermore, remember to update the [download.json](https://github.com/INTO-CPS-Association/INTO-CPS-Association.github.io/tree/development/download) file in the development branch of INTO-CPS-Association/INTO-CPS-Association.github.io.
The item "maestro2" should be updated with the new version number, releasepage, url and md5sum. The url should point to the Maestro Web API jar.

## Building the documentation

The documentation is automatically on push by readthedocs.io.

Local build is carried out through the make.bat/Makefile within the docs repository. It requires Sphinx and
sphinxcontrib-plantuml
