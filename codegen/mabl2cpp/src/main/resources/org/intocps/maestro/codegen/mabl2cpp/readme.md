# Building the co-simulation algorithm

# Prerequisites 
The build system must be in place to build the program

## Linux (ubuntu)

Install:

```bash
apt install git libzip-dev build-essential cmake
```

## Mac

Make sure xcode is installed and the license is accepted
```bash
brew install cmake
```

## Windows

Install i.e.:

1. msys2 with mingw64 (https://www.msys2.org/)
2. Follow the guide at https://www.msys2.org/
   * Note that `c:\msys64\msys2.exe` is the prompt used to install with pacman 
   * The prompt at `c:\msys64\mingw64.exe` is what should be used for compilation
3. in `c:\msys64\msys2.exe` install the following:
  ```bash
pacman -Syu
pacman -Su
pacman -S mingw-w64-x86_64-toolchain
pacman -S base-devel
pacman -S mingw-w64-x86_64-cmake mingw-w64-x86_64-make
pacman -S git
  ```


# Build
The `CMakeLists.txt` configuration is setup to use git to fetch dependent libraries and add them to the compilation. These are:
* the fmi header files from intocpsfmi
* libzip to support file extraction from the zip (*.fmu) archive 
* rapidjson to read the runtime configuration

# Linux/mac systems

~~~bash
cmake .
make
~~~

# Windows

Open the prompt via `c:\msys64\mingw64.exe` and run
```bash
cd $USERPROFILE/Downloads/my_project #assuming its in our download folder
ls # to check that CMakeLists.txt is in the current folder
cmake . # to generate the project
ninja  # to build
./sim.exe # run the program
```

If the application should be used in another shell then the mingw libraries needs to be available in the `PATH` so
1. Add C:\msys64\mingw64\bin
2. Add C:\msys64\usr\bin

Alternatively `make` can also be used instead of `ninja`
* then Run cmake with cmake profile `-G"MSYS Makefiles"` and use `make` to build

To check what libraries needs to be in the path in case the program must be launched from elsewhere:

~~~bash
objdump -p sim.exe | grep "DLL Name:"
~~~


# Extras

Windows is known to be working with these MSYS2 packages:
```bash
$ pacman -Qe
base 2022.06-1
base-devel 2022.12-2
filesystem 2023.02.07-2
git 2.46.1-1
mingw-w64-x86_64-binutils 2.43.1-1
mingw-w64-x86_64-cmake 3.30.3-1
mingw-w64-x86_64-crt-git 12.0.0.r264.g5c63f0a96-1
mingw-w64-x86_64-gcc 14.2.0-1
mingw-w64-x86_64-gdb 15.1-1
mingw-w64-x86_64-gdb-multiarch 15.1-1
mingw-w64-x86_64-headers-git 12.0.0.r264.g5c63f0a96-1
mingw-w64-x86_64-libmangle-git 12.0.0.r264.g5c63f0a96-1
mingw-w64-x86_64-libwinpthread-git 12.0.0.r264.g5c63f0a96-1
mingw-w64-x86_64-make 4.4.1-2
mingw-w64-x86_64-pkgconf 1~2.3.0-1
mingw-w64-x86_64-tools-git 12.0.0.r264.g5c63f0a96-1
mingw-w64-x86_64-winpthreads-git 12.0.0.r264.g5c63f0a96-1
mingw-w64-x86_64-winstorecompat-git 12.0.0.r264.g5c63f0a96-1
msys2-runtime 3.5.4-2
```