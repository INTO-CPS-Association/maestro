import argparse
import glob
import json
import os
import subprocess
import shutil
import tempfile
import time
from concurrent.futures.thread import ThreadPoolExecutor

import requests

import testutils


def GetParameters():
    parser = argparse.ArgumentParser(prog="Tests the maestro web interface to ensure it can properly handle multiple simulations simulations", usage='%(prog)s [options]')
    parser.add_argument('--path', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")
    parser.add_argument('--port', help='Maestro connection port')
    parser.set_defaults(port=8082)

    return parser.parse_args()

def FindJar():
    basePath = r"../maestro-webapi/target/"
    basePath = os.path.abspath(os.path.join(basePath, "maestro-webapi*.jar"))

    # try and find the jar file
    result = glob.glob(basePath)
    if len(result) == 0 or len(result) > 1:
        raise FileNotFoundError("Could not automatically find jar file please specify manually")

    return result[0]

def StartCOE(path, port):
    return subprocess.Popen(f"java -jar {path} -p {port}", shell=True)

def StopCOE(p):
    p.terminate()

def RunTests(threadCount):
    with ThreadPoolExecutor(max_workers=threadCount) as ex:
        _ = {ex.submit(RunTest) for _ in range(threadCount)}

def RunTest():
    config = testutils.retrieveConfiguration()

    # Create Session
    r = requests.get("http://localhost:8082/createSession")
    if not r.status_code == 200:
        raise Exception("Could not create session")
    sessionId = json.loads(r.text)['sessionId']

    # Init Session
    r = requests.post(f"http://localhost:8082/initialize/{sessionId}", json=config)
    if not r.status_code == 200:
        raise Exception("Could not initialize")

    initSessionId = json.loads(r.text)['sessionId']
    if initSessionId != sessionId:
        raise Exception("Incorrect Init Session ID!")

    # Simulate
    r = requests.post(f"http://localhost:8082/simulate/{sessionId}", json=json.load(open("wt/start_message.json")))
    if not r.status_code == 200:
        raise Exception(f"Could not simulate: {r.text}")

    simSessionId = json.loads(r.text)['sessionId']
    if simSessionId != sessionId:
        raise Exception("Incorrect Simulate Session ID!")

    # Plain Results
    r = requests.get(f"http://localhost:8082/result/{sessionId}/plain")
    if not r.status_code == 200:
        raise Exception(f"Could not get plain results: {r.text}")

    tempDirectory = tempfile.mkdtemp()

    csv = r.text
    csvFilePath = os.path.join(tempDirectory, "actual_result.csv")
    with open(csvFilePath, "w+") as f:
        f.write(csv.replace("\r\n", "\n"))

    if not testutils.compare("CSV", "wt/result.csv", csvFilePath):
        raise Exception("CSV files did not match!")

    # cleanup after myself
    shutil.rmtree(tempDirectory)

    # Destroy Session
    r = requests.get(f"http://localhost:8082/destroy/{sessionId}")

    if not r.status_code == 200:
        raise Exception(f"Could not destroy: {r.text}")

if "__main__":
    args = GetParameters()

    os.chdir(os.path.dirname(os.path.realpath(__file__)))
    path = os.path.abspath(args.path) if str(args.path) != "None" else FindJar()

    p = StartCOE(path, args.port)
    time.sleep(2) # give some time for the COE to actually start

    threadCounts = [1, 2, 4, 6, 8, 10]

    try:
        for i in threadCounts:
            print(f"Running test with {i} threads")
            RunTests(i)
            print(f"{i} threads tests passed!")
    finally:
        StopCOE(p)
