#!/usr/bin/env python3
import argparse
import sys
import testutils
import os
import shutil
import tempfile
import pathlib
import json

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

SCR_path = "scenario_controller_resources"

def validateAlgorithmExecution(outputcsv, expectedcsv):
    if os.path.exists(outputcsv) and testutils.compareCSV(outputcsv, expectedcsv):
        print("Succesfully executed the algorithm and returned output")
    else:
        Exception("Output returned from executing the algorithm did not match the expected output")

def validateCliSpecResult(outputs, temporary):
    testutils.checkMablSpecExists(temporary.mablSpecPath)
    if not testutils.compareCSV('wt/result.csv', outputs):
        tempActualOutputs=temporary.dirPath +  "actual_result.csv"
        print("Copying outputs file to temporary directory: " + tempActualOutputs)
        shutil.copyfile(outputs, tempActualOutputs)
        raise Exception("Results files do not match")

def cliSpecGen():
    testutils.printSection("CLI with Specification Generation")
    temporary=testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} import -output {1} --dump-intermediate sg1 {2} {3} -i -vi FMI2".format(path, temporary.dirPath, temporary.initializationPath, testutils.simulationConfigurationPath)
    testutils.testCliCommandWithFunc(cmd, lambda: validateCliSpecResult(outputs, temporary))


def cliRaw():
    testutils.printSection("CLI Raw")
    temporary=testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} interpret -output {1} --dump-intermediate {1} {2} {3} -vi FMI2".format(path, temporary.dirPath, testutils.mablExample, testutils.folderWithModuleDefinitions)
    testutils.testCliCommandWithFunc(cmd, lambda: validateCliSpecResult(outputs, temporary))


def cliExpansion():
    testutils.printSection("CLI Expansion")
    temporary=testutils.createAndPrepareTempDirectory()
    outputs = os.path.join(temporary.dirPath, outputsFileName)
    cmd = "java -jar {0} interpret -output {1} --dump-intermediate {1} {2} -vi FMI2".format(path, temporary.dirPath, testutils.mablExample)
    testutils.testCliCommandWithFunc(cmd, lambda: validateCliSpecResult(outputs, temporary))

def cliGenerateAlgorithmFromScenario():
    testutils.printSection("CLI generate algorithm from scenario")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    scenarioPath = os.path.join(SCR_path, "generate_from_scenario", "scenario.conf")
    cmd = "java -jar {0} generate-algorithm {1} -output {2}".format(path, scenarioPath, temporary)
    func = lambda: print("Succesfully generated algorithm from scenario") if(os.path.exists(os.path.join(temporary, "algorithm.conf"))) else lambda: (Exception("Algorithm was not returned"))
    testutils.testCliCommandWithFunc(cmd, func)

def cliGenerateAlgorithmFromMultiModel():
    testutils.printSection("CLI generate algorithm from multi model")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    resourcesPath = os.path.join(SCR_path, "generate_from_multi_model")
    multiModelPath = os.path.join(temporary, "multimodel.json")

    with open(os.path.join(resourcesPath, "multimodel.json"), "r") as jsonFile:
        multiModel = json.load(jsonFile)

    multiModel["fmus"]["{FMU}"]=pathlib.Path(os.path.abspath(os.path.join(resourcesPath, "rollback-test.fmu"))).as_uri()
    multiModel["fmus"]["{Controller}"]=pathlib.Path(os.path.abspath(os.path.join(resourcesPath, "rollback-end.fmu"))).as_uri()

    with open(multiModelPath, "w+") as jsonFile:
        json.dump(multiModel, jsonFile)
    
    cmd = "java -jar {0} generate-algorithm {1} -output {2}".format(path, multiModelPath, temporary)
    func = lambda: validateAlgorithmExecution(os.path.join(temporary, "outputs.csv"), os.path.join(resourcesPath, "expectedoutputs.csv"))
    testutils.testCliCommandWithFunc(cmd, func)

def cliExecuteAlgorithmFromExtendedMultiModel():
    testutils.printSection("CLI execute algorithm from multi model")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    resourcesPath = os.path.join(SCR_path, "execute_algorithm")

    multiModelPath = os.path.join(temporary, "multimodel.json")
    executionParametersPath = os.path.join(resourcesPath, "executionParameters.json")

    with open(os.path.join(resourcesPath,"extendedmultimodel.json"), "r") as jsonFile:
        multimodel = json.load(jsonFile)

    multimodel["fmus"]["{FMU}"]=pathlib.Path(os.path.abspath(os.path.join(SCR_path, "generate_from_multi_model", "rollback-test.fmu"))).as_uri()
    multimodel["fmus"]["{Controller}"]=pathlib.Path(os.path.abspath(os.path.join(SCR_path, "generate_from_multi_model", "rollback-end.fmu"))).as_uri()

    with open(multiModelPath, "w+") as jsonFile:
        json.dump(multimodel, jsonFile)

    cmd = "java -jar {0} execute-algorithm -em {1} -ep {2} -output {3} -di".format(path, multiModelPath, executionParametersPath, temporary)
    func = lambda: validateAlgorithmExecution(os.path.join(temporary, "outputs.csv"), os.path.join(resourcesPath, "expectedoutputs.csv"))
    testutils.testCliCommandWithFunc(cmd, func)

def cliExecuteAlgorithmFromMasterModel():
    testutils.printSection("CLI execute algorithm from master model")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    resourcesPath = os.path.join(SCR_path, "execute_algorithm")

    multiModelPath = os.path.join(temporary, "multimodel.json")
    executionParametersPath = os.path.join(resourcesPath, "executionParameters.json")
    masterModelPath = os.path.join(resourcesPath, "masterModel.conf")

    with open(os.path.join(resourcesPath,"multimodel.json"), "r") as jsonFile:
        multimodel = json.load(jsonFile)

    multimodel["fmus"]["{FMU}"]=pathlib.Path(os.path.abspath(os.path.join(SCR_path, "generate_from_multi_model", "rollback-test.fmu"))).as_uri()
    multimodel["fmus"]["{Controller}"]=pathlib.Path(os.path.abspath(os.path.join(SCR_path, "generate_from_multi_model", "rollback-end.fmu"))).as_uri()

    with open(multiModelPath, "w+") as jsonFile:
        json.dump(multimodel, jsonFile)

    cmd = "java -jar {0} execute-algorithm -em {1} -ep {2} -al {3} -output {4} -di".format(path, multiModelPath, executionParametersPath, masterModelPath, temporary)
    func = lambda: print("Succesfully executed the algorithm and returned output") if(validateAlgorithmExecution(os.path.join(temporary, "outputs.csv"), os.path.join(resourcesPath, "expectedoutputs.csv"))) else lambda: (Exception("No output was returned from executing the algorithm"))
    testutils.testCliCommandWithFunc(cmd, func)


print("Testing CLI of: " + path)
cliRaw()
cliSpecGen()
cliExpansion()
cliGenerateAlgorithmFromScenario()
cliGenerateAlgorithmFromMultiModel()
cliExecuteAlgorithmFromExtendedMultiModel()
cliExecuteAlgorithmFromMasterModel()