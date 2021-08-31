import argparse
import subprocess
import sys
from subprocess import PIPE

def runTest(description, testScriptFile, args):
    print(description)
    cmd = ["python", testScriptFile] + args
    res = subprocess.run(cmd)
    if(res.returncode != 0):
        print(f"### TEST HAD ERROR(S) ###")
        sys.exit(1)

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('--cliPath', type=str, default=None, help="Path to the Maestro CLI jar (Can be relative path)")
parser.add_argument('--webPath', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")
parser.add_argument('--includeSlowTests', action="store_true", help='Includes CLI export cpp test in maestro CLI tests')
args = parser.parse_args()

runTest("Running Legacy CLI Tests", "cli_legacy_test.py", ["--path", f"{args.webPath}"])
print('\n')
runTest("Running Web API Tests", "webapi_test.py", ["--path", f"{args.webPath}"])
print('\n')
CLITestCmd = ["--path", str(args.cliPath)]
if args.includeSlowTests:
    CLITestCmd += ['--includeSlowTests']
runTest("Running CLI Tests", "maestro_cli_test.py", CLITestCmd)