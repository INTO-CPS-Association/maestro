import os
import fnmatch
import argparse

parser = argparse.ArgumentParser(description='COE x-check execution creator')

parser.add_argument('--platform', metavar='PLATFORM',
                    help='The FMI platform: <darwin64> | <win32> | <win64> | <linux32> | <linux64>', required=True)

parser.add_argument('--version', metavar='VERSION',
                    help='The COE version to test', required=True)

parser.add_argument('--oin', 
                    help='Include only models with input files', required=False, action='store_true')

args = parser.parse_args()


def recursive_glob(treeroot, pattern):
    results = []
    for base, dirs, files in os.walk(treeroot):
        goodfiles = fnmatch.filter(files, pattern)
        results.extend(os.path.join(base, f) for f in goodfiles)
    return results

def isWin(p):
    return "win32" == p or "win64" == p

def get_parent(root,level):
    parent = os.path.abspath(os.path.dirname(dir));
    for x in range(0,level):
        #      print parent
        (parent,t) = os.path.split(parent)
    return parent

cwd = os.getcwd()

print "Generating test script for platform: %s" % args.platform

sims = ""
cleanup="cwd=`pwd`\n"

if isWin( args.platform):
    sims+="@echo off\n"
    sims+="REM Python library notes: \n"
    sims+="REM http://www.lfd.uci.edu/~gohlke/pythonlibs/#scipy\n"
    sims+="REM http://www.lfd.uci.edu/~gohlke/pythonlibs/#numpy\n"
    cp = '.;%COEJAR%'
    #sims+="set COEJAR=`readlink -f coe.jar`\n"
    sims+="CALL :NORMALIZEPATH \"coe.jar\"\n"
    sims+="SET COEJAR=%RETVAL%\n"
    sims+="set cwd=%~dp0\n"
else:
    cp = ".:$COEJAR"
    sims+="COEJAR=`readlink -f coe.jar`\n"
    sims+="cwd=`pwd`\n"

genRoot = os.path.join(args.platform,"COE",args.version)
print "Generation root: %s " % genRoot
testset = [a for a in recursive_glob(genRoot,'*_ref.csv') if "COE" in a and args.platform in a]
tntest= len(testset)
ntest = 0
for ref in  testset:
    ntest+=1
    refName = os.path.basename(ref)
    name = refName[:refName.index('_ref.csv')]
    dir= os.path.abspath(os.path.dirname(ref))
    #print dir

    toolVersion = os.path.basename(get_parent(dir,0))
    toolName = os.path.basename(get_parent(dir,1))
    repoRoot = get_parent(dir,8)

    print "Tool %s, Version %s, Model: %s, Path: %s" %(toolName,toolVersion, name,dir)
    
    prefix = dir[len(get_parent(dir,5)):]
    parent = get_parent(dir,8)
    #platform=  os.path.basename(get_parent(dir,4+3))
    testFmuDir = os.path.join(repoRoot, "Test_FMUs","FMI_2.0","CoSimulation",args.platform,toolName,toolVersion,name)

#    for x in range(0,4):
#        prefix = prefix[prefix.index(os.sep)+1:]
#        testFmuDir = os.path.join(testFmuDir,prefix)

    fmu = os.path.relpath(os.path.join(testFmuDir,name+".fmu"),dir)
    opt = os.path.relpath(os.path.join(testFmuDir,name+"_ref.opt"),dir)
    ref = os.path.relpath(os.path.join(testFmuDir,name+"_ref.csv"),dir)
    inArg =""
    if os.path.exists(os.path.join(testFmuDir,name+"_in.csv")):
        inArg=" -in " +os.path.relpath(os.path.join(testFmuDir,name+"_in.csv"),dir)
    dir = os.path.relpath(dir,cwd)
    
    sims+= "\necho \"################################################# "+str(ntest).zfill(2)+" / "+str(tntest).zfill(2)+" ##################################################\"\n"
    sims+="echo \"# "+toolName+" | "+ toolVersion+" | " + name+"\"\n"
    sims+="echo \"# "+dir+"\"\n"
    sims+= "echo \"#############################################################################################################\"\n"

    JVM32bit=""
    if "linux32" == args.platform:
        JVM32bit=" -d32 "

    dirVar = "$cwd"

    # *nix only
    cleanup+="\ncd "+dirVar+"/"+dir + "\n"
    cleanup+="rm -f coe.log config.json plot_cmd.sh x-plot.py\n"
    cleanup+="cd "+dirVar

    
    if isWin(args.platform):
        dirVar = "%cwd%"
        
    sims+="\ncd "+dirVar+"/"+dir + "\n"
    if len(inArg)==0 and args.oin:
        sims+=""
    else:
        sims+="java"+JVM32bit+" -Dfmi.instantiate.with.empty.authority=true -cp "+cp+" org.intocps.orchestration.coe.single.SingleSimMain -xplot -readme -level WARN  -fmu " +fmu+ " -opt "+ opt + " -ref "+ ref+inArg+"\n"  #--min-stepsize 0.001

    sims +="\n"
    sims +="cd "+dirVar +"\n"


if isWin( args.platform ):
    sims += ''':: ========== FUNCTIONS ==========
EXIT /B

:NORMALIZEPATH
  SET RETVAL=%~dpfn1
  EXIT /B
'''
    with open("sims-"+args.platform+".bat", "w") as text_file:
        text_file.write(sims)

    winPost='find '+os.path.join(args.platform,"COE",args.version)+' -name plot_cmd.sh -exec echo {} \; -execdir sh {} \;\n'
    winPost+="#cleanup\n\n" + cleanup
    with open("sims-"+args.platform+".post.sh", "w") as text_file:
        text_file.write(winPost)

if "linux" in args.platform or "darwin" in args.platform:
    sims+="\n\n#cleanup\n\n"+cleanup
    with open("sims-"+args.platform+".sh", "w") as text_file:
        text_file.write(sims)

