import argparse
import subprocess

parser = argparse.ArgumentParser(prog='Example of Maestro Legacy CLI', usage='%(prog)s [options]')
parser.add_argument('--cliPath', type=str, default=r"../maestro/target/ maestro-2.0.4-SNAPSHOT-jar-with-dependencies.jar", help="Path to the Maestro CLI jar (Can be relative path)")
parser.add_argument('--webPath', type=str, default=r"../maestro-webapi/target/maestro-webapi-2.0.4-SNAPSHOT.jar", help="Path to the Maestro Web API jar (Can be relative path)")

args = parser.parse_args()

print("Running CLI Test")
subprocess.call(["python", "maestro_cli_test.py", f"--path {args.cliPath}"])

print("Running Legacy CLI Test")
subprocess.call(["python", "cli_legacy_test.py", f"--path {args.webPath}"])

print("Running Web API Test")
subprocess.call(["python", "webapi_test.py", f"--path {args.webPath}"])
