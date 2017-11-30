import os
import fnmatch
import argparse

parser = argparse.ArgumentParser(description='COE x-check execution creator')

parser.add_argument('--platform', metavar='PLATFORM',
                    help='The FMI platform: <darwin64> | <win32> | <win64> | <linux32> | <linux64>', required=True)

args = parser.parse_args()


def recursive_glob(treeroot, pattern):
    results = []
    for base, dirs, files in os.walk(treeroot):
        goodfiles = fnmatch.filter(files, pattern)
        results.extend(os.path.join(base, f) for f in goodfiles)
    return results

def get_parent(root,level):
    parent = os.path.abspath(os.path.dirname(dir));
    for x in range(0,level):
  #      print parent
        (parent,t) = os.path.split(parent)
    return parent

cwd = os.getcwd()

sims = ""

if "win" in args.platform:
        sims+="@echo off\n"
        cp = '.;\\\\psf\\Home\\data\\into-cps\\intocps-coe\\orchestration\\coe\\target\\coe-0.1.11-SNAPSHOT-jar-with-dependencies.jar'

if "linux" in args.platform:
    cp = ".:/media/psf/Home/data/into-cps/intocps-coe/orchestration/coe/target/coe-0.1.11-SNAPSHOT-jar-with-dependencies.jar"

if "darwin" in args.platform:
    cp = ".:/Users/kel/data/into-cps/intocps-coe/orchestration/coe/target/coe-0.1.11-SNAPSHOT-jar-with-dependencies.jar"
        
testset = [a for a in recursive_glob('.','*_ref.csv') if "COE" in a and args.platform in a]
tntest= len(testset)
ntest = 0
for ref in  testset:
    ntest+=1
    refName = os.path.basename(ref)
    name = refName[:refName.index('_ref.csv')]
    dir= os.path.abspath(os.path.dirname(ref))
    print dir
    prefix = dir[len(get_parent(dir,5)):]
    parent = get_parent(dir,8)
    platform=  os.path.basename(get_parent(dir,4))
    testFmuDir = os.path.join(parent, "Test_FMUs","FMI_2.0","CoSimulation",platform)

    for x in range(0,4):
        prefix = prefix[prefix.index(os.sep)+1:]
    testFmuDir = os.path.join(testFmuDir,prefix)

    fmu = os.path.join(testFmuDir,name+".fmu")
    opt = os.path.join(testFmuDir,name+"_ref.opt")
    ref = os.path.join(testFmuDir,name+"_ref.csv")
 
    sims+= "\necho \"################################################# "+str(ntest).zfill(2)+" / "+str(tntest).zfill(2)+" ##################################################\"\n"
    sims+="echo \"# "+name+"\"\n"
    sims+="echo \"# "+dir+"\"\n"
    sims+= "echo \"#############################################################################################################\"\n"

    JVM32bit=""
    if "linux32" in args.platform:
        JVM32bit=" -d32 "
    sims+="\ncd "+dir + "\njava"+JVM32bit+" -Dfmi.instantiate.with.empty.authority=true -cp "+cp+" org.intocps.orchestration.coe.single.SingleSimMain -xplot -readme -level WARN  -fmu " +fmu+ " -opt "+ opt + " -ref "+ ref+"\n"  #--min-stepsize 0.001
    sims +="\n"
    sims +="cd "+cwd


if "win" in args.platform:
    with open("sims-"+args.platform+".bat", "w") as text_file:
        text_file.write(sims)

if "linux" in args.platform or "darwin" in args.platform:
    with open("sims-"+args.platform+".sh", "w") as text_file:
        text_file.write(sims)

