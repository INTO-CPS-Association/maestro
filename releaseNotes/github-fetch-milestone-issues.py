#!/usr/bin/env python3

import json
import requests
import os.path
import datetime

oauth_token=os.getenv("GITHUB_API_TOKEN")
header={'Accept': 'application/vnd.github.full+json','Authorization': 'token '+oauth_token}


def write_issues(response):
    "output a list of issues to csv"
    if not r.status_code == 200:
        raise Exception(r.status_code)
    for issue in r.json():
        print(issue);
        labels = issue['labels']
        for label in labels:
            if label['name'] == "Client Requested":
                csvout.writerow([issue['number'], issue['title'].encode('utf-8'), issue['body'].encode('utf-8'), issue['created_at'], issue['updated_at']])

def write(string):
    print(string)

def writeMilestone(file,title,url,dueon,template):
    if file == None:
        return
    
    t =title+" - Release Notes"
    
    if dueon:
        d = datetime.datetime.strptime(dueon, "%Y-%m-%dT%H:%M:%SZ")
        t += " - {:%d %B %Y}".format(d)

    write(t)
    write("Issues")

    file.write("\n# ["+ t+"]("+url+")\n\n")
    if template != None:
        with open(template) as f:
            for line in f:
                file.write(line)
    file.write("## Bugfixes\n\n")
    file.write("Please note that the interactive list is at <"+url+">\n")
    

def writeIssue(file,state,number, title,url):
    if file == None:
        return
    
    write(state + " - " + title)
    file.write("* [#" +str(number)+" "+ state + " - "+title+"]("+url+")\n")

r = requests.get("https://api.github.com/repos/INTO-CPS-Association/maestro/milestones?state=closed", headers=header)

#file = open("output.txt", "w")

if not r.status_code == 200:
        raise Exception(r.status_code)

for milestone in r.json():

    version = milestone['title']
    if version.startswith('v'):
        version = version[1:]

    mdname = "ReleaseNotes_"+version+".md"
    mdnameabb = "ReleaseNotes_"+version+"_abbrev.md"
    fileM = None
    fileMabb = None
    if os.path.isfile(mdname):
        fileM = None
        continue
    else:
        fileM = open(mdname, "w")
        writeMilestone(fileM,"Maestro "+version,milestone['html_url'],milestone['due_on'],"ReleaseNotes-template.md")


    if os.path.isfile(mdnameabb):
        fileMabb = None
        
    else:
        fileMabb = open(mdnameabb, "w")
        writeMilestone(fileMabb,"Maestro "+version,milestone['html_url'],milestone['due_on'],"ReleaseNotes-template-abbrev.md")
        
#    writeMilestone(file,version,milestone['url'],milestone['due_on'],None)


    ri = requests.get("http://api.github.com/repos/INTO-CPS-Association/maestro/issues?state=all&milestone="+str(milestone['number']), headers=header)
    if not ri.status_code == 200:
        raise Exception(ri.status_code)
    for issue in ri.json():
#        writeIssue(file,issue['state'],issue['number'], issue['title'],issue['html_url'])
        writeIssue(fileM,issue['state'],issue['number'], issue['title'],issue['html_url'])
        if fileMabb != None:
            writeIssue(fileMabb,issue['state'],issue['number'], issue['title'],issue['html_url'])
    if fileM !=None:
        fileM.close()
    if fileMabb !=None:
        fileMabb.close()

#file.close()
