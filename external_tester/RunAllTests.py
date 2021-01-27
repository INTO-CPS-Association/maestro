import argparse
import subprocess

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('cliJar', type=str, help="Filename of CLI .jar")
parser.add_argument('webJar', type=str, help="Filename of Web API jar")
parser.add_argument('--cliPath', type=str, default=r"../maestro/target", help='Relative path to the folder containing Maestro CLI jar')
parser.add_argument('--webPath', type=str, default=r"../maestro-webapi/target", help='Relative path to the folder containing Maestro Web API jar')

args = parser.parse_args()

print("Running CLI Test")
subprocess.call(["python", "maestro_cli_test.py", args.cliJar, f"--path {args.cliPath}"])

print("Running Legacy CLI Test")
subprocess.call(["python", "cli_legacy_test.py", args.webJar, f"--path {args.webPath}"])

print("Running Web API Test")
subprocess.call(["python", "webapi_test.py", args.webJar, f"--path {args.webPath}"])
