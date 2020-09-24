import argparse
import csv
import http.client
import json
import sys

parser = argparse.ArgumentParser(prog='Example of Maestro Master Web Interface', usage='%(prog)s [options]')
parser.add_argument('--live', help='live output from API', action='store_true')
parser.add_argument('--port', help='Maestro connection port')
parser.set_defaults(live=False)
parser.set_defaults(port=8082)

args = parser.parse_args()

port = args.port
liveOutput = args.live

def post(c, location, data_path):
    headers = {'Content-type': 'application/json'}
    foo = json.load(open(data_path))
    json_data = json.dumps(foo)
    c.request('POST', location, json_data, headers)
    res = c.getresponse()
    return res

config = json.load(open("scenario.json"))

conn = http.client.HTTPConnection('localhost:' + str(port))

print("Create session")
conn.request('GET', '/createSession')
response = conn.getresponse()
if not response.status == 200:
    print("Could not create session")
    sys.exit()

status = json.loads(response.read().decode())
print ("Session '%s', data=%s'" % (status["sessionId"], status))

response = post(conn, '/initialize/' + status["sessionId"], "scenario.json")
if not response.status == 200:
    print("Could not initialize")
    sys.exit()

print ("Initialize response code '%d, data=%s'" % (response.status, response.read().decode()))

response = post(conn, '/simulate/' + status["sessionId"], "simulate.json")
if not response.status == 200:
    print("Could not simulate")
    sys.exit()

print ("Simulate response code '%d, data=%s'" % (response.status, response.read().decode()))


conn.request('GET', '/result/' + status["sessionId"] + "/plain")
response = conn.getresponse()
if not response.status == 200:
    print("Could not receive results")
    sys.exit()

result_csv_path = "result.csv"
csv = response.read().decode()
print ("Result response code '%d" % (response.status))
f = open(result_csv_path, "w")
f.write(csv)
f.close()

# Cleans up session data. 
# Try to comment to see what Maestro stores.
# This can be retrieved by using zip instead of plain in the result GET request above.
conn.request('GET', '/destroy/' + status['sessionId'])
response = conn.getresponse()
if not response.status == 200:
    print("Could not destroy session")
    sys.exit()

print ("Destroy response code '%d, data='%s'" % (response.status, response.read().decode()))