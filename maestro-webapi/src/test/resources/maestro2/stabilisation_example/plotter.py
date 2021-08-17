import pandas as pd
import matplotlib.pyplot as plt
df = pd.read_csv("stable.csv")
fig,axs=plt.subplots(2)
axs[0].plot(df['time'], df['{m1}.mi1.x1'], label='MSD1 position')
axs[0].plot(df['time'], df['{m2}.mi2.x2'], label='MSD2 position')
axs[0].legend()
df2 = pd.read_csv("unstable.csv")
axs[1].plot(df2['time'], df2['{m1}.mi1.x1'], label='MSD1 position')
axs[1].plot(df2['time'], df2['{m2}.mi2.x2'], label='MSD2 position')
axs[1].legend()

plt.show()