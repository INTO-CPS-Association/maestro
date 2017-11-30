#!/usr/bin/env python

# packages to install on windows
# http://www.lfd.uci.edu/~gohlke/pythonlibs/#scipy
# http://www.lfd.uci.edu/~gohlke/pythonlibs/#numpy
#

import matplotlib.pyplot as plt
import csv
import argparse
import numpy as np
import scipy.interpolate as sciint
import os
import pandas as pd

rtol = 0.1

parser = argparse.ArgumentParser(description='Result plotter')

parser.add_argument('--name', metavar='NAME',
                    help='The name to be displayed in the report')

parser.add_argument('--result', metavar='RESULT_PATH', required=True,
                    help='The path of the COE generated result file (CSV)')
parser.add_argument('--ref', metavar='REF_PATH', required=False,
                    help='The path of the export tool generated result file (CSV)')

parser.add_argument('--config', metavar='CONFIG_PATH',
                    help='The path of the COE config file (JSON)')

args = parser.parse_args()


def createWebHeader(title, config):
    return """ <!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title> """ + title + """</title>


		<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">

		<!-- Optional theme -->
		<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">

<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>

		<!-- Latest compiled and minified JavaScript -->
		<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>


		<script>
			window.onload = function loadConfig(){
			div = document.getElementById('configDiv');
			s = """ + config + """
			sj = JSON.parse(s)
			console.info(sj)
			ss = JSON.stringify(sj, null, 4)
			div.innerHTML = syntaxHighlight(ss);

			}

function syntaxHighlight(json) {
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}


</script>

<style>
pre {outline: 1px solid #ccc; padding: 5px; margin: 5px; }
.string { color: green; }
.number { color: darkorange; }
.boolean { color: blue; }
.null { color: magenta; }
.key { color: red; }
	</style>


  </head> """;


def webConfigDiv(title, html):
    return '<h1>' + title + '</h1>' + """
		<div class="panel panel-primary">
			<div class="panel-heading">
				<h3 class="panel-title">Configuration</h3>
			</div>
			<div class="panel-body">
				<div><pre id="configDiv" /></div>""" + html + """
			</div>
		</div>""";


def webOutputDiv(name):
    return """<div class="panel panel-info">
			<div class="panel-heading">
				<h3 class="panel-title">""" + name + """</h3>
			</div>
			<div class="panel-body">
				<img src=" """ + name + """.svg"/></div></div>""";


PASSED = 1
REJECTED = -1
FAILED = -2
NOT_SET = 0


def webOutputDivHtml(html, status):
    statusType = "info"
    statusText = "unknown"

    if status == PASSED:
        statusType = "success"
        statusText = "passed"
    if status == REJECTED:
        statusType = "warning"
        statusText = "rejected"
    if status == FAILED:
        statusType = "danger"
        statusText = "failed"

    return "<div class=\"panel panel-info\"><div class=\"panel-heading\"><h3 class=\"panel-title\">" + name + "</h3></div><div class=\"panel-body\">" + html + "<div class=\"alert alert-" + statusType + "\">" + statusText + "</div></div></div>";


def readSvgHtml(fname):
    html = ""
    with open(fname) as f:
        next(f)
        for line in f:
            html += line + "\n"
    return html


def csvRead(path, stripInstances=0):
    df = pd.read_csv(path, encoding='utf8', engine='python')
    column = {}
    headers= list()
    for h in df:
        hstr = str(h)
        if stripInstances:
            if "." in hstr:
                hstr = hstr[hstr.rfind(".") + 1:]

        column[hstr] = df[h].values
        headers.append(hstr)

    return {'headers': headers, 'columns': column}


def createHtml(name, config, body, status):
    web = createWebHeader(name, config)
    html = "<p>Tolerance: " + str(rtol) + "</p>"

    statusType = "info"
    statusText = "unknown"

    if status == PASSED:
        statusType = "success"
        statusText = "passed"
    if status == REJECTED:
        statusType = "warning"
        statusText = "rejected"
    if status == FAILED:
        statusType = "danger"
        statusText = "failed"

    html += "<div class=\"alert alert-" + statusType + "\">" + statusText + "</div>"
    web += '<body>' + webConfigDiv(name, html) + body
    web += '</body></html>'
    return web


# print column


config = "{}"
name = "unknown"

if args.name:
    name = args.name;

if args.config:
    with open(args.config, 'r') as myfile:
        config = myfile.read().replace('\n', '')
        config = "\"" + config.replace('"', '\\"') + "\""

data = csvRead(args.result, 1)

if args.ref:
    dataRef = csvRead(args.ref)
else:
    dataRef = {'headers': [], 'columns': []}

