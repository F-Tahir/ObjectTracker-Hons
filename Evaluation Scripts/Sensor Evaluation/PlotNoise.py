# Given a yml file, this script loops through each frameblock to get accelerometer readings,
# and plots a graph of the values for each axis. This is initially used to plot the
# sensor noise (i.e. value fluctuations when there is no device movement)
import numpy as np
import matplotlib.pyplot as plt
import optparse

optparser = optparse.OptionParser()
optparser.add_option("-y", "--yml", dest="yml", default="../Data/accelerometer/phone_still1.yml", help="YML file needing to be parsed")
opts = optparser.parse_args()[0]

f = open(opts.yml, "r")

t_acc_x = list()
t_acc_y = list()
t_acc_z = list()
t_framestamp = list()

for line in f:
    # Step 1: Loop through all framestamp blocks and get the framestamp
    if "framestamp" in line:
        t_framestamp.append(int(line.strip().split(":")[1]))

    # Step 2: Get the accelerometer values for each framestamp, across each axis.
    if "accelerometer_values" in line:
        t_acc_x.append(float(f.next().strip().split(":")[1]))
        t_acc_y.append(float(f.next().strip().split(":")[1]))
        t_acc_z.append(float(f.next().strip().split(":")[1]))


plt.plot(t_framestamp, t_acc_x, label="x-axis readings")
plt.plot(t_framestamp, t_acc_y, label="y-axis readings")
plt.plot(t_framestamp, t_acc_z, label="z-axis readings")
plt.title("Accelerometer noise")
plt.xlabel("Frame Number")
plt.ylabel("Sensor Value")
plt.legend()
plt.show()
