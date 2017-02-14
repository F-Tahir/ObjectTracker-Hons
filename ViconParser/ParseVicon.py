import csv
import optparse
import time
import itertools

optparser = optparse.OptionParser()
optparser.add_option("-c", "--csv", dest="csv", default="rec1.csv", help="CSV file needing to be parsed")
optparser.add_option("-y", "--yml", dest="yml", default="rec1.yml", help="YML file needing to be parsed")
optparser.add_option("-f", "--fps", dest="fps", default=12, type="int", help="FPS of recording from Android device")
optparser.add_option("-n", "--newfile", dest="dest", default="rec1processed.yml", help="Location of file to write data to.")
optparser.add_option("-t", "--threshold", dest="threshold", default=0.01, type="int", help="Location of file to write data to.")
opts = optparser.parse_args()[0]

# Rec1 fps is fps: 12.35138744

f = open(opts.yml, "r")   # Source file to write to
f1 = open(opts.dest, 'a') # Destination file to write to


for line in f:
	f1.write(line)
	# Detect timestamp entry in yml file
	if "timestamp" in line:
		# Strip the line from any whitespace, split it on colons, and convert timestamp to seconds/milliseconds
		timestamp = line.strip()
		timestamp = timestamp.split(':')
		# Ignore hours as the test recordings are only few mins long
		seconds = int(timestamp[2])*60 + int(timestamp[3]) + int(timestamp[4])/1000.0
	
		# Loop through csv file to find closest timestamp with some threshold
		with open(opts.csv, "r") as csvfile:
			reader = csv.DictReader(csvfile)
			for row in reader:
				csvtimestamp = float(row['Frame'])/120.0

				if seconds - csvtimestamp < opts.threshold:
					# Write 11 lines from timestamp to gyro readings, then append Vicon readings after this
					for line in itertools.islice(f, 11):
						f1.write(line)

					f1.write("\tphone_readings:\n")
					f1.write("\t\trx: " + row["Phone_RX"] + "\n")
					f1.write("\t\try: " + row["Phone_RY"] + "\n")
					f1.write("\t\trz: " + row["Phone_RZ"] + "\n")
					f1.write("\t\ttx: " + row["Phone_TX"] + "\n")
					f1.write("\t\tty: " + row["Phone_TY"] + "\n")
					f1.write("\t\ttz: " + row["Phone_TZ"] + "\n")

					f1.write("\trobot_readings:\n")
					f1.write("\t\trx: " + row["Robot_RX"] + "\n")
					f1.write("\t\try: " + row["Robot_RY"] + "\n")
					f1.write("\t\trz: " + row["Robot_RZ"] + "\n")
					f1.write("\t\ttx: " + row["Robot_TX"] + "\n")
					f1.write("\t\tty: " + row["Robot_TY"] + "\n")
					f1.write("\t\ttz: " + row["Robot_TZ"] + "\n")
					break

		



					
				






		

			
						

		





				

