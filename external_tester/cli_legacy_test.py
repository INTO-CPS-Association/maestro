#!/usr/bin/env python3
import sys

import testutils
import argparse
import os
import tempfile
import json
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

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('--path', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")

args = parser.parse_args()

# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

path = os.path.abspath(args.path) if str(args.path) != "None" else findJar()

if not os.path.isfile(path):
    print('The path does not exist')
    sys.exit()

print("Testing CLI of: " + path)

def terminate(p):
    p.kill()
    sys.exit()
try:
    def legacyCliSimConfig():
        testutils.printSection("Legacy CLI with Simulation Configuration")
        temporary= testutils.createAndPrepareTempDirectory()
        cmd1 = "java -jar {0} -o -c {1} -sc {2} -r {3}".format(path, temporary.initializationPath, testutils.simulationConfigurationPath, temporary.resultPath)
        print("Cmd: " + cmd1)
        p = subprocess.run(cmd1, shell=True)
        if p.returncode != 0:
            print("ERROR: In Executing %s" % cmd1)
        else:
            print("SUCCESS")
            testutils.compare("CSV", "wt/result.csv", temporary.resultPath)
            testutils.checkMablSpecExists(temporary.mablSpecPath)

    legacyCliSimConfig()

    def legacyCliStarttimeEndtime():
        testutils.printSection("Legacy CLI with starttime and endtime")
        temporary=testutils.createAndPrepareTempDirectory()
        simConfigParsed = testutils.retrieveSimulationConfiguration()
        cmd2 = "java -jar {0} -o -c {1} -s {2} -e {3} -r {4}".format(path, temporary.initializationPath, simConfigParsed['startTime'], simConfigParsed['endTime'], temporary.resultPath)
        print("Cmd: " + cmd2)
        p2 = subprocess.run(cmd2, shell=True)
        if p2.returncode != 0:
            print("ERROR: In executing %s" % cmd2)
        else:
            print("SUCCESS")
            testutils.compare("CSV", "wt/result.csv", temporary.resultPath)
            testutils.checkMablSpecExists(temporary.mablSpecPath)

    legacyCliStarttimeEndtime()


except Exception as x:
    print("ERROR: Exception: " + str(x))
