#!/usr/bin/env python3
import argparse
import testutils
import os
import subprocess
import shutil

parser = argparse.ArgumentParser(prog='Example of Maestro CLI', usage='%(prog)s [options]')
parser.add_argument('--path', required=True, type=str, help ='Path to the Maestro CLI jar')

args = parser.parse_args()

path = args.path

if not os.path.isfile(path):
    print('The path does not exist')
    sys.exit()

# Interpreter outputs to directory from where it is executed.
outputs = "outputs.csv"

def deleteOutputsFile(outputsFile):
    if os.path.exists(outputs) and os.path.isfile(outputs):
        print("Removing file: " + outputs)
        os.remove(outputs)


print("Testing CLI with specification generation of: " + path)
try:
    deleteOutputsFile(outputs)
    def cliSpecGen():
        testutils.printSection("CLI with Specification Generation")
        temporary=testutils.createAndPrepareTempDirectory()
        cmd = "java -jar {0} --dump {1} --dump-intermediate {1} -sg1 {2} {3} -i -v FMI2".format(path, temporary.dirPath, temporary.initializationPath, testutils.simulationConfigurationPath)
        print("Cmd: " + cmd)
        p = subprocess.run(cmd, shell=True)
        if p.returncode != 0:
            print("ERROR: In Executing %s" % cmd)
            return False
        else:
            print("SUCCESS")
            testutils.checkMablSpecExists(temporary.mablSpecPath)
            if not testutils.compare("CSV", "wt/result.csv", outputs):
                tempActualOutputs=temporary.dirPath + "/actual_" + outputs
                print("Copying outputs file to temporary directory: " + tempActualOutputs)
                shutil.copyfile(outputs, tempActualOutputs)

    cliSpecGen()
    deleteOutputsFile(outputs)

    def cliRaw():
        testutils.printSection("CLI Raw")
        temporary=testutils.createAndPrepareTempDirectory()
        cmd = "java -jar {0} --dump {1} --dump-intermediate {1} {2} {3} -i -v FMI2".format(path, temporary.dirPath, testutils.mablExample, testutils.folderWithModuleDefinitions)
        print("Cmd: " + cmd)
        p = subprocess.run(cmd, shell=True)
        if p.returncode != 0:
            print("ERROR: In Executing %s" % cmd)
        else:
            print("SUCCESS")
            testutils.checkMablSpecExists(temporary.mablSpecPath)
            if not testutils.compare("CSV", "wt/result.csv", outputs):
                tempActualOutputs=temporary.dirPath + "/actual_" + outputs
                print("Copying outputs file to temporary directory: " + tempActualOutputs)
                shutil.copyfile(outputs, tempActualOutputs)

    cliRaw()
    deleteOutputsFile(outputs)

    def cliExpansion():
        testutils.printSection("CLI Expansion")
        temporary=testutils.createAndPrepareTempDirectory()
        cmd = "java -jar {0} --dump {1} --dump-intermediate {1} {2} -i -v FMI2".format(path, temporary.dirPath, testutils.mablExample)
        print("Cmd: " + cmd)
        p = subprocess.run(cmd, shell=True)
        if p.returncode != 0:
            print("ERROR: In Executing %s" % cmd)
        else:
            print("SUCCESS")
            testutils.checkMablSpecExists(temporary.mablSpecPath)
            if not testutils.compare("CSV", "wt/result.csv", outputs):
                tempActualOutputs=temporary.dirPath + "/actual_" + outputs
                print("Copying outputs file to temporary directory: " + tempActualOutputs)
                shutil.copyfile(outputs, tempActualOutputs)

    cliExpansion()
    deleteOutputsFile(outputs)

except Exception as x:
    print("ERROR: Exception: " + str(x))
