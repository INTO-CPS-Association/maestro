#!/usr/bin/env python3
import json
import os
import filecmp
import tempfile
import pathlib
from collections import namedtuple

TempDirectoryData = namedtuple('TempDirectoryData', 'dirPath initializationPath resultPath mablSpecPath')

initializationConfigurationPath = "wt/mm.json"
simulationConfigurationPath = "wt/start_message.json"
mablExample = "wt/example1.mabl"
folderWithModuleDefinitions =  "../typechecker/src/main/resources/org/intocps/maestro/typechecker/"
mablExpansionExample="wt/expansion_example.mabl"

# Update paths to FMUs
def retrieveConfiguration():
    config = json.load(open(initializationConfigurationPath))
    config["fmus"]["{crtl}"]=pathlib.Path(os.path.abspath('wt/watertankcontroller-c.fmu')).as_uri()
    config["fmus"]["{wt}"]=pathlib.Path(os.path.abspath('wt/singlewatertank-20sim.fmu')).as_uri()
    return config

def retrieveSimulationConfiguration():
    config = json.load(open(simulationConfigurationPath))
    return config

def compare(strPrefix, expected, actual):
    if os.path.exists(expected):
        convert(expected)

        compareResult = filecmp.cmp(expected, actual)
        if not compareResult:
            print("ERROR: {}: Files {} and {} do not match".format(strPrefix, expected, actual))
            return False
        else:
            print("%s: Files match" % strPrefix)
            return True
    else:
        print("%s: No results file exists within wt. Results are not compared." % strPrefix)

def convert(expected):
    # Converts the expected results.csv to the current OS line ending format as COE outputs using current OS line endings
    with open(expected, 'r') as f:
        content = f.read()

    with open(expected, 'w+') as f:
        f.write(content)
        
def printSection(section):
    hashes = "###############################"
    print("\n" + hashes)
    print(section)
    print(hashes)

def createAndPrepareTempDirectory():
    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    config = retrieveConfiguration()
    print("Initialization config: %s" % json.dumps(config))
    newInitializationFilePath = tempDirectory+"/initialization.json"
    with open(newInitializationFilePath, 'w') as newInitFIle:
        json.dump(config, newInitFIle)
    resultPath = tempDirectory+"/actual_result.csv"
    mablSpecPath = tempDirectory + "/spec.mabl"
    return TempDirectoryData(tempDirectory, newInitializationFilePath, resultPath, mablSpecPath)

def checkMablSpecExists(mablSpecPath):
    if os.path.isfile(mablSpecPath):
        print("MaBL Spec exists at: " + mablSpecPath)
    else:
        raise Exception(f"Mable spec does not exist at {mablSpecPath}")
