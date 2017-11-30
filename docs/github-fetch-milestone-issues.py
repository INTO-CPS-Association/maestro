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

    file.write("\n# ["+ t+"]("+url+")\n\nLinks require access to the private repository.\n\n")
    if template != None:
        with open(template) as f:
            for line in f:
                file.write(line)
    file.write("## Bugfixes\n\n")
    file.write("Please note that the interactive list is at <"+url+">\n\n")
    

def writeIssue(file,state,number, title,url):
    if file == None:
        return
    
    write(state + " - " + title)
    file.write("* [#" +str(number)+" "+ state + " - "+title+"]("+url+")\n")

r = requests.get("https://api.github.com/repos/twt-gmbh/INTO-CPS-COE/milestones?state=closed", headers=header)

file = open("index.md", "w")
file.write("---\n"+"layout: default\n"+"title: Overview\n"+"---\n\n"+"<link rel=\"stylesheet\" href=\"css/releases.css\"><script src=\"http://code.jquery.com/jquery-1.11.1.min.js\"></script><script src=\"javascripts/moment-with-langs.js\"></script><script src=\"javascripts/github-releases.js\"></script><script>updateFrontPage();</script>\n\n")

if not r.status_code == 200:
        raise Exception(r.status_code)

for milestone in r.json():

    version = milestone['title']
    if version.startswith('v'):
        version = version[1:]

    
    writeMilestone(file,"Maestro "+version,milestone['html_url'],milestone['due_on'],None)



        
#    writeMilestone(file,version,milestone['url'],milestone['due_on'],None)


    ri = requests.get("http://api.github.com/repos/twt-gmbh/INTO-CPS-COE/issues?state=all&milestone="+str(milestone['number']), headers=header)
    if not ri.status_code == 200:
        raise Exception(ri.status_code)
    for issue in ri.json():
        writeIssue(file,issue['state'],issue['number'], issue['title'],issue['html_url'])
  #      writeIssue(fileM,issue['state'],issue['number'], issue['title'],issue['html_url'])
   #     if fileMabb != None:
    #        writeIssue(fileMabb,issue['state'],issue['number'], issue['title'],issue['html_url'])
   # if fileM !=None:
    #    fileM.close()
    #if fileMabb !=None:
     #   fileMabb.close()

file.close()
