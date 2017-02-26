# Given a yml file, this script loops through each frameblock to get accelerometer readings,
# and plots a graph of the values for each axis. This is initially used to plot the
# sensor noise (i.e. value fluctuations when there is no device movement)
import numpy as np
import optparse
import matplotlib.pyplot as plt


optparser = optparse.OptionParser()
optparser.add_option("-y", "--yml", dest="yml", default="../Data/accelerometer/accel_1000_ui.yml", help="YML file needing to be parsed")
optparser.add_option("-a", "--axis", dest="axis", default="x", help="Axis to plot: x, y, z, all")
opts = optparser.parse_args()[0]

f = open(opts.yml, "r")

t_x = list()
t_y = list()
t_z = list()
t_iteration = list()

# Skip over "General Debug Information" block
f.next()
f.next()
f.next()
f.next()


for line in f:
    # Loop through all yml blocks and get iterations, and for that iteration,
    # the sensor readings
    if "iteration" in line:
        t_iteration.append(int(line.strip().split(":")[1]))
        t_x.append(float(f.next().strip().split(":")[1]))
        t_y.append(float(f.next().strip().split(":")[1]))
        t_z.append(float(f.next().strip().split(":")[1]))


# Plot sensor data for each iteration.

if (opts.axis == "x"):
    print "Hereeee"
    plt.plot(t_iteration, t_x, label="x-axis readings")

if (opts.axis == "y"):
    plt.plot(t_iteration, t_y, label="y-axis readings")

if (opts.axis == "z"):
    plt.plot(t_iteration, t_z, label="z-axis readings")

if (opts.axis == "all"):
    print "Here"
    plt.plot(t_iteration, t_x, label="x-axis readings")
    plt.plot(t_iteration, t_y, label="y-axis readings")
    plt.plot(t_iteration, t_z, label="z-axis readings")
    plt.legend()

plt.title("Sensor Data")
plt.xlabel("Frame Number")
plt.ylabel("Sensor Value")

plt.show()
