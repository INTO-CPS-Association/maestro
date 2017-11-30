#!/bin/bash
set -e
echo "Syncing COE"



coe=coe.jar

if [ -f "$coe" ]
then
    echo "$coe is already downloaded."
else
    wget -v http://overture.au.dk/artifactory/into-cps/org/intocps/orchestration/coe/0.2.10/coe-0.2.10-jar-with-dependencies.jar -O $coe
fi



VERSION=`java -jar $coe -version`

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
		python cs-all.py --platform $i --version $VERSION 
#--oin
		if [ "$i" = "linux32" ] || [ "$i" = "linux64" ] || [ "$i" = "darwin64" ]; then 
				chmod +x "sims-$i.sh" 
		else
				chmod +x "sims-$i.post.sh" 
		fi;
done
