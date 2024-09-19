# Compilation

~~~bash
cmake .
make
~~~

## Windows

Install i.e.:

1. msys2 with mingw64 (https://www.msys2.org/)
2. Follow the guide at https://www.msys2.org/
   Note that `c:\msys64\msys2.exe` is the prompt used to install with pacman 
   The prompt at `c:\msys64\mingw64.exe` is what should be used for compilation
3. in `c:\msys64\msys2.exe` install the following:
  ```bash
pacman -Syu
pacman -Su
pacman -S mingw-w64-x86_64-toolchain
pacman -S base-devel
pacman -S mingw-w64-x86_64-cmake mingw-w64-x86_64-make
  ```
4. in `c:\msys64\mingw64.exe` run
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

## Linux (ubuntu)

Install:

```bash
apt install git libzip-dev build-essential cmake
```