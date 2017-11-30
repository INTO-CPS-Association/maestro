#! /usr/bin/env python
import urllib2
import os
import argparse
import json
import time


def jsonCall(url, data):
    req = urllib2.Request(url)
    req.add_header('Content-Type', 'application/json')
    return urllib2.urlopen(req, data)


def file_len(fname):
    i = 0
    with open(fname) as f:
        for i, l in enumerate(f):
            pass
    return i + 1


parser = argparse.ArgumentParser(description='Python COE Client')

parser.add_argument('--config', metavar='PATH', required=True,
                    help='Path to a valid configuration file')

parser.add_argument('--result', metavar='RESULT_PATH', required=True,
                    help='The path of the COE generated result file (CSV)')

parser.add_argument('--starttime', metavar='time', required=False,
                    help='The start time')

parser.add_argument('--endtime', metavar='time', required=True,
                    help='The end time')

parser.add_argument('--url', metavar='url', required=False,
                    help='The COE endpoint url')

parser.add_argument('--repeat', metavar='count', required=False,
                    help='Number of times to repeat the simulation')

parser.add_argument('--verbose', nargs='?', help='Verbose')

args = parser.parse_args()

# Check required info
verbose = False

if args.verbose is not None:
    verbose = True

url = "http://localhost:8082"

if args.url is not None:
    url = args.url
    if url[-1] == "/":
        url = url[:len(url) - 1]

startTime = 0
if args.starttime is not None:
    startTime = float(args.starttime)

endTime = float(args.endtime)

if startTime >= endTime:
    print "Invalid end time. <start time> " + str(
        +startTime) + " must be less than <end time> " + str(endTime)
    exit(-1)

if not os.path.exists(args.config):
    print "Config path invalid: " + args.config
    exit(-1)

repeats = 1
print "Configuration:"
print "\tStart Time   : " + str(startTime)
print "\tEnd Time     : " + str(endTime)
print "\tConfig Path: : " + args.config
print "\tResult Path: : " + args.result
print "\tCOE end point: " + url
if args.repeat is not None:
    repeats = int(args.repeat)
    print "\tRepeats      : " + str(repeats)

exec_start_time = time.time()

for simIndex in range(0, repeats):
    if args.repeat is not None:
        print "\nSimulating: " + str(simIndex + 1) + " / " + str(repeats)
    #
    # Simulation
    #
    sesstionId = ""

    # Create session
    try:
        response = urllib2.urlopen(url + "/createSession")
        status = json.load(response)
        if verbose:
            print status
        sessionId = status['sessionId']
        print "\tCreated session: " + str(sessionId)
    except urllib2.URLError as e:
        print str(e)
        exit(-1)

    # Initialize
    try:
        config = ""
        with open(args.config, 'r') as f:
            config = f.read()

        response = jsonCall(url + "/initialize/" + str(sessionId), config)
        status = json.load(response)
        if verbose:
            print status

        print "\tInitialized session status: " + status[0]['status']
    except urllib2.URLError as e:
        print str(e)
        print e.read()
        exit(-1)

    # Simulate
    try:
        config = {'startTime': startTime, 'endTime': endTime}

        response = jsonCall(url + "/simulate/" + str(sessionId),
                            json.dumps(config))
        status = json.load(response)

        if verbose:
            print status

        print "\tSimulation " + str(status['status']) + " in " + str(
            status['lastExecTime']) + " seconds"
    except urllib2.URLError as e:
        print str(e)
        print e.read()
        exit(-1)

    # Get result
    try:
        response = urllib2.urlopen(url + "/result/" + str(sessionId))
        CHUNK = 16 * 1024

        resultPath = args.result

        if args.repeat is not None:
            resultPath = resultPath[:resultPath.rfind('.')] + "-%06d.csv" % (simIndex,)

        with open(resultPath, 'wb') as f:
            while True:
                chunk = response.read(CHUNK)
                if not chunk:
                    break
                f.write(chunk)
        print "\tRead result " + str(file_len(args.result)) + " lines"
    except urllib2.URLError as e:
        print str(e)
        print e.read()
        exit(-1)

    # Destroy
    try:
        response = urllib2.urlopen(url + "/destroy/" + str(sessionId))
    except urllib2.URLError as e:
        print str(e)
        print e.read()
        exit(-1)

    print "\tsimulation done"

m, s = divmod(time.time() - exec_start_time, 60)
h, m = divmod(m, 60)

print "\nTotal execution time: %02d:%02d:%02d" % (h, m, s)
