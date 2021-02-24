#!/usr/bin/env python3
import argparse
import sys
import testutils
import os
import subprocess
import shutil
import glob

def findJar():
    basePath = r"../maestro/target/"
    basePath = os.path.abspath(os.path.join(basePath, "maestro-*-jar-with-dependencies.jar"))

    # try and find the jar file
    result = glob.glob(basePath)
    if len(result) == 0 or len(result) > 1:
        raise FileNotFoundError("Could not automatically find jar file please specify manually")

    return result[0]

parser = argparse.ArgumentParser(prog='Example of Maestro CLI', usage='%(prog)s [options]')
parser.add_argument('--path', type=str, default=None, help="Path to the Maestro CLI jar (Can be relative path)")

args = parser.parse_args()

# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

path = os.path.abspath(args.path) if str(args.path) != "None" else findJar()

if not os.path.isfile(path):
    print('The path does not exist')
    sys.exit()

# Interpreter outputs to directory from where it is executed.
outputsFileName = "outputs.csv"

print("Testing CLI with specification generation of: " + path)

def cliSpecGen():
    testutils.printSection("CLI with Specification Generation")
    temporary=testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    #cmd = "java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 {0} --dump-simple {1} --dump-intermediate {1} -sg1 {2} {3} -i -v FMI2".format(path, temporary.dirPath, temporary.initializationPath, testutils.simulationConfigurationPath)
    cmd = "java -jar {0} --dump-simple {1} --dump-intermediate {1} -sg1 {2} {3} -i -v FMI2".format(path, temporary.dirPath, temporary.initializationPath, testutils.simulationConfigurationPath)
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")
    else:
        print("SUCCESS")
        testutils.checkMablSpecExists(temporary.mablSpecPath)

        if not testutils.compare("CSV", "wt/result.csv", outputs):
            tempActualOutputs=temporary.dirPath + "/actual_" + outputs
            print("Copying outputs file to temporary directory: " + tempActualOutputs)
            shutil.copyfile(outputs, tempActualOutputs)
            raise Exception("Results files do not match")

cliSpecGen()


def cliRaw():
    testutils.printSection("CLI Raw")
    temporary=testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} --dump-simple {1} --dump-intermediate {1} {2} {3} -i -v FMI2".format(path, temporary.dirPath, testutils.mablExample, testutils.folderWithModuleDefinitions)
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")
    else:
        print("SUCCESS")
        testutils.checkMablSpecExists(temporary.mablSpecPath)
        if not testutils.compare("CSV", "wt/result.csv", outputs):
            tempActualOutputs=temporary.dirPath + "/actual_" + outputs
            print("Copying outputs file to temporary directory: " + tempActualOutputs)
            shutil.copyfile(outputs, tempActualOutputs)
            raise Exception("Results files do not match")

cliRaw()


def cliExpansion():
    testutils.printSection("CLI Expansion")
    temporary=testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} --dump-simple {1} --dump-intermediate {1} {2} -i -v FMI2".format(path, temporary.dirPath, testutils.mablExample)
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")
    else:
        print("SUCCESS")
        testutils.checkMablSpecExists(temporary.mablSpecPath)
        if not testutils.compare("CSV", "wt/result.csv", outputs):
            tempActualOutputs=temporary.dirPath + "/actual_" + outputs
            print("Copying outputs file to temporary directory: " + tempActualOutputs)
            shutil.copyfile(outputs, tempActualOutputs)
            raise Exception("Results files do not match")

cliExpansion()