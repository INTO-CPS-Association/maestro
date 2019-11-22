import csv
import json
import matplotlib.pyplot as plt
from io import open
from itertools import chain
from pathlib import Path


def check_results(outputs, result_csv_path, start_time, end_time, step_size):
    # config = json.load(open(args.config, encoding='utf8'))
    # cli = json.load(open(args.args, encoding='utf8'))

    outputColumns = outputs  # [c for c in config['connections']]
    stepSize = step_size  # float(config['algorithm']['size'])

    def convertToValue(string):
        try:
            return float(string)
        except ValueError:
            if string == "true" or string == "false":
                if string == "true":
                    return 1
            return 0

    with open(result_csv_path, encoding='utf8') as theFile:
        reader = csv.DictReader(theFile)
        for line in reader:
            headers = [h for h in line]
            if not set(outputColumns).issubset(set(headers)):
                print ("Missing columns")
                missingHeaders = set(outputColumns)
                missingHeaders.difference_update(set(headers))
                print (missingHeaders)
                return 1
            # print(headers)
            # print (line)
    with open(result_csv_path, encoding='utf8') as theFile:
        reader = csv.DictReader(theFile)
        data = [line for line in reader]

        timeColumn = [float(line['time']) for line in data]

        if len(timeColumn) == 0:
            print("no simulation results present")
            return 1

        fig = plt.figure()
        for column in outputColumns:
            y = [convertToValue(line[column]) for line in data]
            plt.plot(timeColumn, y, label=column)
        plt.legend(loc='upper left')
        # plt.show()
        fig.savefig(Path(result_csv_path).parent.joinpath(Path(result_csv_path).name[:-4] + "-results.pdf"))

        expectedStart = start_time  # float(cli['start_time'])
        expectedEnd = end_time  # float(cli['end_time'])

        firstTime = min(timeColumn)
        if firstTime < expectedStart or firstTime > expectedEnd:
            print("start time not valid: %f should be %f" % (firstTime, expectedStart))
            return 1

        endTime = max(timeColumn)

        if expectedStart > endTime or endTime < (expectedEnd - stepSize) or endTime > expectedEnd:
            print("end time not valid: %f should be %f" % (endTime, expectedEnd))
            return 1

        expectedSteps = ((expectedEnd - expectedStart) / stepSize) + 1
        entryCount = len(timeColumn)
        if not entryCount == expectedSteps:
            print("step count does not match expected: %d should be %d" % (entryCount, expectedSteps))
            return 1
    return 0


def check_result_from_simulator(init_file_path, result_path):
    config = json.load(open(init_file_path, encoding='utf8'))
    if "connections" in config:
        outputs = [str(k) for k in config["connections"]]
    else:
        outputs = []

    if "requested_outputs" in config:
        additionalOutputs = [[v + "." + k for k in config["requested_outputs"][v]] for v in config["requested_outputs"]]
        outputs = outputs + (list(chain.from_iterable(additionalOutputs)))
    startTime = 0
    endTime = config["end_time"]
    step_size = config["step_size"]
    return check_results(outputs, result_path, startTime, endTime, step_size)
