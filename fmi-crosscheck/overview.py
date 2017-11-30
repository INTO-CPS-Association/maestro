import os
import fnmatch
import argparse

parser = argparse.ArgumentParser(description='COE x-check execution creator')

#parser.add_argument('--platform', metavar='PLATFORM',
#                    help='The FMI platform: <darwin64> | <win32> | <win64> | <linux32> | <linux64>', required=True)

parser.add_argument('--version', metavar='VERSION',
                    help='The COE version to test', required=True)

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

print "Generating test script for platform: %s" % "all"

ow="<!DOCTYPE html><html><head>"+'''

<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>

<!-- Latest compiled and minified CSS -->
<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\" integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" crossorigin=\"anonymous\"/>

<!-- Optional theme -->
<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css\" integrity=\"sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp\" crossorigin=\"anonymous\"/>

<!-- Latest compiled and minified JavaScript -->
<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\" integrity=\"sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa\" crossorigin=\"anonymous\"></script>



'''+"</head><body>"

#ow+="<tr class=\"table-active\">.dddddddd..</tr>"

sims = ""
cleanup=""

for platform in ["darwin64","linux32","linux64", "win32","win64"]:

    ow+="<h2>"+platform+"</h2>"
    ow+="<table class=\"table table-hover\"><tbody>"

    genRoot = os.path.join(platform,"COE",args.version)
    print "Generation root: %s " % genRoot
    testset = [a for a in recursive_glob(genRoot,'*_ref.csv') if "COE" in a and platform in a]
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
        testFmuDir = os.path.join(repoRoot, "Test_FMUs","FMI_2.0","CoSimulation",platform,toolName,toolVersion,name)

        fmu = os.path.relpath(os.path.join(testFmuDir,name+".fmu"),dir)
        opt = os.path.relpath(os.path.join(testFmuDir,name+"_ref.opt"),dir)
        ref = os.path.relpath(os.path.join(testFmuDir,name+"_ref.csv"),dir)
        inf = os.path.join(testFmuDir,name+"_in.csv")
        dir = os.path.relpath(dir,cwd)
    
        status = "-"
        cs="danger"
        if os.path.exists(os.path.join(dir,"passed")):
            status ="+"
            cs="success"
        else:
            if os.path.exists(os.path.join(dir,"reject")):
                status = "."
                cs = "warning"
        icon=""


        if os.path.exists(inf):
            icon = "<span class=\"glyphicon glyphicon-sort\"  aria-hidden=\"true\"></span>"
        url = os.path.join(dir,"index.html")

        ow+="<tr class=\""+cs+"\"><td>"+toolName+"</td><td>"+toolVersion+"</td><td><a href=\""+url+"\">"+name+"</a></td><td>"+icon+"</td><td>"+status+"</td></tr>\n"

    ow+="\n\n</tbody></table>"

ow+="</body></html>"
with open("overview-status.html", "w") as text_file:
    text_file.write(ow)

