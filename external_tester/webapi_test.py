import argparse
import json
import os
import time
import subprocess
import requests
import tempfile
from contextlib import closing
from zipfile import ZipFile
import threading
import websocket
import testutils
import pathlib

websocketopen = False
socketFile = None

def cleanUp(p):
    p.terminate()
    if socketFile:
        socketFile.close()

def ws_thread(*args):
    print("ws_thread: %s" % args[0])
    ws = websocket.WebSocketApp(args[0], on_open = ws_open, on_message = args[1], on_close=ws_close)
    ws.run_forever()

def ws_open(ws):
    print("websocket opened")
    global websocketopen
    websocketopen = True

def ws_close(ws):
    print("websocket closed")

def testSimulationController(basicUrl):
    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    # Update paths to FMUs
    config = testutils.retrieveConfiguration()
    print("CONFIG: %s" % json.dumps(config))


    testutils.printSection("CREATE SESSION")
    r = requests.get(basicUrl + "/createSession")
    if not r.status_code == 200:
        raise Exception("Could not create session")

    status = json.loads(r.text)
    print ("Session '%s', data=%s'" % (status["sessionId"], status))

    # Initialize
    testutils.printSection("INITIALIZE")

    r = requests.post(basicUrl + "/initialize/" + status["sessionId"], json=config)
    if not r.status_code == 200:
        raise Exception("Could not initialize")

    print ("Initialize response code '%d, data=%s'" % (r.status_code, r.text))
    sessionID = status["sessionId"]

    # Weboscket support
    testutils.printSection("WEBSOCKET")
    wsurl = "ws://localhost:{port}/attachSession/{session}".format(port=port, session=sessionID)
    print("Connecting to websocket with url: " + wsurl)
    wsResult = os.path.join(tempDirectory, "wsActualResult.txt")
    socketFile = open(wsResult, "w")
    print("Writing websocket output to: " + wsResult)

    wsOnMessage = lambda ws, msg: socketFile.write(msg)
    wsThread=threading.Thread(target=ws_thread, args=(wsurl,wsOnMessage,))
    wsThread.start()
    webSocketWaitAttempts = 0
    while not websocketopen and webSocketWaitAttempts < 5:
        webSocketWaitAttempts+=1
        print("WS: Awaiting websocket opening")
        time.sleep(0.5)

    if(not websocketopen):
        raise Exception("Unable to open socket connection")

    #Simulate
    testutils.printSection("SIMULATE")
    r = requests.post(basicUrl + "/simulate/" + sessionID, json=json.load(open("wt/start_message.json")))
    if not r.status_code == 200:
        raise Exception(f"Could not simulate: {r.text}")

    print ("Simulate response code '%d, data=%s'" % (r.status_code, r.text))
    wsThread.join()
    socketFile.close()

    #Compare results
    testutils.printSection("WS OUTPUT COMPARE")
    if(not testutils.compare("WS", "wt/wsexpected.txt", wsResult)):
        raise Exception("Output files do not match.")

    #Get plain results
    testutils.printSection("PLAIN RESULT")
    r = requests.get(basicUrl + "/result/" + sessionID + "/plain")
    if not r.status_code == 200:
        raise Exception(f"Could not get plain results: {r.text}")

    print ("Result response code '%d" % (r.status_code))
    result_csv_path = "actual_result.csv"
    csv = r.text
    csvFilePath = os.path.join(tempDirectory, result_csv_path)
    with open(csvFilePath, "w+") as f:
        f.write(csv.replace("\r\n", "\n"))
        
    print("Wrote csv file to: " + csvFilePath)
    if not testutils.compareCSV("wt/result.csv", csvFilePath):
        raise Exception("CSV files did not match!")

    #Get zip results
    testutils.printSection("ZIP RESULT")
    r = requests.get(basicUrl + "/result/" + sessionID + "/zip", stream=True)
    if not r.status_code == 200:
        raise Exception(f"Could not get zip results: {r.text}")

    print ("Result response code '%d" % (r.status_code))
    result_zip_path = "actual_zip_result.zip"
    zipFilePath = os.path.join(tempDirectory, result_zip_path)
    chunk_size = 128
    with open(zipFilePath, 'wb') as fd:
        for chunk in r.iter_content(chunk_size=chunk_size):
            fd.write(chunk)
    print("Wrote zip file to: " + zipFilePath)
    with closing(ZipFile(zipFilePath)) as archive:
        filesInZipCount = len(archive.infolist())
    if filesInZipCount < 2:
        raise Exception(f"Wrong number of files in zip. Actually: {str(filesInZipCount)}")
    else:
        print("2 or more files in result zip. Actually: " + str(filesInZipCount))

    # Destroy
    testutils.printSection("DESTROY")
    r = requests.get(basicUrl + "/destroy/" + sessionID)
    print ("Result response code '%d" % (r.status_code))

    if not r.status_code == 200:
        raise Exception(f"Could not destroy: {r.text}")


