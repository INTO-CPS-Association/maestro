import numpy as np
import matplotlib.pyplot as plt
import sys


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

	time , step ,level2 ,level1, waterout , valve , valve2 = np.loadtxt(filename, delimiter=',', unpack=True, skiprows=1,converters={5:converter, 6:converter})
	
	sim20= np.loadtxt('plots-cwt-hacked.csv', delimiter=',', unpack=False, skiprows=1)
	sim20M = np.asmatrix(sim20)
	
	fig = plt.figure()

	xLim = [0,xMax]

	#
	# level 1
	#
	axl = plt.subplot(311)
	plt.plot(time , level1, label='level1')
	plot11=plt.plot(np.asarray(sim20M[:,0]) , np.asarray(sim20M[:,1]), label='level1-sim',color="red")
	configFig(axl, xLim, [-1,3])
	
	#
	# level 2
	#
	axl = plt.subplot(312)
	plt.plot(time , level2, label='level2')
	plot22=plt.plot(np.asarray(sim20M[:,0]) , np.asarray(sim20M[:,10]), label='level2-sim',color="red")
	configFig(axl,xLim, [0,4])
	
	#
	# valve 2
	#
	axl = plt.subplot(313)
	plt.plot(time , valve2, label='valve2')
	plot33=plt.plot(np.asarray(sim20M[:,0]) , np.asarray(sim20M[:,11]), label='valve2-sim',color="red")
	configFig(axl,xLim, [0,1.5])
	

	plt.xlabel('time [s]')
	
	plt.savefig(filename+'-check.pdf')
	plt.show()

graph(sys.argv[1],float(sys.argv[2]))

