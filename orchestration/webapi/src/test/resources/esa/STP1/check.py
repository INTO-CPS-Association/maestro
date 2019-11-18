import argparse
import json
import os
import sys
from io import open

sys.path.append(os.getcwd() + '/..')

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
