#!/usr/bin/env python3
import shutil
import sys

import testutils
import argparse
import os
import subprocess
import glob

def findJar():
    basePath = r"../maestro-webapi/target/"
    basePath = os.path.abspath(os.path.join(basePath, "maestro-webapi*.jar"))

    # try and find the jar file
    result = glob.glob(basePath)
    if len(result) == 0 or len(result) > 1:
        raise FileNotFoundError("Could not automatically find jar file please specify manually")

    return result[0]

def cliTest(tempDir, testCmd):
    print("Cmd: " + testCmd)
    p = subprocess.run(testCmd, shell=True)
    if p.returncode != 0:
        raise Exception(f"Error executing: {testCmd}")
    else:
        testutils.checkMablSpecExists(tempDir.mablSpecPath)
        if not testutils.compareCSV(expectedResultsFilePath, tempDir.resultPath):
            tempActualOutputs=tempDir.dirPath + actualResultsFileName
            print("Copying outputs file to temporary directory: " + tempActualOutputs)
            shutil.copyfile(tempDir.resultPath, tempActualOutputs)
            raise Exception("Results files do not match")

def legacyCliSimConfig():
    testutils.printSection("Legacy CLI with Simulation Configuration")
    temporary= testutils.createAndPrepareTempDirectory()
    cmd1 = "java -jar {0} -o -c {1} -sc {2} -r {3}".format(path, temporary.initializationPath, testutils.simulationConfigurationPath, temporary.resultPath)
    cliTest(temporary, cmd1)


def legacyCliStarttimeEndtime():
    testutils.printSection("Legacy CLI with starttime and endtime")
    temporary=testutils.createAndPrepareTempDirectory()
    simConfigParsed = testutils.retrieveSimulationConfiguration()
    cmd2 = "java -jar {0} -o -c {1} -s {2} -e {3} -r {4}".format(path, temporary.initializationPath, simConfigParsed['startTime'], simConfigParsed['endTime'], temporary.resultPath)
    cliTest(temporary, cmd2)

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('--path', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")

args = parser.parse_args()

# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

path = os.path.abspath(args.path) if str(args.path) != "None" else findJar()

if not os.path.isfile(path):
    print('The path does not exist')
    sys.exit()

actualResultsFileName = "actual_result.csv"
expectedResultsFilePath = 'wt/result.csv'

print("Testing CLI of: " + path)

legacyCliSimConfig()
legacyCliStarttimeEndtime()
