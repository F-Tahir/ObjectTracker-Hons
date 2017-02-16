# This script allows me to loop through a video frame-by-frame, and click on
# the location of the object, with 100% accuracy. The locations are appended
# to an output file, and can be used for evaluating the accuracy of my manual
# or automatic tracking
import optparse
import math


optparser = optparse.OptionParser()
optparser.add_option("-m", "--man", dest="man", default="rec2output.yml",
    help="Original YML file (with automatic tracking results from app)")
optparser.add_option("-a", "--aut", dest="aut", default="../Data/rec2.yml",
    help="New YML file (with manual tracking results from GetFrameData.py)")
optparser.add_option("-f", "--frames", dest="frames", default=2000, type="int", help="Number of frames to analyze")
optparser.add_option("-t", "--type", dest="type", default="euclid", help="Evaluation heuristic")
opts = optparser.parse_args()[0]


manfile = open(opts.man, "r")
autofile = open(opts.aut, "r")


# Stores tuples in the form (framecount, (x, y))
man_results = list()
auto_results = list()
distance  = 0

# Loop through the manually obtained results and store them in tuples of above form
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

        x = int(autofile.next().strip().split(":")[1])
        y = int(autofile.next().strip().split(":")[1])
        auto_results.append((framestamp, (x, y)))

        # Only need same amount of tuples as man_results
        if framestamp == len(man_results) or framestamp == opts.frames:
            break


print man_results[1][1][0]

# Calculate Euclidean distance between points in same frame correspondence, between
# automatic and manual tracking. Sum up and take average.
if opts.type == "euclid":
    distance += sum(math.hypot(man[1][0] - auto[1][0],
        man[1][1] - auto[1][1]) for man, auto in zip (man_results, auto_results))

    print "Evaluation using Euclidean distance is %s" % str(distance/len(man_results))
    # Possibly print other valuable heuristic measurements too.

else:
    # Manning distance/city block distance/etc?
    print "Undefined method"