def testScenarioController(basicUrl):
    baseResourcePath = "scenario_controller_resources"
    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    #Test generate algorithm from scenario
    testutils.printSection("GENERATE ALGORITHM FROM SCENARIO")
    gas_resourcesPath = os.path.join(baseResourcePath, "generate_from_scenario")
    with open(os.path.join(gas_resourcesPath, "scenario.conf")) as f:
        payloadString = f.read()

    response = requests.post(f"{basicUrl}/generateAlgorithmFromScenario", data=payloadString, headers={'Content-Type': 'text/plain'})
    testutils.ensureResponseOk(response)

    actualResult = os.path.join(tempDirectory, "actualResultFromScenario.txt")
    expectedResult = os.path.join(gas_resourcesPath, "expectedResult.txt")

    with open(actualResult, "w") as f:
        f.write(response.text)

    if(not testutils.compare("Generate from scenario", expectedResult, actualResult)):
        raise Exception("Expected algorithm does not match the actual algorithm.")

    #Test generate algorithm from multi model
    testutils.printSection("GENERATE ALGORITHM FROM MULTI MODEL")
    gamm_resourcesPath = os.path.join(baseResourcePath, "generate_from_multi_model")

    # Set FMU path to be a relative path
    config = json.load(open(os.path.join(gamm_resourcesPath, "multimodel.json")))
    expectedJson = json.load(open(os.path.join(gamm_resourcesPath, "expectedResult.json")))
    relativeFMUPathUri = pathlib.Path(os.path.abspath(os.path.join(gamm_resourcesPath, "rollback-test.fmu"))).as_uri()
    relativeControllerPathUri = pathlib.Path(os.path.abspath(os.path.join(gamm_resourcesPath, "rollback-end.fmu"))).as_uri()
    expectedJson["multiModel"]["fmus"]["{FMU}"]=relativeFMUPathUri
    expectedJson["multiModel"]["fmus"]["{Controller}"]=relativeControllerPathUri
    config["fmus"]["{FMU}"]=relativeFMUPathUri
    config["fmus"]["{Controller}"]=relativeControllerPathUri

    response = requests.post(f"{basicUrl}/generateAlgorithmFromMultiModel", json=config)
    testutils.ensureResponseOk(response)
    actualJson = response.json()

    if(not actualJson == expectedJson):
        print("ERROR: actual and expected json do not match")
        print("Actual json:")
        print(json.dumps(actualJson, indent=2))
        print("Expected json:")
        print(json.dumps(expectedJson, indent=2))
        raise Exception("Actual json returned does not match the expected json")
    else:
        print("Actual json returned matches the expected json")

    #Test execute algorithm
    testutils.printSection("EXECUTE ALGORITHM")
    ea_resourcesPath = os.path.join(baseResourcePath, "execute_algorithm")

    executableModel = json.load(open(os.path.join(ea_resourcesPath,"executableModel.json")))
    executableModel["multiModel"]["fmus"]["{FMU}"]=relativeFMUPathUri
    executableModel["multiModel"]["fmus"]["{Controller}"]=relativeControllerPathUri

    response = requests.post(f"{basicUrl}/executeAlgorithm", json=executableModel)
    testutils.ensureResponseOk(response)

    zipFilePath = os.path.join(tempDirectory, "actual_zip_result.zip")
    chunk_size = 128
    with open(zipFilePath, 'wb') as fd:
        for chunk in response.iter_content(chunk_size=chunk_size):
            fd.write(chunk)
    print("Wrote zip file to: " + zipFilePath)

    actualCSVFilePath = os.path.join(tempDirectory,"actual_result.csv")
    expectedCSVFilePath = os.path.join(ea_resourcesPath, "expectedoutputs.csv")

    with ZipFile(zipFilePath, 'r') as z:
        with open(actualCSVFilePath, 'wb') as f:
            f.write(z.read('outputs.csv'))

    if not testutils.compareCSV(expectedCSVFilePath, actualCSVFilePath):
        raise Exception("CSV files did not match!")


parser = argparse.ArgumentParser(prog='Example of Maestro Master Web Interface', usage='%(prog)s [options]')
parser.add_argument('--path', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")
parser.add_argument('--port', help='Maestro connection port')
parser.set_defaults(port=8082)

args = parser.parse_args()

# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

relativePath = os.path.abspath(os.path.join(r"../maestro-webapi/target/", "maestro-webapi*.jar"))

jarPath = os.path.abspath(args.path) if str(args.path) != "None" else testutils.findJar(relativePath)

port = args.port

# Check if port is free
if testutils.is_port_in_use(port):
    print("Port %s is already in use. Finding free port" % port)
    port = testutils.find_free_port()
    print("New port is: %s" % port)


if not os.path.isfile(jarPath):
    raise Exception(f"The path does not exist: {jarPath}")

print(f"Testing Web api of: {jarPath} with port: {str(port)}")

cmd = f"java -jar {jarPath} -p {str(port)}"
proc = subprocess.Popen(cmd, shell=True)
basicUrl = f"http://localhost:{str(port)}"

try:
    # Test connection
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

    # Run tests
    print("Testing simulation controller..")
    testSimulationController(basicUrl)
    print("Testing scenario controller..")
    testScenarioController(basicUrl)
finally:
   cleanUp(proc) 