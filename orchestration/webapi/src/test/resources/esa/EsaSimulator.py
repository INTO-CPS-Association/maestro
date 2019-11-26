import http.client
import time


class EsaSimulator:
    def __init__(self, url):
        self.url = url
        self.headers = {'Content-type': 'application/json'}

    def ping(self):
        try:
            conn = http.client.HTTPConnection(self.url)
            conn.request('GET', '/api/esav1/simulator/ping')
            response = conn.getresponse()
            return response.status == 200
        except ConnectionRefusedError:
            return False

    def initialize(self, data):
        conn = http.client.HTTPConnection(self.url)
        conn.request('POST', "/api/esav1/simulator/initialize", data, self.headers)
        res = conn.getresponse()
        return res

    def simulate(self, data):
        conn = http.client.HTTPConnection(self.url)
        conn.request('POST', "/api/esav1/simulator/simulate", data, self.headers)
        res = conn.getresponse()
        return res

    def stop(self):
        conn = http.client.HTTPConnection(self.url)
        conn.request('POST', "/api/esav1/simulator/stop", "", self.headers)
        res = conn.getresponse()
        return res

    def reset(self):
        conn = http.client.HTTPConnection(self.url)
        conn.request('POST', "/api/esav1/simulator/reset", "", self.headers)
        res = conn.getresponse()
        return res

    def get_plain_result(self):
        conn = http.client.HTTPConnection(self.url)
        conn.request('GET', "/api/esav1/simulator/result/plain")
        res = conn.getresponse()
        return res

    def store_plain_result(self, result_csv_path):
        response = self.get_plain_result()
        if response.status == 200:
            with open(result_csv_path, "w") as f:
                csv = response.read().decode()
                f.write(csv)
        else:
            print("Faild to get results code: %d, content: %s" % (response.status, response))

    def destroy(self):
        conn = http.client.HTTPConnection(self.url)
        conn.request('GET', "/api/esav1/simulator/destroy", "", self.headers)
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
