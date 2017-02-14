import numpy as np
import matplotlib.pyplot as plt


# Step 1: Loop through all framestamp blocks and get accummulated accelerator readings
# => X(t) =  sumof (i = 1 to t) x(i)
# => Y(t) = sumof (i = 1 to t) (yi)
# Store (X(t), Y(t)) tuples in a numpy array, called T_acc

# Step 2: Loop through all framestamp blocks and get phone's translation x and y readings
# => X(t), Y(t) = { (x,y) | x, y in set of real numbers} = X/y readings at frame t
# Store (X(t), Y(t)) tuples in a numpy array, called T_vicon


# Plot the points in T_acc and T_vicon in separate graphs

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
