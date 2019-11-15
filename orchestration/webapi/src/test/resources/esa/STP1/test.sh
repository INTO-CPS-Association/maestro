#!/bin/bash

echo "Test Basic Simulation CLI"

if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    echo "Please call with path of coe and path to the result folder"
fi

mkdir -p test-run




cp $1 test-run
cp $2/*.fmu test-run

cd test-run

pwd

jar=$(basename $1)

echo Jar is $jar

version=$(java -jar $jar --version)

echo "Version $version"

starttime=$(jq ".start_time" ../cli_arguments.json)
endtime=$(jq ".end_time" ../cli_arguments.json)
outputfile=$(jq ".output_file" ../cli_arguments.json)

echo "Time interval from ${starttime} to ${endtime}"

java -jar $jar --oneshot --configuration ../config.json --result result.csv --starttime $starttime --endtime $endtime --result $outputfile

cd ..
python3 check.py --csv test-run/result.csv --config config.json --args cli_arguments.json