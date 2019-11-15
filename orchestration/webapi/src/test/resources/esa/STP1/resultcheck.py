import csv
import matplotlib.pyplot as plt
from io import open
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

        fig = plt.figure()
        for column in outputColumns:
            y = [convertToValue(line[column]) for line in data]
            plt.plot(timeColumn, y, label=column)
        plt.legend(loc='upper left')
        # plt.show()
        fig.savefig(Path(result_csv_path).parent.joinpath(Path("results.pdf")))

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
