import pandas as pd
import matplotlib.pyplot as plt
df = pd.read_csv("expectedoutputs.csv")
plt.plot(df['time'], df['m1.m1_mi1.v1'], label="m1_mi1.v1")
plt.plot(df['time'], df['m2.m2_mi2.fk'], label="m2_mi2.fk")
plt.plot(df['time'], df['m1.m1_mi1.x1'], label="m1_mi1.x1")
plt.legend()

plt.show()