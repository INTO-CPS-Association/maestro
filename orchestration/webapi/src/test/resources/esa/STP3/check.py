import argparse
import json
import os
import socket
import subprocess
import sys
from contextlib import closing

sys.path.append(os.getcwd() + '/..')

import tempfile
import shutil
from pathlib import Path
from EsaSimulationManager import EsaSimulationManager
from EsaSimulator import EsaSimulator
import threading
from resultcheck import check_results


def stdoutprocess(o):
    while True:
        stdoutdata = o.stdout.readline()
        if stdoutdata:
            print("#### >>>> %s" % stdoutdata[:-1])
        else:
            break


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

print("Starting api on port %d" % port)


def copy_fmus_to_dir(init_file_path, target_directory):
    config = json.load(open(init_file_path))
    for fmu in config["fmus"]:
        print (fmu)
        name = Path(config["fmus"][fmu])
        src = '../fmus' / name
        dest = target_directory / name
        shutil.copy(src, dest)


def create_simulator(manager):
    print("Creating simulator")
    config = manager.create()

    if config is None:
        print("Count not obtain simulator")
        return None, None

    url = config['instance_url']
    print("Simulator url is: %s" % url)

    simulator = EsaSimulator(url)
    copy_fmus_to_dir("1-initialize.json", config['working_directory'])
    id = config['instance_id']
    return simulator, id


def check_result_from_simulator(init_file_path, result_path):
    config = json.load(open(init_file_path, encoding='utf8'))
    outputs = config["connections"]
    startTime = 0
    endTime = config["end_time"]
    step_size = config["step_size"]
    return check_results(outputs, result_path, startTime, endTime, step_size)


failed = False
liveOutput = args.live
connectionRetries = 30

with tempfile.TemporaryDirectory() as directory:
    jarName = Path(args.jar).name
    jarDest = directory / Path(jarName)
    jarDest.resolve()

    jar = Path(args.jar)
    jar = jar.resolve()

    stream = None
    if liveOutput:
        stream = subprocess.PIPE
    else:
        stream = open('api.log', 'w')

    api_process = subprocess.Popen(['java', "-Dserver.port=" + str(port), '-jar', jar],
                                   stdout=stream, stderr=stream, cwd=directory)

    if liveOutput:
        t = threading.Thread(target=stdoutprocess, args=(api_process,))
        t.daemon = True
        t.start()
    for i in range(0, 1):

        manager = EsaSimulationManager("localhost:" + str(port))
        if not manager.connect(connectionRetries):
            print("Connection timeout")
            failed = True
            break
        sim1, sim1Id = create_simulator(manager)

        if not sim1.connect(connectionRetries):
            print("Could not connect to simulator 1")
            failed = True
            break

        sim2, sim2Id = create_simulator(manager)

        if not sim2.connect(connectionRetries):
            print("Could not connect to simulator 2")
            failed = True
            break

        for ni in range(0, 1):
            print("------------------------------------------")
            print("Testing Simulation: Initializing")
            print("------------------------------------------")

            if sim1.initialize(json.dumps(json.load(open("1-initialize.json", encoding='utf8')))).status != 200:
                print("Initialize simulator 1 failed")
                failed = True
                break

            if sim2.initialize(json.dumps(json.load(open("2-initialize.json", encoding='utf8')))).status != 200:
                print("Initialize simulator 2 failed")
                failed = True
                break

            print("------------------------------------------")
            print("Testing Simulation: Simulating")
            print("------------------------------------------")

            if sim1.simulate(json.dumps(json.load(open("1-simulateFor.json", encoding='utf8')))).status != 200:
                print("Simulate simulator 1 failed")
                failed = True
                break
            if sim2.simulate(json.dumps(json.load(open("2-simulateFor.json", encoding='utf8')))).status != 200:
                print("Simulate simulator 2 failed")
                failed = True
                break

            print("------------------------------------------")
            print("Testing Simulation: Stopping")
            print("------------------------------------------")
            sim1.stop()
            sim2.stop()

            print("------------------------------------------")
            print("Testing Simulation: Obtain results")
            print("------------------------------------------")

            sim1.store_plain_result("1.csv")
            sim2.store_plain_result("2.csv")

            print("------------------------------------------")
            print("Testing Simulation: Deleting")
            print("------------------------------------------")
            sim1.destroy()
            sim2.destroy()

        manager.delete(sim1Id)
        manager.delete(sim2Id)

        if not check_result_from_simulator("1-initialize.json", "1.csv"):
            print("Output of simulator 1 wrong")
            failed = True

        if not check_result_from_simulator("2-initialize.json", "2.csv"):
            print("Output of simulator 2 wrong")
            failed = True

        break
    print("Terminating")
    api_process.terminate()
    api_process.wait()

if failed:
    print("FAIL!")
    exit(1)
