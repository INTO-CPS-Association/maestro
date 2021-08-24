import os
import testutils
import subprocess
import requests
import time
import tempfile
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

def webApiTest(jarPath):
    port = 8082

    # Check if port is free
    if testutils.is_port_in_use(port):
        print("Port %s is already in use. Finding free port" % port)
        port = testutils.find_free_port()
        print("New port is: %s" % port)

    cmd = f"java -jar {jarPath} -p {str(port)}"
    proc = subprocess.Popen(cmd, shell=True)
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
        print("Sucessfully tested scenario verification WEB API commands")
    finally:
        proc.terminate()

def verifyAlgorithmTest(SCR_path, jarPath):
    testutils.printSection("CLI verify algorithm")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    masterModelPath = os.path.join(SCR_path, "verify_algorithm", "masterModel.conf")
    cmd = "java -jar {0} verify-algorithm {1} -output {2}".format(jarPath, masterModelPath, temporary)
    func = lambda: True
    testutils.testCliCommandWithFunc(cmd, func)

def visualizeTracesTest(SCR_path, jarPath):
    testutils.printSection("CLI visualize traces")
    temporary = tempfile.mkdtemp()
    print(f"Temporary directory: {temporary}")
    masterModelPath = os.path.join(SCR_path, "visualize_traces", "masterModel.conf")
    cmd = "java -jar {0} visualize-traces {1} -output {2}".format(jarPath, masterModelPath, temporary)
    func = lambda: True
    testutils.testCliCommandWithFunc(cmd, func)

def cliTest(jarPath):
    SCR_path = "scenario_controller_resources"
    verifyAlgorithmTest(SCR_path, jarPath)
    visualizeTracesTest(SCR_path, jarPath)
    print("Sucessfully tested scenario verification CLI commands")



# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

webApiJarPath = testutils.findJar(os.path.abspath(os.path.join(r"../maestro-webapi/target/", "maestro-webapi*.jar")))
if not os.path.isfile(webApiJarPath):
    raise Exception(f"Unable to locate jar: {webApiJarPath}")
webApiTest(webApiJarPath)

## guru.nidi.graphviz.engine fails to initialize if it has just been used e.g. by running webApiTest before cliTest or the other way around. Therefore only one can be run at the time.
# cliJarPath = testutils.findJar(os.path.abspath(os.path.join(r"../maestro/target/", "maestro-*-jar-with-dependencies.jar")))
# if not os.path.isfile(cliJarPath):
#     raise Exception(f"Unable to locate jar: {cliJarPath}")
# cliTest(cliJarPath)