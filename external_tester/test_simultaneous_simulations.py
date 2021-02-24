import argparse
import glob
import json
import os
import subprocess
import shutil
import tempfile
import time
from concurrent.futures.thread import ThreadPoolExecutor
import concurrent.futures
from threading import Lock

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


errors = []

def RunTests(threadCount):
    global errors
    errors = []

    with ThreadPoolExecutor(max_workers=threadCount) as ex:
        sims = {ex.submit(RunTest) for _ in range(threadCount)}

        for _ in concurrent.futures.as_completed(sims):
            pass

    for error in errors:
        raise Exception(error)


mutex = Lock()

def RunTest():
    # just to ensure that any file reads in the python size are not causing false positives
    with mutex:
        tempDirectory = tempfile.mkdtemp()
        config = testutils.retrieveConfiguration()

    # Create Session
    r = requests.get("http://localhost:8082/createSession")
    if not r.status_code == 200:
        errors.append("Could not create session")
        return
    sessionId = json.loads(r.text)['sessionId']

    # Init Session
    r = requests.post(f"http://localhost:8082/initialize/{sessionId}", json=config)
    if not r.status_code == 200:
        errors.append("Could not initialize")
        return

    initSessionId = json.loads(r.text)['sessionId']
    if initSessionId != sessionId:
        errors.append(f"Incorrect Init Session ID! Got: {initSessionId} Expected: {sessionId}")
        return

    r = requests.post(f"http://localhost:8082/executeViaCLI/{sessionId}", json={"executeViaCLI":True})
    if not r.status_code == 200:
            errors.append("Could not set executeViaCLI")
            return


    # Simulate
    with mutex:
        fileDestination = os.path.join(tempDirectory, "startMessage.json")
        shutil.copyfile("wt/start_message.json", fileDestination)

    r = requests.post(f"http://localhost:8082/simulate/{sessionId}", json=json.load(open(fileDestination)))
    if not r.status_code == 200:
        errors.append(f"Could not simulate: {r.text}")
        return

    simSessionId = json.loads(r.text)['sessionId']
    if simSessionId != sessionId:
        errors.append(f"Incorrect Simulate Session ID! Got: {simSessionId} Expected: {sessionId}")
        return

    # Plain Results
    r = requests.get(f"http://localhost:8082/result/{sessionId}/plain")
    if not r.status_code == 200:
        errors.append(f"Could not get plain results: {r.text}")
        return

    csv = r.text
    csvFilePath = os.path.join(tempDirectory, "actual_result.csv")
    with open(csvFilePath, "w+") as f:
        f.write(csv.replace("\r\n", "\n"))

    r = requests.get(f"http://localhost:8082/result/{sessionId}/zip", stream=true)
    if not r.status_code == 200:
           errors.append(f"Could not get zip results: {r.text}")
           return
    print ("Result response code '%d" % (r.status_code))
    result_zip_path = "actual_zip_result.zip"
    zipFilePath = os.path.join(tempDirectory,result_zip_path)
    chunk_size = 128
    with open(zipFilePath, 'wb') as fd:
       for chunk in r.iter_content(chunk_size=chunk_size):
           fd.write(chunk)
    print("Wrote zip file file to: " + zipFilePath)

    # just to ensure that any file reads in the python size are not causing false positives
    with mutex:
        fileDestination = os.path.join(tempDirectory, "expected_result.csv")
        shutil.copyfile("wt/result.csv", fileDestination)

    if not testutils.compare("CSV", fileDestination, csvFilePath):
        errors.append(f"CSV files did not match for session {sessionId}!")
        return



    # cleanup after myself
    shutil.rmtree(tempDirectory)

    # Destroy Session
    r = requests.get(f"http://localhost:8082/destroy/{sessionId}")

    if not r.status_code == 200:
        errors.append(f"Could not destroy session {sessionId}: {r.text}")
        return


if "__main__":
    args = GetParameters()

    os.chdir(os.path.dirname(os.path.realpath(__file__)))
    path = os.path.abspath(args.path) if str(args.path) != "None" else FindJar()

    #p = StartCOE(path, args.port)
    #time.sleep(20) # give some time for the COE to actually start

    threadCounts = [1, 2, 4, 8, 10, 12, 16]

    try:
        for i in threadCounts:
            print(f"Running test with {i} threads")
            RunTests(i)
            print(f"{i} threads tests passed!")
    finally:
        print("stopping")
        #StopCOE(p)
