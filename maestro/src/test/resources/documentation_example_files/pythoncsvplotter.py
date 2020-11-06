import pandas as pd
import matplotlib.pyplot as plt
df = pd.read_csv("outputs.csv")
plt.plot(df['time'], df['crtlValveState'], label="valve state")
plt.plot(df['time'], df['wtLevel'], label="level")
plt.legend()

plt.show()