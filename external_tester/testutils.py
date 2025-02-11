#!/usr/bin/env python3
import json
import os
import filecmp
import tempfile
import pathlib
import csv
from collections import namedtuple
import glob
import socket
import re
import subprocess
from threading import Thread 
import time
import string

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

def compareCSV(expected, actual):
    if os.path.exists(expected):
        convert(expected)
        compareResult = True

        with open(expected) as expectedCSV, open(actual) as actualCSV:
            expectedReader = csv.DictReader(expectedCSV)
            
            expectedReader.fieldnames
            actualReader = csv.DictReader(actualCSV)

            if not(set(expectedReader.fieldnames) == set(actualReader.fieldnames)):
                print("Columns does not match!")
                compareResult = False

            elif not(expectedReader.__sizeof__() == actualReader.__sizeof__()):
                 print(f"Column lengths does not match! {expectedReader.__sizeof__()} {actualReader.__sizeof__()}")
                 compareResult = False

            else: 
                for actualRow, expectedRow in zip(actualReader, expectedReader):
                    if not(compareResult):
                        break
                    for expectedColumn in expectedReader.fieldnames:
                        if not(actualRow[expectedColumn] == expectedRow[expectedColumn]):
                            print(f"Value mismatch for column '{expectedColumn}' on line {expectedReader.line_num}. Expected value: {actualRow[expectedColumn]} and actual value: {expectedRow[expectedColumn]}")
                            compareResult = False

        if not compareResult:
            print("ERROR: CSV files {} and {} do not match!".format(expected, actual))
            return False
        else:
            print("CSV files match")
            return True
    else:
        print(f"ERROR: {expected} doest not exist!")
        return False

def compareTexts(expectedText, actualText):
    remove = str.maketrans('', '', string.whitespace)
    expectedText = expectedText.translate(remove)
    actualLines = actualText.translate(remove)
    return expectedText == actualLines

def compareFiles(file1, file2):
    expectedFile = open(file1, 'r')
    actualFile = open(file2, 'r')
    expectedLines = expectedFile.readlines()
    actualLines = actualFile.readlines()
    expectedLines = "".join(expectedLines)
    actualLines = "".join(actualLines)
    expectedFile.close()
    actualFile.close()
    return compareTexts(expectedLines, actualLines)

def compare(strPrefix, expected, actual):
    if os.path.exists(expected):
        convert(expected)

        compareResult = filecmp.cmp(expected, actual)
        if not compareResult:
            compareRes = compareFiles(expected, actual)
            if not compareRes:
                print("ERROR: {}: Files {} and {} do not match".format(strPrefix, expected, actual))
                return False
            else:
                print("%s: Files match" % strPrefix)
                return True
        else:
            print("%s: Files match" % strPrefix)
            return True
    else:
        print("ERROR: %s: No results file exists. Results are not compared." % strPrefix)
        return False

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

def findJar(relativePath):
    # try and find the jar file
    result = glob.glob(relativePath)
    if len(result) == 0 or len(result) > 1:
        raise FileNotFoundError("Could not automatically find jar file please specify manually")
    return result[0]

def is_port_in_use(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(('localhost', port)) == 0

def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        return s.getsockname()[1]

def ensureResponseOk(response):
    if not response.status_code == 200:
        raise Exception(f"Request returned error code: {response.status_code} with text: {response.text}")

def testCliCommandWithFunc(cmd, func):
    print("Cmd: " + cmd)
    p = subprocess.run(cmd, shell=True)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")
    else:
        func()

def acquireServerDefinedPortFromStdio(proc):
    # If port '0' is specified the server will acquire the port and write the port number to stderr as: '{' + 'port-number' + '}'.
    # Therefore match the pattern and retrieve the port number from stderr

    # readline from popen is blocking so it needs its own thread to be able to time out if no port number is found within a set time.
    linebuffer=[]
    def reader(stderr, buffer):
        while True:
            line = stderr.readline()
            if line:
                buffer.append(line)
            else:
                break

    t=Thread(target=reader, args=(proc.stderr, linebuffer))
    t.daemon=True # Ensures the thread terminates with the main thread.
    t.start()
    timeoutInSecs = 0
    while timeoutInSecs < 20:
        if linebuffer:
            stringLine = linebuffer.pop(0).decode("utf-8")
            print(str(stringLine))
            match = re.search("(?<=\{)[0-9]+(?=\})", stringLine)
            if match:
                # Return the server port
                return match.group()
        time.sleep(1)
        timeoutInSecs += 1

    raise Exception("Unable to locate serverport in stdout") 
