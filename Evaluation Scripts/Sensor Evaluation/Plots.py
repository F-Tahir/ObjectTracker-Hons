# Given a yml file, this script loops through each frameblock to get accelerometer readings,
# and the phone's x and y translation readings (obtained from Vicon). The script
# then creates a graph for each of these pieces of data, and attempts to find some
# relation between the graphs, using various transformations.
import numpy as np
import matplotlib.pyplot as plt
import optparse
import cv2

optparser = optparse.OptionParser()
optparser.add_option("-y", "--yml", dest="yml", default="rec1processed.yml", help="YML file needing to be parsed")
opts = optparser.parse_args()[0]

f = open(opts.yml, "r")


acc_x = 0
acc_y = 0
t_acc = list()
t_vicon = list()

for line in f:
    # Step 1: Loop through all framestamp blocks and get accummulated accelerator readings
    if "accelerometer_values" in line:
        # Get the x and y readings from file, store in a tuple and append to list
        acc_x += float(f.next().strip().split(":")[1])
        acc_y += float(f.next().strip().split(":")[1])
        t_acc.append((acc_x, acc_y))

    # Step 2: Loop through all framestamp blocks and get phone's translation x and y readings
    if "phone_readings" in line:
        # Skip over the rx/ry/rz readings
        f.next()
        f.next()
        f.next()
        # Get the tx and ty readings, store in a tuple and append to list
        phone_tx = float(f.next().strip().split(":")[1])
        phone_ty = float(f.next().strip().split(":")[1])
        t_vicon.append((phone_tx, phone_ty))

print opts.yml
print t_acc

# Plot the points in t_acc
x_acc = [i[0] for i in t_vicon]
y_acc = [i[1] for i in t_vicon]
acc_graph = plt.figure(1)
acc_graph.canvas.set_window_title("Accelerometer Readings")
plt.plot(x_acc, y_acc)
plt.axis([-1500, 2500, -1500, 2500])

# # PLot the points in t_vicon
# x_vicon = [i[0] for i in t_vicon]
# y_vicon = [i[1] for i in t_vicon]
# vicon_graph = plt.figure(2)
# vicon_graph.canvas.set_window_title("Vicon Readings")
# plt.plot(x_vicon, y_vicon)
# plt.axis([-1500, 2500, -1500, 2500])

plt.show()

# Step 3: Compute optimal affine transformation between T_acc and T_vicon
# => Returns a 2x3 matrix containing rotaional matrix and translation matrix
m = cv2.estimateRigidTransform(np.asarray(t_acc), np.asarray(t_vicon), True)
print m # Returns None

# Step 4: Transform t_acc using the transformation matrix m
# t_acc_transformed = cv2.transform(np.asarray(t_acc), m)

# Step 5: Plot t_vicon against t_acc_transfomed on same graphs
# Outline of same graph should be similar, indicating our accelerometer is accurate


# If outline is not accurate, check the following:
# 1) Alignment of Vicon csv values against yml values (align frames correctly)
# 2) Check whether I'm over-simplifying accelerometer readings
