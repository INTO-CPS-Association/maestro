# Compilation

~~~bash
cmake .
make
~~~

## Windows

Install i.e.:

1. msys with mingw64 (https://www.msys2.org/)
2. Follow the guide at https://www.msys2.org/
3. Add C:\msys64\mingw64\bin
4. Add C:\msys64\usr\bin
5. Run pacman -S mingw-w64-x86_64-cmake in the MSYS terminal Make sure to use the mingw64 terminal

Use the cmake profile `-G"MSYS Makefiles"`

To check what libraries needs to be in the path in case the program must be launched from elsewhere:

~~~bash
objdump -p sim-dse/cpp/program/sim.exe | grep "DLL Name:"
~~~

## Linux (ubuntu)

Install:

```bash
apt install git libzip-dev build-essential cmake
```