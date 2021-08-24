import os
import testutils
import subprocess
import requests
import time
import tempfile
import re
from zipfile import ZipFile

def testVerificationEntryPoint(basicUrl, baseResourcePath):
    testutils.printSection("WEB API verify algorithm")

    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    resourcesPath = os.path.join(baseResourcePath, "verify_algorithm")
    with open(os.path.join(resourcesPath, "masterModel.conf")) as f:
        payloadString = f.read()

    response = requests.post(f"{basicUrl}/verifyAlgorithm", data=payloadString, headers={'Content-Type': 'text/plain'})
    testutils.ensureResponseOk(response)

    actualResultJson = response.json()

    if(actualResultJson["verifiedSuccessfully"]):
        raise Exception("The algorithm should not have been verified successfully.")
    else:
        print("SUCCESS: The algorithm did not verify as expected.")

def testVisualizationEntryPoint(basicUrl, baseResourcePath):
    testutils.printSection("WEB API visualize traces")

    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    resourcesPath = os.path.join(baseResourcePath, "visualize_traces")
    with open(os.path.join(resourcesPath, "masterModel.conf")) as f:
        payloadString = f.read()

    response = requests.post(f"{basicUrl}/visualizeTrace", data=payloadString, headers={'Content-Type': 'text/plain'})
    testutils.ensureResponseOk(response)
   
    zipFilePath = os.path.join(tempDirectory, "actual_zip_result.zip")
    chunk_size = 128
    with open(zipFilePath, 'wb') as fd:
        for chunk in response.iter_content(chunk_size=chunk_size):
            fd.write(chunk)
    print("Wrote zip file to: " + zipFilePath)

    with ZipFile(zipFilePath, 'r') as zipObj:
        if not any(".mp4" in fileName for fileName in zipObj.namelist()):
            raise Exception("Expected at least one mp4 file visualizing the trace.")
        else:
            print("SUCCESS: at least one mp4 file visualizing a trace was returned.")

port = 0
parser = argparse.ArgumentParser(prog='Example of Maestro Master Web Interface', usage='%(prog)s [options]')
parser.add_argument('--path', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")
parser.add_argument('--port', help='Maestro connection port')
parser.set_defaults(port=port)


# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

relativePath = os.path.abspath(os.path.join(r"../maestro-webapi/target/", "maestro-webapi*.jar"))
jarPath = os.path.abspath(args.path) if str(args.path) != "None" else testutils.findJar(relativePath)


if not os.path.isfile(jarPath):
    raise Exception(f"The path does not exist: {jarPath}")

cmd = f"java -jar {jarPath} -p {str(port)}"

# Start the server as a subprocess and pipe stdout
proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)

# If port '0' is specified the server will acquire the port and write the port number to stdout as: '<' + 'port-number' + '>'.
if port == 0:
    port = testutils.acquireServerDefinedPortFromStdio(proc)
basicUrl = f"http://localhost:{str(port)}"

try:
    maxWait = 10
    while maxWait > 0:
        try:
            r = requests.get(basicUrl+"/version")
            if r.status_code == 200:
                print("Version: " + r.text)
                break
        except requests.exceptions.ConnectionError as x:
            print("Failed to connect: " + x.__class__.__name__)
            time.sleep(1)
            maxWait -= 1
    if(maxWait == 0):
        raise Exception("Unable to connect to host")

    baseResourcePath = "scenario_controller_resources"
    testVerificationEntryPoint(basicUrl, baseResourcePath)
    testVisualizationEntryPoint(basicUrl, baseResourcePath)
finally:
   proc.terminate()
