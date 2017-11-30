#!/bin/bash
set -e
echo "Syncing COE"

VERSION=0.1.12

platforms=( "darwin64" "linux32" "linux64" "win32" "win64" )

echo "Creating base directories *COE/$VERSION"
mkdir -p {darwin64,linux32,linux64,win32,win64}/COE/$VERSION

echo "Syncing Test FMUs into *COE/$VERSION"

for i in "${platforms[@]}"
do
		:
		SOURCE=$i/COE/$VERSION/
		TARGET=../../../Test_FMUs/FMI_2.0/CoSimulation/$i
		echo "Target is: $SOURCE"
		echo "Source is: $TARGET"
		rsync -ar --info=progress2 --include='*/' --include='*_ref.csv' --exclude='*' $TARGET/ $SOURCE
   # do whatever on $i
done
