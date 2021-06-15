import argparse
import subprocess

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('--cliPath', type=str, default=None, help="Path to the Maestro CLI jar (Can be relative path)")
parser.add_argument('--webPath', type=str, default=None, help="Path to the Maestro Web API jar (Can be relative path)")

args = parser.parse_args()

print("Running CLI Test")
subprocess.call(["python", "maestro_cli_test.py", "--path",  f"{args.cliPath}"])

print("Running Legacy CLI Test")
subprocess.call(["python", "cli_legacy_test.py", "--path", f"{args.webPath}"])

print("Running Web API Test")
subprocess.call(["python", "webapi_test.py", "--path", f"{args.webPath}"])
