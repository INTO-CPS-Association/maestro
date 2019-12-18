import argparse
import csv
import http.client
import json
import os
import socket
import subprocess
import sys
import threading
import time
import websocket
from contextlib import closing

sys.path.append(os.getcwd() + '/..')

from resultcheck import check_results
import tempfile
import shutil
from pathlib import Path

from jarunpacker import jar_unpacker


def find_free_port():
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        s.bind(('', 0))
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        return s.getsockname()[1]


parser = argparse.ArgumentParser(prog='PROG', usage='%(prog)s [options]')
parser.add_argument('--jar', help='jar', required=True)
parser.add_argument('--live', help='live output from API', action='store_true')
parser.set_defaults(live=False)

args = parser.parse_args()

port = find_free_port()
liveOutput = args.live

print("Starting api on port %d" % port)

with tempfile.TemporaryDirectory() as directory:
    directory = Path("tmp")
    directory = directory.resolve()
    if not directory.exists():
        directory.mkdir(parents=True)

    config = json.load(open("initialize.json"))
    for fmu in config["fmus"]:
        print (fmu)
        name = Path(config["fmus"][fmu])
        src = '../fmus' / name
        dest = directory / name
        shutil.copy(src, dest)

    classpathDir = jar_unpacker(args.jar)

    if classpathDir is None:
        print ("Failed to extract jar")
        exit(1)

    classPathDirExtracted = Path(classpathDir) / Path("BOOT-INF") / Path("lib")
    print("classPathDirExtracted: " + str(classPathDirExtracted))

    coeJarPath = classPathDirExtracted / Path("coe-1.0.7-SNAPSHOT.jar")

    pathExistsCounter = 0;
    print("coeJarPath: " + str(coeJarPath))
    while os.path.exists(coeJarPath) == False and pathExistsCounter < 10:
        print(str(coeJarPath) + " not found. Waiting 1 second.")
        time.sleep(1)

    if not os.path.exists(coeJarPath):
        print("Failed to find " + str(coeJarPath))
        exit(1)

    stream = None
    if liveOutput:
        stream = subprocess.PIPE
    else:
        stream = open('api.log', 'w')

    classPathInitial = ".:"

    if os.name == 'nt':
        classPathInitial = ".;"

    classpathJar = classPathInitial + str(classPathDirExtracted / Path("*"))
    print("classPathJar: " + classpathJar)

    api_process = subprocess.Popen(
        ['java', '-cp', str(classpathJar), "org.intocps.orchestration.coe.CoeMain", '-p',
         str(port)],
        stdout=stream, stderr=stream, cwd=str(directory))


    def post(c, location, data_path):
        headers = {'Content-type': 'application/json'}
        foo = json.load(open(data_path))
        json_data = json.dumps(foo)
        c.request('POST', location, json_data, headers)
        res = c.getresponse()
        return res


    def on_message(ws, message):
        print("WS MESSAGE RECEIVED:" + message)
        jdata = json.loads(message)
        a.append([jdata["time"], 0.1, jdata["data"]["{crtl}"]["crtlInstance"]["valve"],
                  jdata["data"]["{wt}"]["wtInstance"]["level"]])


    def post(c, location, data_path):
        headers = {'Content-type': 'application/json'}
        foo = json.load(open(data_path))
        json_data = json.dumps(foo)
        c.request('POST', location, json_data, headers)
        res = c.getresponse()
        return res


    ok = True
    for i in range(0, 25):
        try:
            print("Trying to connect: %d" % i)
            conn = http.client.HTTPConnection('localhost:' + str(port))

            headers = {'Content-type': 'application/json'}
            # jarDest = directory / Path(jarName)
            # print(jarDest)
            # shutil.copy(args.jar, jarDest)
            # api_process = subprocess.Popen(['java', "-Dserver.port=" + str(port), '-jar', jarName, '-p', str(port)],
            #                                stdout=subprocess.PIPE, cwd=directory)

            a = [["time", "step-size", "{crtl}.crtlInstance.valve", "{wt}.wtInstance.level"]]

            conn.request('GET', '/version')
            #

            response = conn.getresponse()

            if not response.status == 200:
                print("Could not ping no connection")
                ok = False
                break

            print("Connected with ping response '%s'," % response.read().decode())

            print("Create session")
            conn.request('GET', '/createSession')
            response = conn.getresponse()
            if not response.status == 200:
                print("Could not create session")
                ok = False
                break

            status = json.loads(response.read().decode())
            print ("Session '%s'" % status["sessionId"])

            response = post(conn, '/initialize/' + status["sessionId"], "initialize.json")
            if not response.status == 200:
                print("Could not initialize")
                ok = False
                break

            print ("Initialize response code '%d, data='%s'" % (response.status, response.read().decode()))

            wsConn = "ws://localhost:" + str(port) + "/attachSession/" + status["sessionId"]
            websocket.enableTrace(True)
            ws = websocket.WebSocketApp(wsConn, on_message=on_message)
            wst = threading.Thread(target=ws.run_forever)
            wst.daemon = True
            wst.start()

            response = post(conn, '/simulate/' + status["sessionId"], "simulate.json")
            if not response.status == 200:
                print("Could not simulate")
                ok = False
                break

            print ("Simulate response code '%d, data='%s'" % (response.status, response.read().decode()))

            with open("livestream.csv", "w", newline="") as f:
                writer = csv.writer(f)
                writer.writerows(a)

            conn.request('GET', '/result/' + status["sessionId"] + "/plain")
            response = conn.getresponse()
            if not response.status == 200:
                print("Could not create session")
                ok = False
                break

            result_csv_path = "result.csv"
            csv = response.read().decode()
            print ("Result response code '%d, data='%s'" % (response.status, csv))
            f = open(result_csv_path, "w")
            f.write(csv)
            f.close()

            step_size = float(config['algorithm']['size'])
            outputs = [c for c in config['connections']]

            simulate = json.load(open("simulate.json"))
            start_time = float(simulate['startTime'])
            end_time = float(simulate['endTime'])
            check_results(outputs, result_csv_path, start_time, end_time, step_size)
            check_results(outputs, "livestream.csv", start_time, end_time, step_size)

            break
        except ConnectionRefusedError:
            time.sleep(1)

    try:
        out1, err1 = api_process.communicate(timeout=0.1)
    except subprocess.TimeoutExpired:
        api_process.terminate()
        api_process.wait()

    if not ok:
        exit(1)
