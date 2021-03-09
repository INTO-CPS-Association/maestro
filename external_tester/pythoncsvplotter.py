import pandas as pd
import matplotlib.pyplot as plt
df = pd.read_csv("outputs.csv")
plt.plot(df['time'], df['{crtl}.crtlInstance.valve'], label='valve state')
plt.plot(df['time'], df['{wt}.wtInstance.level'], label='level')
plt.legend()

plt.show()