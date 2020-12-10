#!/usr/bin/env python3

import testutils
import argparse
import os
import tempfile
import json
import subprocess
from collections import namedtuple

TempDirectoryData = namedtuple('TempDirectoryData', 'dirPath initializationPath resultPath mablSpecPath')

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('--path', required=True, type=str, help ='Path to the maestro2 web api jar')

args = parser.parse_args()

path = args.path


if not os.path.isfile(path):
    print('The path does not exist')
    sys.exit()

print("Testing CLI of: " + path)

def checkMablSpecExists(mablSpecPath):
    if os.path.isfile(mablSpecPath):
        print("MaBL Spec exists at: " + mablSpecPath)
    else:
        print("ERROR: MaBL spec does not exist at: " + mablSpecPath)


def createAndPrepareTempDirectory():
    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    config = testutils.retrieveConfiguration()
    print("Initialization config: %s" % json.dumps(config))
    newInitializationFilePath = tempDirectory+"/initialization.json"
    with open(newInitializationFilePath, 'w') as newInitFIle:
        json.dump(config, newInitFIle)
    resultPath = tempDirectory+"/actual_result.csv"
    mablSpecPath = tempDirectory + "/spec.mabl"
    return TempDirectoryData(tempDirectory, newInitializationFilePath, resultPath, mablSpecPath)


def terminate(p):
    p.kill()
    sys.exit()
try:
    def legacyCliSimConfig():
        testutils.printSection("Legacy CLI with Simulation Configuration")
        temporary= createAndPrepareTempDirectory()
        cmd1 = "java -jar {0} -o -c {1} -sc {2} -r {3}".format(path, temporary.initializationPath, testutils.simulationConfigurationPath, temporary.resultPath)
        print("Cmd: " + cmd1)
        p = subprocess.run(cmd1, shell=True)
        if p.returncode != 0:
            print("ERROR: In Executing %s" % cmd1)
        else:
            print("SUCCESS")
            testutils.compare("CSV", "wt/result.csv", temporary.resultPath)
            checkMablSpecExists(temporary.mablSpecPath)

    legacyCliSimConfig()

    def legacyCliStarttimeEndtime():
        testutils.printSection("Legacy CLI with starttime and endtime")
        temporary=createAndPrepareTempDirectory()
        simConfigParsed = testutils.retrieveSimulationConfiguration()
        cmd2 = "java -jar {0} -o -c {1} -s {2} -e {3} -r {4}".format(path, temporary.initializationPath, simConfigParsed['startTime'], simConfigParsed['endTime'], temporary.resultPath)
        print("Cmd: " + cmd2)
        p2 = subprocess.run(cmd2, shell=True)
        if p2.returncode != 0:
            print("ERROR: In executing %s" % cmd2)
        else:
            print("SUCCESS")
            testutils.compare("CSV", "wt/result.csv", temporary.resultPath)
            checkMablSpecExists(temporary.mablSpecPath)

    legacyCliStarttimeEndtime()


except Exception as x:
    print("ERROR: Exception: " + str(x))
