import argparse
import json
import os
import subprocess
import sys
import time
from io import open

sys.path.append(os.getcwd() + '/..')

from jarunpacker import jar_unpacker
from resultcheck import check_results
from pathlib import Path
import shutil

parser = argparse.ArgumentParser(prog='PROG', usage='%(prog)s [options]')
parser.add_argument('--jar', help='jar', required=True)
parser.add_argument('--live', help='live output from API', action='store_true')
parser.set_defaults(live=False)

args = parser.parse_args()

classpathDir = jar_unpacker(args.jar)

classPathDirExtracted = Path(classpathDir) / Path("BOOT-INF") / Path("lib")
classPathDirExtracted = classPathDirExtracted.resolve()
# print("classPathDirExtracted: " + str(classPathDirExtracted))

coeJarPath = classPathDirExtracted / Path("coe-1.0.7-SNAPSHOT.jar")

# print("coeJarPath: " + str(coeJarPath))

cliArgs = json.load(open("cli_arguments.json"))

starttime = cliArgs["start_time"]
endtime = cliArgs["end_time"]
outputfile = cliArgs["output_file"]

config = json.load(open("config.json"))
for fmu in config["fmus"]:
    print (fmu)
    name = Path(config["fmus"][fmu])
    src = '../fmus' / name
    dest = Path(classpathDir) / name
    shutil.copy(src, dest)

stream = None
if args.live:
    stream = subprocess.PIPE
else:
    stream = open('api.log', 'w')

classPathInitial = ".:"

if os.name == 'nt':
    classPathInitial = ".;"

classpathJar = classPathInitial + str(classPathDirExtracted / Path("*"))
# print("classPathJar: " + classpathJar)

subprocess.run(["java", "-cp", str(
    classpathJar), "org.intocps.orchestration.coe.CoeMain", "--oneshot", "--configuration", "../config.json",
                "--starttime", str(
        starttime), "--endtime", str(endtime), "--result", outputfile],
               stdout=stream, stderr=stream, cwd=classpathDir)

args = parser.parse_args()

config = json.load(open("config.json", encoding='utf8'))

outputColumns = [c for c in config['connections']]
stepSize = float(config['algorithm']['size'])

if check_results(outputColumns, classpathDir / Path(outputfile), starttime, endtime, stepSize) == 1:
    print("Error")
    exit(1)

exit(0)
