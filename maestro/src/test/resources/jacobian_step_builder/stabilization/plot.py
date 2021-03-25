from pandas import DataFrame, read_csv
import matplotlib.pyplot as plt
import pandas as pd
import sys

path= sys.argv[1]

#print path
# define data location
df = read_csv(path)
df.fillna(0, inplace=True)

fig = plt.figure()
fig.canvas.set_window_title(path)

for col in df:
	if col=='time':
		continue
	plt.plot(df['time'],df[col],label=col)

plt.legend(bbox_to_anchor=(1, 1),
           bbox_transform=plt.gcf().transFigure)

#plt.ylim( (-300, 300) )
plt.savefig(path+".pdf", format='pdf')
