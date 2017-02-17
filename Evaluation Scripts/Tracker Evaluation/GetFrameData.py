# This script allows me to loop through a video frame-by-frame, and click on
# the location of the object, with 100% accuracy. The locations are appended
# to an output file, and can be used for evaluating the accuracy of my manual
# or automatic tracking
import cv2
import optparse


optparser = optparse.OptionParser()
optparser.add_option("-r", "--rec", dest="rec", default="../Data/rec1.mp4", help="Recording to analyse.")
optparser.add_option("-o", "--out", dest="out", default="rec1output.yml", help="Output file to store data in")
optparser.add_option("-f", "--frames", dest="frames", default=2000, type="int", help="Number of frames to analyze")
opts = optparser.parse_args()[0]


cap = cv2.VideoCapture(opts.rec)


cv2.namedWindow('video')


global frame, framecount, f
f = open(opts.out, 'a')
framecount = 0
frame = None


# Function called each time mouse is clicked down. Store x and y coordinates,
# with framestamp in a yml file
def get_coord(event, x, y, flags, param):
    global frame, framecount
    if event == cv2.EVENT_LBUTTONDOWN:

        # First frame, initialise frame variable (currently it is None)
        if framecount == 0:
            ret, frame = cap.read()
            framecount = 1
        else:
            # Update frame so that the loop below can show next image
            ret, frame = cap.read()
            f.write("framestamp:" + str(framecount) + "\n")
            f.write("\tx:" + str(x) + "\n")
            f.write("\ty:" + str(y) + "\n\n\n")
            print framecount
            framecount += 1


# Set callback to call get_coord
cv2.setMouseCallback('video', get_coord)
print "Starting manual tracking for ground truth"
print "Video:" + opts.rec
print "Output file: " + opts.out
print "Number of frames to analyse: " + str(opts.frames)


# Infinite loop while cap is opened - frame is only updated on click
while cap.isOpened():
    if frame != None:
        cv2.imshow('video', frame)

    # Break if keyboard click
    if cv2.waitKey(10) & 0xFF == ord('q'):
        break

    # Break after we have analyzed set number of frames
    if framecount == opts.frames+1:
        break

cap.release()
f.close()
