import numpy as np
import matplotlib.pyplot as plt
import optparse

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
        acc_x = float(f.next().strip().split(":")[1])
        acc_y = float(f.next().strip().split(":")[1])
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



# Plot the points in T_acc and T_vicon in separate graphs
x_acc = [i[0] for i in t_vicon]
y_acc = [i[1] for i in t_vicon]
plt.plot(x_acc, y_acc)
plt.axis('equal')
plt.show()


# Step 3: Compute optimal affine transformation between T_acc and T_vicon, using opencv
# => M = cv2.estimateRigidTransform(T_acc, T_vicon, false)
# => Returns a 2x3 matrix containing rotaional matrix and translation matrix

# Step 4: Transform T_acc using the transformation matrix
# => T_acc` = cv2.transform(T_acc, M)

# Step 5: Plot T_vicon against T_acc' (transformed values) on same graphs
# Outline of same graph should be similar, indicating our accelerometer is accurate

# If outline is not accurate, check the following:
# 1) Alignment of Vicon csv values against yml values (align frames correctly)
# 2) Check whether we're over-simplifying accelerometer readings
