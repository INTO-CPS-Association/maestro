import http.client
import json
import time


class EsaSimulationManager:
    def __init__(self, url):
        self.url = url
        self.headers = {'Content-type': 'application/json'}

    def ping(self):
        try:
            conn = http.client.HTTPConnection(self.url)
            conn.request('GET', '/api/esav1/orchestrator/ping')
            res = conn.getresponse()
            return res.status == 200
        except ConnectionRefusedError:
            return False

    def create(self):
        conn = http.client.HTTPConnection(self.url)
        conn.request('POST', "/api/esav1/orchestrator/", "", self.headers)
        res = conn.getresponse()

        if res.status == 200:
            data = res.read().decode()
            return json.loads(data)
        return None

    def delete(self, simulator_id):
        conn = http.client.HTTPConnection(self.url)
        conn.request('DELETE', "/api/esav1/orchestrator/" + simulator_id, "", self.headers)
        res = conn.getresponse()
        return res

    def connect(self, seconds):
        for i in range(0, seconds):
            print("Trying to connect: %d" % i)

            if self.ping():
                return True
            else:
                time.sleep(1)
        return False
