#!/usr/bin/env python3
import argparse
import json
import sys
import testutils
import os
import subprocess
import shutil
import glob
from pathlib import Path
import platform


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
relativePath = os.path.abspath(os.path.join(r"../maestro/target/", "maestro-*-jar-with-dependencies.jar"))
path = os.path.abspath(args.path) if str(args.path) != "None" else testutils.findJar(relativePath)

if not os.path.isfile(path):
    print('The path does not exist')
    sys.exit()

# Interpreter outputs to directory from where it is executed.
outputsFileName = "outputs.csv"


def testWithCommand(outputs, cmd, temporary):
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")
    else:
        testutils.checkMablSpecExists(temporary.mablSpecPath)
        if not testutils.compareCSV('wt/result.csv', outputs):
            tempActualOutputs = temporary.dirPath + "actual_result.csv"
            print("Copying outputs file to temporary directory: " + tempActualOutputs)
            shutil.copyfile(outputs, tempActualOutputs)
            raise Exception("Results files do not match")


def cliSpecGen():
    testutils.printSection("CLI with Specification Generation")
    temporary = testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} import -output {1} --dump-intermediate sg1 {2} {3} -i -vi FMI2".format(path, temporary.dirPath,
                                                                                                temporary.initializationPath,
                                                                                                testutils.simulationConfigurationPath)
    testWithCommand(outputs, cmd, temporary)


def cliRaw():
    testutils.printSection("CLI Raw")
    temporary = testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} interpret -output {1} --dump-intermediate {1} {2} {3} -vi FMI2".format(path, temporary.dirPath,
                                                                                                testutils.mablExample,
                                                                                                testutils.folderWithModuleDefinitions)
    testWithCommand(outputs, cmd, temporary)


def cliExpansion():
    testutils.printSection("CLI Expansion")
    temporary = testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} interpret -output {1} --dump-intermediate {1} {2} -vi FMI2".format(path, temporary.dirPath,
                                                                                            testutils.mablExample)
    testWithCommand(outputs, cmd, temporary)


def cliExportCpp():
    testutils.printSection("CLI export cpp")
    temporary = testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} export -output {1} -vi FMI2 cpp {2}".format(path, temporary.dirPath, testutils.mablExample)
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True, check=True)
    cmd = "cmake"

    if os.name == 'nt' or 'Windows' in platform.system() or 'CYGWIN' in platform.system():
        cmd = cmd + " -G\"MSYS Makefiles\""

    cmd = cmd+" -B{0} -S{1}".format(Path(temporary.dirPath) / 'build', temporary.dirPath)
    print("Cmd: " + cmd)

    p = subprocess.run(cmd, shell=True, check=True)
    cmd = "make -j8 -C {0}".format(Path(temporary.dirPath) / 'build')
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True, check=True)

    output_csv_path = Path(temporary.dirPath) / 'build' / 'output.csv'
    runtime = {
        "DataWriter": [{
            "filename": str(output_csv_path),
            "type": "CSV"
        }]
    }
    runtime_path = Path(temporary.dirPath) / 'build' / 'runtime.json'
    with open(runtime_path, 'w') as f:
        f.write(json.dumps(runtime))

    cmd = "{0} -runtime {1}".format(Path(temporary.dirPath) / 'build' / 'sim', runtime_path)
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")


print("Testing CLI with specification generation of: " + path)
cliRaw()
cliSpecGen()
cliExpansion()
cliExportCpp()
