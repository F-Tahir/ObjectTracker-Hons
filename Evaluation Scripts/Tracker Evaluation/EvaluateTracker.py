# This script allows me to loop through a video frame-by-frame, and click on
# the location of the object, with 100% accuracy. The locations are appended
# to an output file, and can be used for evaluating the accuracy of my manual
# or automatic tracking
import optparse
import math
import matplotlib.pyplot as plt

optparser = optparse.OptionParser()
optparser.add_option("-m", "--man", dest="man", default="../Data/Automatic-Recordings/20170322_194423_GT.yml",
    help="New YML file (with ground truth results from GetFrameData.py)")
optparser.add_option("-a", "--aut", dest="aut", default="../Data/Automatic-Recordings/20170322_194423_DATA.yml",
    help="Original YML file (with automatic tracking results from app)")
optparser.add_option("-k", "--heur", dest="heuristic", default="euclid",
    help="Evaluation heuristic (\"euclid\" or \"accuracy\").")
optparser.add_option("-f", "--frames", dest="frames", default=2000, type="int", help="Number of frames to analyze")
optparser.add_option("-t", "--thresh", dest="threshold", default=20, type="int", help="Threshold for error detection.")
opts = optparser.parse_args()[0]


manfile = open(opts.man, "r")
autofile = open(opts.aut, "r")


# Stores tuples in the form (framecount, (x, y))
man_results = list()
auto_results = list()

# Loop through the manually obtained  (ground truth)results and store them in tuples of above form
for line in manfile:
    if "framestamp" in line:
        framestamp = int(line.strip().split(":")[1])
        x = int(manfile.next().strip().split(":")[1])
        y = int(manfile.next().strip().split(":")[1])
        man_results.append((framestamp, (x, y)))

        if framestamp == opts.frames:
            break

# Loop through the automatically obtained results (from automatic tracking in
# android app) and store then in tuples of the above form
for line in autofile:
    if "framestamp" in line:
        framestamp = int(line.strip().split(":")[1])
        # Skip timestamp and object_position header
        autofile.next()
        autofile.next()
        autofile.next()

        x = int(autofile.next().strip().split(":")[1])
        y = int(autofile.next().strip().split(":")[1])
        auto_results.append((framestamp, (x, y)))

        # Only need same amount of tuples as man_results
        if framestamp == len(man_results) or framestamp == opts.frames:
            break




# Print heuristics
print "\n\n######################################################################"
print "                     EVALUATION SUMMMARY"
print "######################################################################\n"
print "Evaluation method: %s" % opts.heuristic
print "Evaluation method: %s" % opts.heuristic


print(len(man_results))
print(len(auto_results))
# Calculate Euclidean distance between points in same frame correspondence, between
# automatic and manual tracking. Sum up and take average.
if opts.heuristic == "euclid":
    distance = 0
    distance += sum(math.hypot(man[1][0] - auto[1][0],
        man[1][1] - auto[1][1]) for man, auto in zip (man_results, auto_results))
    print """Description: This heuristic method computes the euclidean distance between
    the predicted position and the ground truth position, for each frame, and then
    takes an average.\n"""
    print "Total # Frames: %s" % str(len(man_results))
    print "Average Euclidean Distance: %s\n\n" % str(distance/len(man_results))

# Return number of frames where tracking is accurate/inaccurate by measuring
# euclidean distance from predicted position to ground truth position.
elif opts.heuristic == "accuracy":
    accurate = 0
    inaccurate = 0
    for i in xrange(0, len(man_results)):
        lhs = (man_results[i][1][0] - auto_results[i][1][0])**2
        rhs = (man_results[i][1][1] - auto_results[i][1][1])**2
        distance = math.sqrt(lhs + rhs)

        if distance <= opts.threshold/2:
            accurate += 1
        else:
            inaccurate += 1

    print """Description: This heuristic method calculates the euclidean distance
    between predicted and ground truth positions, for each frame. If the distance
    is below a threshold, the frame is classed as accurate, otherwise it is inaccurate."""
    print "Correct # frames: %s" % str(accurate)
    print "Incorrect # frames: %s" % str(inaccurate)
    print "Total # Frames: %s" % str(len(man_results))
    print "Accuracy: %s\n\n" % str((float(accurate)/len(man_results))*100.0)

# Return a graph that shows plots of both the manually obtained data, and the
# automatically obtained data. The graphs overlayed on each other can show similarity.
elif opts.heuristic == "graph":
    print """Description: This heuristic method shows plots of the manually obtained
    data against the automatically obtained data. The graphs overlayed on each otherwise
    can show similarity."""

    man_x = [seq[1][0] for seq in man_results]
    man_y = [seq[1][1] for seq in man_results]
    aut_x = [seq[1][0] for seq in auto_results]
    aut_y = [seq[1][1] for seq in auto_results]
    plt.plot(man_x, man_y)
    plt.plot(aut_x, aut_y)

    plt.legend(["Ground-truth", "Automatic Tracking"])
    plt.show()
    # plt.plot(man_results[])

elif opts.heuristic == "similarity":
    # Implement similarity code from INF2B
    print "Todo"
