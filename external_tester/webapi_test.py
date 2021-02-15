import argparse
import json
import sys
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

websocketopen = False

def printSection(section):
    hashes = "###############################"
    print("\n" + hashes)
    print(section)
    print(hashes)

def terminate(p):
    p.kill()
    sys.exit()

def terminateSocket(p):
    if socketFile:
        socketFile.close()

def ws_open(ws):
    print("WS_THREAD: open")
    global websocketopen
    websocketopen = True

def ws_close(ws):
    print("WS_THREAD: closed")

def ws_thread(*args):
    print("ws_thread: %s" % args[0])
    ws = websocket.WebSocketApp(args[0], on_open = ws_open, on_message = args[1], on_close=ws_close)
    ws.run_forever()

def findJar():
    basePath = r"../maestro-webapi/target/"
    basePath = os.path.abspath(os.path.join(basePath, "maestro-webapi*.jar"))

    # try and find the jar file
    result = glob.glob(basePath)
    if len(result) == 0 or len(result) > 1:
        raise FileNotFoundError("Could not automatically find jar file please specify manually")

    return result[0]


parser = argparse.ArgumentParser(prog='Example of Maestro Master Web Interface', usage='%(prog)s [options]')
parser.add_argument('--path', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")
parser.add_argument('--port', help='Maestro connection port')
parser.add_argument("--runAsMultiThread", action="store_true")
parser.set_defaults(port=8082)

args = parser.parse_args()

# cd to run everything relative to this file
os.chdir(os.path.dirname(os.path.realpath(__file__)))

path = os.path.abspath(args.path) if str(args.path) != "None" else findJar()

port = args.port


if not os.path.isfile(path):
    print('The path does not exist')
    sys.exit()

print("Testing Web api of: " + path + "with port: " + str(port))

if not args.runAsMultiThread:
    cmd = "java -jar " + path
    p = subprocess.Popen(cmd, shell=True)

try:
    tempDirectory = tempfile.mkdtemp()
    print("Temporary directory: " + tempDirectory)

    basicUrl = "http://localhost:"+str(port)

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

# Update paths to FMUs
    config = testutils.retrieveConfiguration()
    print("CONFIG: %s" % json.dumps(config))


    printSection("CREATE SESSION")
    r = requests.get(basicUrl + "/createSession")
    if not r.status_code == 200:
        raise Exception("Could not create session")

    status = json.loads(r.text)
    print ("Session '%s', data=%s'" % (status["sessionId"], status))

    # Initialize
    printSection("INITIALIZE")

    r = requests.post(basicUrl + "/initialize/" + status["sessionId"], json=config)
    if not r.status_code == 200:
        raise Exception("Could not initialize")


    print ("Initialize response code '%d, data=%s'" % (r.status_code, r.text))
    sessionID = status["sessionId"]

    # Weboscket support
    printSection("WEBSOCKET")
    wsurl = "ws://localhost:8082/attachSession/" + sessionID
    wsResult = tempDirectory + "/" + "wsActualResult.txt"
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

    #Simulate
    printSection("SIMULATE")
    r = requests.post(basicUrl + "/simulate/" + sessionID, json=json.load(open("wt/start_message.json")))
    if not r.status_code == 200:
        raise Exception(f"Could not simulate: {r.text}")

    print ("Simulate response code '%d, data=%s'" % (r.status_code, r.text))
    wsThread.join()
    socketFile.close()

    printSection("WS COMPARE")
    testutils.compare("WS", "wt/wsexpected.txt", wsResult)

    #Get plain results
    printSection("PLAIN RESULT")
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
    if not testutils.compare("CSV", "wt/result.csv", csvFilePath):
        raise Exception("CSV files did not match!")

    #Get zip results
    printSection("ZIP RESULT")
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
    print("Wrote zip file file to: " + zipFilePath)
    with closing(ZipFile(zipFilePath)) as archive:
        filesInZipCount = len(archive.infolist())
    if filesInZipCount < 2:
        raise Exception(f"Wrong number of files in zip. Actually: {str(filesInZipCount)}")
    else:
        print("2 or more files in result zip. Actually: " + str(filesInZipCount))

    # Destroy
    printSection("DESTROY")
    r = requests.get(basicUrl + "/destroy/" + sessionID)
    print ("Result response code '%d" % (r.status_code))

    if not r.status_code == 200:
        raise Exception(f"Could not destroy: {r.text}")

finally:
    if not args.runAsMultiThread:
        terminateSocket(p)
        terminate(p)
