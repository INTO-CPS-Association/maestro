import argparse
import http.client
import json
import os
import socket
import subprocess
import sys
import time
from contextlib import closing

sys.path.append(os.getcwd() + '/..')

from resultcheck import check_results
import tempfile
import shutil
from pathlib import Path


def find_free_port():
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        s.bind(('', 0))
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        return s.getsockname()[1]


parser = argparse.ArgumentParser(prog='PROG', usage='%(prog)s [options]')
parser.add_argument('--jar', help='jar', required=True)

args = parser.parse_args()

port = find_free_port()

print("Starting api on port %d" % port)

with tempfile.TemporaryDirectory() as directory:
    config = json.load(open("initialize.json"))
    for fmu in config["fmus"]:
        print (fmu)
        name = Path(config["fmus"][fmu])
        src = '../fmus' / name
        dest = directory / name
        shutil.copy(src, dest)

    jarName = Path(args.jar).name
    jarDest = directory / Path(jarName)
    print(jarDest)
    shutil.copy(args.jar, jarDest)
    api_process = subprocess.Popen(['java', '-cp', jarName, "org.intocps.orchestration.coe.CoeMain" ,'-p', str(port)],
                                   stdout=subprocess.PIPE, cwd=directory)


    def post(c, location, data_path):
        headers = {'Content-type': 'application/json'}
        foo = json.load(open(data_path))
        json_data = json.dumps(foo)
        c.request('POST', location, json_data, headers)
        res = c.getresponse()
        return res


    ok = True
    for i in range(0, 5):
        try:
            print("Trying to connect: %d" % i)
            conn = http.client.HTTPConnection('localhost:' + str(port))

            headers = {'Content-type': 'application/json'}

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

            response = post(conn, '/simulate/' + status["sessionId"], "simulate.json")
            if not response.status == 200:
                print("Could not simulate")
                ok = False
                break

            print ("Simulate response code '%d, data='%s'" % (response.status, response.read().decode()))

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
