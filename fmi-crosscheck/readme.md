# FMI CrossCheck Results

Make sure you have this installed:
* Java
* Python 2.7 + pandoc, numpy

To produce new cross check results please do as follows in a *nix system:

1. Check out `https://svn.fmi-standard.org/fmi/branches/public`
2. Link all files from this folder to `CrossCheck_Results/FMI_2.0/CoSimulation` in the FMI svn:

`https://svn.fmi-standard.org/fmi/branches/public/CrossCheck_Results/FMI_2.0/CoSimulation`

3. Copy the new COE into the same folder as `coe.jar`
4. Run$ `./prepare-coe-release.sh`
5. Run:

```bash

./sims-darwin64.sh # on mac
./sims-linux32.sh  # on linux with i386 support and a Java 32bit VM in the path
./sims-linux64.sh  # on linux with amd64 support and a Java 64bit VM in the path

./sims-win32.bat   # on windows with Java 32bit
./sims-win64.bat   # on windows with Java 64bit

# post handling for windows
./sims-win32.post.sh # on a *nix system
./sims-win64.post.sh # on a *nix system
```

6. To generate an overview locally run: `python overview.py --version `java -jar coe.jar -V
7. Commit the results create but remember to delete e.g. `find . -name ".DS_Store" -delete` before