headers = data['headers']
column = data['columns']

passed = True
passedCurrent = True

htmlBody = ""

for h in headers:
    if h == 'time' or h == 'step-size':
        continue
    fig = plt.figure(h)

    fsimTime = np.array(column["time"], dtype=float)
    xlim = fsimTime[-1]

    ax = fig.add_subplot(2, 1, 1)
    ax.set_ylabel(h)
    #    lgd2 = lgd

    if h in dataRef['headers']:
        cols = dataRef['columns']

        # time column
        frefTime = np.array(cols["time"], dtype=float)

        # adjust xlim
        xlim = min(frefTime[-1], xlim)

        # add reference signal to plot
        ax.plot(cols['time'], cols[h], 'r-', label=h + '_Ref')

        isBoolean = all(x == '0' or x == '1' for x in column[h]) and all(
            x == '0' or x == '1' for x in cols[h])
        print "Is Boolean: " + str(isBoolean)

        if isBoolean:
            type = bool
        else:
            type = float

        # generate diff sub plot
        # generate diff
        a = np.array(cols[h], dtype=type)
        v = np.array(column[h], dtype=type)

        if not isBoolean:
            ax.plot(cols["time"], [x + rtol for x in a], 'y-',
                    label=h + '_upper', color='#d3f7cd')
            ax.plot(cols["time"], [x - rtol for x in a], 'b-',
                    label=h + '_lower', color='#d3f7cd')
            ax.fill_between(frefTime, [x + rtol for x in a],
                            [x - rtol for x in a], color='#d3f7cd')

        # interpolate signals
        fref = sciint.interp1d(np.array(cols["time"], dtype=float), a)
        fsig = sciint.interp1d(np.array(column["time"], dtype=float), v)

        # find last commen index in result which has a time not exceding the _ref signal
        compIndexEnd = len(column["time"]) - 1

        for x in range(0, compIndexEnd):
            for j in range(len(cols["time"]), 0):
                if column["time"][x] <= cols["time"][j]:
                    compIndexEnd = x;
                    break;

        # check result against reference with relative tolorance
        if not isBoolean:
            g = [
                fref(fsimTime[x]) + rtol >= v[x] and fref(fsimTime[x]) - rtol <=
                v[x] for x in range(0, len(fsimTime[:compIndexEnd]))]
        else:
            g = [fref(column["time"][x]) == v[x] for x in
                 range(0, len(fsimTime[:compIndexEnd]))]

        print "End time: " + str(fsimTime[compIndexEnd])

        # the overall status for the result signal within the tolorance
        passedCurrent = all(x for x in g)

        print "Is " + h + " passed: " + str(passedCurrent)

        passed = passed and passedCurrent

        stepSize = xlim / min(len(frefTime), len(fsimTime))
        xnew = np.linspace(0, xlim, num=xlim / stepSize, endpoint=True)
        ax2 = fig.add_subplot(2, 1, 2)
        ax2.plot(xnew, fref(xnew) - fsig(xnew), 'g-', label=h + '_ref - ' + h)
        ax2.set_xlim([0, xlim])
        ax2.set_ylabel("interpolated (linear) signal difference")
        lgd2 = ax2.legend(loc='center left', bbox_to_anchor=(1, 0.5))

    # add simulated signal to plot
    ax.plot(column['time'], column[h], 'b-', label=h, color='0.75')
    # configure plot
    ax.set_xlim([0, xlim])

    lgd = ax.legend(loc='center left', bbox_to_anchor=(1, 0.5))
    if 'lgd2' not in locals():
        lgd2 = lgd
    plt.xlabel('time [s]')

    DefaultSize = fig.get_size_inches()
    fig.set_size_inches((DefaultSize[0] * 1.8, DefaultSize[1] * 2.5))

    imgfname = h + ".svg"
    fig.savefig(imgfname, bbox_extra_artists=(lgd, lgd2), bbox_inches='tight')

    status = FAILED

    if passedCurrent:
        status = PASSED

    htmlBody += webOutputDivHtml(readSvgHtml(imgfname), status)
    try:

        os.remove(imgfname)
    except Exception:
        pass
# plt.show()


status = PASSED
if os.path.exists("rejected"):
    status = REJECTED
else:
    if passed:
        status = PASSED
    else:
        status = FAILED

with open("index.html", "w") as text_file:
    text_file.write(createHtml(name, config, htmlBody, status))

print "Is passed: " + str(passed)

if status == PASSED:
    if not os.path.exists("passed"):
        with open("passed", "w") as text_file:
            text_file.write("")
else:
    if status == FAILED:
        if not os.path.exists("failed"):
            with open("failed", "w") as text_file:
                text_file.write("result did not match reference signal")
