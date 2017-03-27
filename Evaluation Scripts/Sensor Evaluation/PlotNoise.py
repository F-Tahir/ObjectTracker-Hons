# Given a yml file, this script loops through each frameblock to get accelerometer readings,
# and plots a graph of the values for each axis. This is initially used to plot the
# sensor noise (i.e. value fluctuations when there is no device movement)
import numpy as np
import optparse
import matplotlib.pyplot as plt
from scipy.integrate import cumtrapz


optparser = optparse.OptionParser()
optparser.add_option("-y", "--yml", dest="yml", default="../Data/noise/20170324_192850_DUMP.yml", help="YML file needing to be parsed")
optparser.add_option("-a", "--axis", dest="axis", default="x", help="Axis to plot: x, y, z, all")
opts = optparser.parse_args()[0]

f = open(opts.yml, "r")

t_x = list()
t_y = list()
t_z = list()
t_iteration = list()
t_velocity = list()
t_position = list()
t_iteration_s = list()

# Skip over "General Debug Information" block
f.next()
f.next()
f.next()
f.next()



for line in f:
    # Loop through all yml blocks and get iterations, and for that iteration,
    # the sensor readings
    if "time" in line:
        t_iteration.append(int(line.strip().split(":")[1]))
        t_x.append(float(f.next().strip().split(":")[1]))
        t_y.append(float(f.next().strip().split(":")[1]))
        t_z.append(float(f.next().strip().split(":")[1]))

for i in range(0, len(t_iteration)):
    t_iteration_s.append(t_iteration[i]/1000.0)

print len(t_iteration_s)


# Double integrating to obtain positions
t_velocity = cumtrapz(t_y, t_iteration_s, initial=0)
t_position = cumtrapz(t_velocity, t_iteration_s, initial=0)


# Plot sensor data for each iteration.
if (opts.axis == "x"):
    plt.plot(t_iteration, t_x, label="x-axis readings")

if (opts.axis == "y"):
    plt.plot(t_iteration, t_y, label="y-axis readings")

if (opts.axis == "xy"):
    plt.plot(t_iteration, t_x, label="x-axis readings")
    plt.plot(t_iteration, t_y, label="y-axis readings")

if (opts.axis == "z"):
    plt.plot(t_iteration, t_z, label="z-axis readings")

if (opts.axis == "all"):
    plt.plot(t_iteration, t_x, label="x-axis readings")
    plt.plot(t_iteration, t_y, label="y-axis readings")
    plt.plot(t_iteration, t_z, label="z-axis readings")
    plt.legend()

plt.xlabel("Time (milliseconds)")
plt.ylabel("Gyroscope Noise (y-axis)")

plt.show()
