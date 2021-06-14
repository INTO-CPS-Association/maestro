import argparse
import subprocess
import sys
from subprocess import PIPE

def runTest(testName, testScriptFile, pathArgs):
    print(testName)
    res = subprocess.run(["python", testScriptFile, "--path", pathArgs])
    if(res.returncode != 0):
        print(f"### TEST HAD ERROR(S) ###")
        sys.exit(1)

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('--cliPath', type=str, default=None, help="Path to the Maestro CLI jar (Can be relative path)")
parser.add_argument('--webPath', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")

runTest("Running Legacy CLI Test", "cli_legacy_test.py", f"{parser.parse_args().webPath}")
print('\n')
runTest("Running Web API Test", "webapi_test.py", f"{parser.parse_args().webPath}")
print('\n')
runTest("Running CLI Test", "maestro_cli_test.py", f"{parser.parse_args().cliPath}")