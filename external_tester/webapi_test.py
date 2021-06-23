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
import glob
import socket

websocketopen = False
socketFile = None

def is_port_in_use(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(('localhost', port)) == 0


def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        return s.getsockname()[1]

def cleanUp(p):
    p.terminate()
    if socketFile:
        socketFile.close()

def ws_open(ws):
    print("websocket opened")
    global websocketopen
    websocketopen = True

def ws_close(ws):
    print("websocket closed")

def on_message(ws, message):
    print("MESSAGE: %s" % message)
    def run(*args):
        print("MESSAGE IN THREAD: %s" % message)
        socketFile.write(message)

    threading.Thread(target=run).start()

def findJar():
    basePath = r"../maestro-webapi/target/"
    basePath = os.path.abspath(os.path.join(basePath, "maestro-webapi*.jar"))

    # try and find the jar file
    result = glob.glob(basePath)
    if len(result) == 0 or len(result) > 1:
        raise FileNotFoundError("Could not automatically find jar file please specify manually")
    return result[0]


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
    wsResult = tempDirectory + "/" + "wsActualResult.txt"
    socketFile = open(wsResult, "w")
    print("Writing websocket output to: " + wsResult)

    wsOnMessage = lambda ws, msg: socketFile.write(msg)
    webSocket = websocket.WebSocketApp(wsurl, on_open= ws_open, on_message= wsOnMessage, on_close= ws_close)
    wsThread=threading.Thread(target=webSocket.run_forever)
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
    webSocket.close()
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
    zipFilePath = tempDirectory + "/" + result_zip_path
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

def testScenarioVerifierController(basicUrl):
    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

parser = argparse.ArgumentParser(prog='Example of Maestro Master Web Interface', usage='%(prog)s [options]')
parser.add_argument('--path', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")
parser.add_argument('--port', help='Maestro connection port')
parser.set_defaults(port=8082)

args = parser.parse_args()

# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

jarPath = os.path.abspath(args.path) if str(args.path) != "None" else findJar()

port = args.port

# Check if port is free
if is_port_in_use(port):
    print("Port %s is already in use. Finding free port" % port)
    port = find_free_port()
    print("New port is: %s" % port)


if not os.path.isfile(jarPath):
    raise Exception(f"The path does not exist: {jarPath}")

print(f"Testing Web api of: {jarPath} with port: {str(port)}")

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

    print("Testing simulation controller..")
    testSimulationController(basicUrl)
    print("Testing scenario verifier controller..")
    testScenarioVerifierController(basicUrl)
finally:
    cleanUp(proc) 