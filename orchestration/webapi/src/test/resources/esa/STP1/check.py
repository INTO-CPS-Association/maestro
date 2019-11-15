import argparse
import json
from io import open

from resultcheck import check_results

parser = argparse.ArgumentParser(prog='PROG', usage='%(prog)s [options]')
parser.add_argument('--config', help='configuration', required=True)
parser.add_argument('--csv', help='result csv file', required=True)
parser.add_argument('--args', help='cli args', required=True)

args = parser.parse_args()

config = json.load(open(args.config, encoding='utf8'))
cli = json.load(open(args.args, encoding='utf8'))

outputColumns = [c for c in config['connections']]
stepSize = float(config['algorithm']['size'])

expectedStart = float(cli['start_time'])
expectedEnd = float(cli['end_time'])

if check_results(outputColumns, args.csv, expectedStart, expectedEnd, stepSize) == 1:
    print("Error")
    exit(1)

exit(0)

#
# def convertToValue(string):
#     try:
#         return float(string)
#     except ValueError:
#         if string == "true" or string == "false":
#             if string == "true":
#                 return 1
#         return 0
#
#
# with open(args.csv, encoding='utf8') as theFile:
#     reader = csv.DictReader(theFile)
#     for line in reader:
#         headers = [h for h in line]
#         if not set(outputColumns).issubset(set(headers)):
#             print ("Missing columns")
#             missingHeaders = set(outputColumns)
#             missingHeaders.difference_update(set(headers))
#             print (missingHeaders)
#             exit(1)
#         # print(headers)
#         # print (line)
# with open(args.csv, encoding='utf8') as theFile:
#     reader = csv.DictReader(theFile)
#     data = [line for line in reader]
#
#     timeColumn = [float(line['time']) for line in data]
#
#     fig = plt.figure()
#     for column in outputColumns:
#         y = [convertToValue(line[column]) for line in data]
#         plt.plot(timeColumn, y, label=column)
#     plt.legend(loc='upper left')
#     # plt.show()
#     fig.savefig(Path(args.csv).parent.joinpath(Path("results.pdf")))
#
#     expectedStart = float(cli['start_time'])
#     expectedEnd = float(cli['end_time'])
#
#     firstTime = min(timeColumn)
#     if firstTime < expectedStart or firstTime > expectedEnd:
#         print("start time not valid: %f should be %f" % (firstTime, expectedStart))
#         exit(1)
#
#     endTime = max(timeColumn)
#
#     if expectedStart > endTime or endTime < (expectedEnd - stepSize) or endTime > expectedEnd:
#         print("end time not valid: %f should be %f" % (endTime, expectedEnd))
#         exit(1)
#
#     expectedSteps = ((expectedEnd - expectedStart) / stepSize) + 1
#     entryCount = len(timeColumn)
#     if not entryCount == expectedSteps:
#         print("step count does not match expected: %d should be %d" % (entryCount, expectedSteps))
#         exit(1)
