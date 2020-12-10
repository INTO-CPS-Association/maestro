#!/usr/bin/env python3
import json
import os
import filecmp


initializationConfigurationPath = "wt/mm.json"
simulationConfigurationPath = "wt/start_message.json"
# Update paths to FMUs
def retrieveConfiguration():
    config = json.load(open(initializationConfigurationPath))
    config["fmus"]["{crtl}"]=os.path.abspath('wt/watertankcontroller-c.fmu')
    config["fmus"]["{wt}"]=os.path.abspath('wt/singlewatertank-20sim.fmu')
    return config

def retrieveSimulationConfiguration():
    config = json.load(open(simulationConfigurationPath))
    return config

def compare(strPrefix, expected, actual):
    if os.path.exists(expected):
        if not filecmp.cmp(expected, actual):
            print("ERROR: %s: Files %s and %s do not match"% strPrefix, expected, actual)
        else:
            print("%s: Files match" % strPrefix)
    else:
        print("%s: No results file exists within wt. Results are not compared." % strPrefix)

def printSection(section):
    hashes = "###############################"
    print("\n" + hashes)
    print(section)
    print(hashes)
