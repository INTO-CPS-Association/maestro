import numpy as np
import matplotlib.pyplot as plt
import sys
#http://stackoverflow.com/questions/4700614/how-to-put-the-legend-out-of-the-plot

def configFig(axl,xLim,yLim) :
	# Shrink current axis by 20%
	box = axl.get_position()
	axl.set_position([box.x0, box.y0, box.width * 0.8, box.height])
	# Put a legend to the right of the current axis
	axl.legend(loc='center left', bbox_to_anchor=(1, 0.5))
	
	plt.ylim(yLim)
	plt.xlim(xLim)
	
	plt.grid(True)


def graph(filename,xMax):

	converter = lambda x : 1 if x.strip() == "true" else 0

	time , step ,valve , level  = np.loadtxt(filename, delimiter=',', unpack=True, skiprows=1,converters={2:converter})
	
	fig = plt.figure()

	xLim = [0,xMax]

	#
	# level 1
	#
	axl = plt.subplot(311)
	plt.plot(time , level, label='level')
	configFig(axl, xLim, [-1,12])
	
	#
	# level 2
	#
	axl = plt.subplot(312)
	plt.plot(time , valve, label='valve')
	configFig(axl,xLim, [0,4])
	
	

	plt.xlabel('time [s]')
	
	plt.savefig(filename+'.pdf')


graph(sys.argv[1],float(sys.argv[2]))

