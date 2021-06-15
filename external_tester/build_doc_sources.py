import shutil
import tempfile 
import os
import json
import glob
import subprocess


zip1name = 'getting-started-part1'
zip2name = 'getting-started-part2'
wtDirectory = 'wt'
wtControllerPath = os.path.join(wtDirectory,'watertankcontroller-c.fmu')
wtTankPath = os.path.join(wtDirectory,'singlewatertank-20sim.fmu')
wtExampleSpecRelativePath = os.path.join(wtDirectory,'example1.mabl')
wtExampleConfigRelativePath = os.path.join(wtDirectory,'mm.json')
pyPlotterPath = 'pythoncsvplotter.py'
toDirectoryAbsPath = os.path.abspath(os.path.join(os.path.dirname(os.getcwd()), 'docs', 'user', 'images', 'wt_example'))
newExampleSpecName = 'wt-example.mabl'
newExampleConfigName = 'wt-example-config.json'
startMsgPath = os.path.join(wtDirectory,'start_message.json')


def createAndPrepareTempDirectory():
    tempDirectory = tempfile.TemporaryDirectory()
    shutil.copy(wtControllerPath, tempDirectory.name)
    shutil.copy(wtTankPath, tempDirectory.name)
    return tempDirectory


def buildDocsConfig():
    startMsg = json.load(open(startMsgPath))
    config = json.load(open(wtExampleConfigRelativePath))
    config["fmus"]["{crtl}"]='watertankcontroller-c.fmu'
    config["fmus"]["{wt}"]='singlewatertank-20sim.fmu'
    config.pop('livestream')
    config['endTime'] = startMsg['endTime']
    return config


def findJar():
    basePath = r"../maestro/target/"
    basePath = os.path.abspath(os.path.join(basePath, "maestro-*-jar-with-dependencies.jar"))

    # try and find the jar file
    result = glob.glob(basePath)
    if len(result) == 0 or len(result) > 1:
        raise FileNotFoundError("Could not automatically find jar file please specify manually")

    return result[0]


def specGen(tempDirPath):
    # generate expansions
    config = os.path.join(tempDirPath, newExampleConfigName)
    cmd = f'java -jar {findJar()} --spec-generate1 {config} --dump-intermediate "{tempDirPath}"'
    p = subprocess.run(cmd, shell=True, cwd=tempDirPath)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")

    # generate spec and spec.runtime
    cmd = f'java -jar {findJar()} --spec-generate1 {config} --dump-simple "./"'
    p = subprocess.run(cmd, shell=True, cwd=tempDirPath)
    if p.returncode != 0:
        raise Exception(f"Error executing {cmd}")


def buildAndCopyPart1():
    zip1tmpdir = createAndPrepareTempDirectory()
    zip1tmpdirPath = zip1tmpdir.name
    with open(wtExampleSpecRelativePath, 'r') as mabl_file:
        data = mabl_file.read()

    # flatten path to fmus
    data = data.replace('wt/watertankcontroller-c.fmu', 'watertankcontroller-c.fmu')
    data = data.replace('wt/singlewatertank-20sim.fmu','singlewatertank-20sim.fmu')


    with open(os.path.join(zip1tmpdirPath, newExampleSpecName), "w") as mabl_file:
        print(data, file=mabl_file)
    shutil.copy(pyPlotterPath, os.path.join(zip1tmpdirPath, 'part1pythoncsvplotter.py'))
    shutil.copy(os.path.join(zip1tmpdirPath, newExampleSpecName), toDirectoryAbsPath)

    # generate getting-started-part 1 zipfile
    shutil.make_archive(os.path.join(toDirectoryAbsPath, zip1name), 'zip', zip1tmpdirPath)
    
    zip1tmpdir.cleanup()


def buildAndCopyPart2():
    zip2tmpdir = createAndPrepareTempDirectory()
    zip2tmpdirPath = zip2tmpdir.name
    config = buildDocsConfig()

    with open(os.path.join(toDirectoryAbsPath, newExampleConfigName), 'w') as json_file:
        json.dump(config, json_file, indent=2)
    with open(os.path.join(zip2tmpdirPath, newExampleConfigName), 'w') as json_file:
        json.dump(config, json_file, indent=2)
    shutil.copy(pyPlotterPath, os.path.join(zip2tmpdirPath, 'part2pythoncsvplotter.py'))

    # generate getting-started-part 2 zipfile
    shutil.make_archive(os.path.join(toDirectoryAbsPath, zip2name), 'zip', zip2tmpdirPath)

    # generate expanded specs and copy
    specGen(zip2tmpdirPath)
    shutil.copy(os.path.join(zip2tmpdirPath, 'spec00000.mabl'), toDirectoryAbsPath)
    shutil.copy(os.path.join(zip2tmpdirPath, 'spec00002.mabl'), toDirectoryAbsPath)
    shutil.copy(os.path.join(zip2tmpdirPath, 'spec.runtime.json'), toDirectoryAbsPath)

    zip2tmpdir.cleanup()


buildAndCopyPart1()
buildAndCopyPart2()

print("Successfully build doc sources!")