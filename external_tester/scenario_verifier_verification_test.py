import os
import testutils
import subprocess
import requests
import time
import tempfile
from pathlib import Path

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
        print("SUCCESS: Verification of the algorithm has been tested.")

def testVisualizationEntryPoint(basicUrl, baseResourcePath):
    testutils.printSection("WEB API visualize traces")

    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    resourcesPath = os.path.join(baseResourcePath, "visualize_traces")
    with open(os.path.join(resourcesPath, "masterModel.conf")) as f:
        payloadString = f.read()

    response = requests.post(f"{basicUrl}/visualizeTrace", data=payloadString, headers={'Content-Type': 'text/plain'})
    testutils.ensureResponseOk(response)

    videoFilePath = os.path.join(tempDirectory, "traces.mp4")

    with open(videoFilePath, 'wb') as wfile:
      wfile.write(response.content)

    print("Video file is located at: " + videoFilePath)

    fileSize = Path(videoFilePath).stat().st_size
   
    if fileSize < 100:
        raise Exception("The size of the returned file seems too small to contain any video.")
    else:
        print("SUCCESS: Trace visualization of the algorithm has been tested.")
    

def webApiTest(jarPath):
    port = 0
    cmd = f"java -jar {jarPath} -p {str(port)}"
    # Start the server as a subprocess and pipe stderr
    proc = subprocess.Popen(cmd, shell=True, stderr=subprocess.PIPE)

    # If port '0' is specified the server will acquire the port and write the port number to stderr as: '{' + 'port-number' + '}'.
    if port == 0:
        port = testutils.acquireServerDefinedPortFromStdio(proc)
    basicUrl = f"http://localhost:{str(port)}"
    print(f"Testing Web api of: {jarPath} with port: {str(port)}")

    try:
        maxWait = 20
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


def verifyAlgorithmTest(SCR_path, jarPath):
    testutils.printSection("CLI verify algorithm")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    masterModelPath = os.path.join(SCR_path, "verify_algorithm", "masterModel.conf")
    cmd = "java -jar {0} scenario-verifier verify-algorithm {1} -output {2}".format(jarPath, masterModelPath, temporary)
    func = lambda: True
    testutils.testCliCommandWithFunc(cmd, func)

def visualizeTracesTest(SCR_path, jarPath):
    testutils.printSection("CLI visualize traces")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    masterModelPath = os.path.join(SCR_path, "visualize_traces", "masterModel.conf")
    cmd = "java -jar {0} scenario-verifier visualize-traces {1} -output {2}".format(jarPath, masterModelPath, temporary)
    func = lambda: True
    testutils.testCliCommandWithFunc(cmd, func)

def cliTest(jarPath):
    SCR_path = "scenario_controller_resources"
    verifyAlgorithmTest(SCR_path, jarPath)
    visualizeTracesTest(SCR_path, jarPath)
    print("Sucessfully tested scenario verification CLI commands")

# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

webApiJarPath = testutils.findJar(os.path.abspath(os.path.join(r"../maestro-webapi/target/", "maestro-webapi*-bundle.jar")))
if not os.path.isfile(webApiJarPath):
    raise Exception(f"Unable to locate jar: {webApiJarPath}")
webApiTest(webApiJarPath)

## guru.nidi.graphviz.engine fails to initialize if it has just been used e.g. by running webApiTest before cliTest or the other way around. Therefore only one can be run at the time.
# cliJarPath = testutils.findJar(os.path.abspath(os.path.join(r"../maestro/target/", "maestro-*-jar-with-dependencies.jar")))
# if not os.path.isfile(cliJarPath):
#     raise Exception(f"Unable to locate jar: {cliJarPath}")
# cliTest(cliJarPath)