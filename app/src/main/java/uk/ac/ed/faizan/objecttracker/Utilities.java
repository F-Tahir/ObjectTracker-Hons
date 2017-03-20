package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static uk.ac.ed.faizan.objecttracker.R.id.tracking_mode;


/**
 * Application Utilities. Consists of functions used to create the media and data files used during
 * recording.
 */
public final class Utilities {
	private static final String TAG = Utilities.class.getSimpleName();


	/**
	 * This is used to format the time in a yyyyMMdd_HHmmss format, and the data and video file
	 * are named according to this format, to create unclashable names.
	 */
	private static ThreadLocal<SimpleDateFormat> sFileDateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);
		}
	};

	/**
	 * This is used to format the time in a yyyyMMdd format, and a folder is created according to this
	 * name. This means that all recordings and data files in one day are stored in the same folder.
	 */
	private static ThreadLocal<SimpleDateFormat> sDirDateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		}
	};

	/**
	 * This is used to format the time in a day-month-year-time format, and this is appended to the start
	 * of the yml file.
	 */
	private static ThreadLocal<SimpleDateFormat> sDataTimeFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("E, dd MMM yyyy, HH:mm:ss", Locale.ENGLISH);
		}
	};

	/**
	 * This is used to format the time in a hour:min:second:millisecond format, used for sensor
	 * readings
	 */
	private static ThreadLocal<SimpleDateFormat> sWorldTimeFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss:SSS", Locale.ENGLISH);
		}
	};



	/**
	 * Checks to see whether the device has accessible external storage, and that we can read
	 * and write from it.
	 *
	 * @return True if the stoage is read/writable, false otherwise.
	 */
	public static boolean isExternalStorageMounted() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}


	/**
	 * Creates a non-clashable video output file in the root ObjectTracker folder
	 *
	 * @param time This parameter ensures a file with a non-clashable name is created.
	 */
	@Nullable
	public static File getVideoFile(long time) {
		return getExternalFile(time, "VID", "mp4", false);
	}

	/**
	 * Creates a non-clashable data output (.yml) file in the root ObjectTracker folder. This data
	 * file is used to store (x,y) coordinates as well as frame/timestamps of the object being tracked.
	 *
	 * @param time This parameter ensures a file with a non-clashable name is created.
	 */
	@Nullable
	public static File getDataFile(long time) {
		return getExternalFile(time, "DATA", "yml", false);
	}

	/**
	 * Creates a non-clashable debug output (.yml) file in the  ObjectTracker/DEBUG folder. This data
	 * file is used to store sensory readings to debug sensors
	 *
	 * @param time This parameter ensures a file with a non-clashable name is created.
	 */
	@Nullable
	public static File getDebugFile(long time) {
		return getExternalFile(time, "DUMP", "yml", true);
	}




	/**
	 * Creates the root ObjectTracker directory if it does not exist. Within that directory, creates
	 * a folder with the naming format yyyyMMdd (i.e. a new folder for each day), so that the folders
	 * are not cluttered. Within this folder, creates a furhter file depending on the <i>ext</i> parameter.
	 *
	 * @param time This parameter ensures a file with a non-clashable name is created.
	 * @param ext  Can be .yml or .mp4. If .yml, creates the data file to store tracking info. If .mp4, creates
	 *             a video file.
	 * @return Returns the path to the newly created .mp4 or .yml file.
	 */
	@Nullable
	private static File getExternalFile(long time, String type, String ext, boolean debug) {
		if (!isExternalStorageMounted()) {
			return null;
		}

		File root = new File(Environment.getExternalStorageDirectory(), "ObjectTracker");

		File dir;

		// If debug is false, create a normal folder for recordings, otherwise create a folder called DEBUG
		if (!debug) {
			dir = new File(root, sDirDateFormat.get().format(time));
		} else {
			dir = new File(root, "_DEBUG_");
		}
		if (!dir.exists() && !dir.mkdirs()) {
			return null;
		}

		return new File(dir, String.format(Locale.ENGLISH, "%s_%s.%s",
				sFileDateFormat.get().format(time), type, ext));


	}


	/**
	 * This method returns the current time, in "day, date month year @ hours:mins:seconds" format.
	 * This string is then apppended to the start of the yml data file.
	 *
	 * @param time Time in milliseconds from current epoch.
	 * @return		Time in milliseconds formatted in a human-readable way.
	 */
	public static String getTimeForDataFile(long time) {
		return sDataTimeFormat.get().format(time);
	}


	/**
	 * This function is called after mMediaRecorder.prepare() in CameraPreview to document some data
	 * at the start of the yml file. Data such as the tracking mode (automatic or manual), matching
	 * method (if automatic mode), as well as image resolution are recorded at the start of the yml file.
	 *
	 * This method is overloaded - the other method of the same name is used to append the object data
	 * at each frame (or click, if in manual mode).
	 *
	 * @param dataFile 			The file to append to
	 * @param trackingMode		Automatic or manual tracking
	 * @param matchingMode		Matching method (if in automatic mode)
	 * @param resolutionWidth	Resolution (width) of the image being displayed on-screen
	 * @param resolutionHeight	Resolution (height) of the image being displayed on-screen
	 * @param templateWidth		Resolution (width) of the template, if automatic tracking
	 * @param templateHeight	Resolution (height) of the template, if automatic tracking
	 */
	public static void appendToDataFile(@NonNull File dataFile, int trackingMode, int matchingMode,
										String time, int resolutionWidth, int resolutionHeight,
										int templateWidth, int templateHeight) {

		String trackingModeString;
		String matchingModeString;
		String templateResolutionString;

		if (trackingMode == 0) {
			trackingModeString = "Manual Tracking";
			templateResolutionString = "N/A";
		} else {
			trackingModeString = "Automatic Tracking";
			templateResolutionString = String.format(Locale.ENGLISH, "%dx%d", templateWidth, templateHeight);
		}

		if (matchingMode == 0) {
			matchingModeString = "Square Difference";
		} else if (matchingMode == 1) {
			matchingModeString = "Square Difference Normalized";
		} else if (matchingMode == 2) {
			matchingModeString = "Cross-Correlation";
		} else if (matchingMode == 3) {
			matchingModeString = "Cross-Correlation Normalized";
		} else if (matchingMode == 4) {
			matchingModeString = "Correlation Coefficient";
		} else if (matchingMode == 5) {
			matchingModeString = "Correlation Coefficient Normalized";
		} else {
			matchingModeString = "N/A (manual tracking mode)";
		}

		try {
			if (!dataFile.exists() && !dataFile.createNewFile()) {
				return;
			}
		} catch (IOException e) {
			Log.e(TAG, "appendToFile", e);
		}

		try {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(dataFile.toString(), true);
				byte[] buffer = String.format(Locale.ENGLISH, "# General Recording Information\n\n" +
					"time_of_recording: %s \ntracking_mode: %s\nmatching_mode: %s\nimage_size: %dx%d" +
					"\ntemplate_size: %s\n\n\n\n " +
					"# Tracking Information\n\n", time, trackingModeString, matchingModeString,
					resolutionWidth, resolutionHeight, templateResolutionString).getBytes();

				fos.write(buffer);
				fos.flush();
				Log.i(TAG, "Wrote to " + dataFile);
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
		} catch (FileNotFoundException e) {
			Log.i(TAG, "Data file was not found - check that it was created properly.");
		} catch (IOException e) {
			Log.i(TAG, "IOException occured when trying to close FileOutputStream for data file.");
		}
	}



	/**
	 * Appends data to the file created in getOutputDataFile(). This function is called in manual
	 * tracking mode, each time the user clicks on the screen. A circle is drawn, and the touch
	 * coordinates are stored using this function. This function is also called in onCameraFrame()
	 * when in automatic mode, so that we record the position of the object at each frame.
	 *
	 * <p>The <i>xCoord</i> and <i>yCoord</i> parameters that are passed in are in terms of the video
	 * size, not the screen size (which can be different). The maths is done in the drawCircle() function
	 * in CameraPreview.</p>
	 *
	 * <p>If xCoord = -1 or yCoord = -1, this means that this was just a sensor reading update,
	 * and so the object position does not need to be written to file. This happens only in manual
	 * tracking mode.</p>
	 *
	 *
	 * @param dataFile  		The file to append data to
	 * @param timeStamp 		The time of touch (in terms of record time)
	 * @param xCoord    		The x-coordinate of the touch
	 * @param yCoord    		The y-coordinate of the touch
	 * @param accelValues 		The latest accelerometer readings, in an array of size 3
	 * @param gyroValues 		The latest gyroscopen readings, in an array of size 3
	 * @see CameraPreview
	 */
	public static void appendToDataFile(@NonNull File dataFile, int frameCount, String timeStamp, float xCoord, float yCoord,
										float[] accelValues, float[] gyroValues, long time) {
		try {
			if (!dataFile.exists() && !dataFile.createNewFile()) {
				return;
			}
		} catch (IOException e) {
			Log.e(TAG, "appendToFile", e);
		}

		try {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(dataFile.toString(), true);

				byte[] buffer;
				if (xCoord != -1 && yCoord != -1) {
					buffer = String.format(Locale.ENGLISH, "framestamp: %d\n\ttimestamp: %s\n\tworld_time: %s\n\tobject_position:" +
							"\n\t\tx: %d\n\t\ty: %d \n\taccelerometer_values:\n\t\tx: %.2f\n\t\ty: %.2f" +
							"\n\t\tz: %.2f\n\tgyroscope_values:\n\t\tx: %.2f\n\t\ty: %.2f\n\t\tz: %.2f\n\n",
						frameCount, timeStamp, sWorldTimeFormat.get().format(time), (int) xCoord, (int) yCoord, accelValues[0], accelValues[1],
						accelValues[2], gyroValues[0], gyroValues[1], gyroValues[2]).getBytes();

					// Sensor changing in manual mode, but user has not clicked. Need to record
					// new sensor readings only.
				} else {
					buffer = String.format(Locale.ENGLISH, "framestamp: %d\n\ttimestamp: %s\n\tworld_time: %s\n\taccelerometer_values:" +
						"\n\t\tx: %.2f\n\t\ty: %.2f \n\t\tz: %.2f\n\tgyroscope_values:" +
						"\n\t\tx: %.2f\n\t\ty: %.2f\n\t\tz: %.2f\n\n",
						frameCount, timeStamp, sWorldTimeFormat.get().format(time), accelValues[0], accelValues[1],
						accelValues[2], gyroValues[0], gyroValues[1], gyroValues[2]).getBytes();
				}
				fos.write(buffer);
				fos.flush();
				Log.i(TAG, "Wrote to " + dataFile);
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
		} catch (FileNotFoundException e) {
			Log.i(TAG, "Data file was not found - check that it was created properly.");
		} catch (IOException e) {
			Log.i(TAG, "IOException occured when trying to close FileOutputStream for data file.");
		}
	}



	/**
	 * This method is called ewery time the UI buttons need to be enabled or disabled. In the object
	 * tracking scenario, it is called every time the record button is pressed.  UI buttons are disabled
	 * during recording so that the user cannot select a different tracking mode whilst recording, for example.
	 *
	 * @param trackingModeButton The id of the button used to change the tracking mode.
	 * @param freezeButton       The id of the button used to freeze the camera preview.
	 * @param isRecording        Boolean to state whether the device is recording a current video or not.
	 */
	public static void reconfigureUIButtons(View trackingModeButton, View freezeButton, View methodButton,
											boolean isRecording, int trackingMode) {

		if (isRecording) {
			trackingModeButton.setEnabled(false);
			freezeButton.setEnabled(false);
			methodButton.setEnabled(false);

			trackingModeButton.setAlpha(0.5f);
			freezeButton.setAlpha(0.5f);
			methodButton.setAlpha(0.5f);



		} else if (!TrackingActivity.templateSelectionInitialized){
			trackingModeButton.setEnabled(true);
			freezeButton.setAlpha(0.5f);
			freezeButton.setEnabled(false);

			trackingModeButton.setAlpha(1.0f);


			if (trackingMode == 1) {
				methodButton.setEnabled(true);
				methodButton.setAlpha(1.0f);
			} else {
				methodButton.setEnabled(false);
				methodButton.setAlpha(0.5f);
			}

		}
	}


	/**
	 * Given an x-coordinate in terms of the screen resolution (which can be higher or lower than the
	 * image resolution), this function converts the coordinate in terms of the camera resolution, so that
	 * overlays and templates are drawn/created in the right positions.
	 *
	 * @param mX          The x-coordinate of the touched location, in terms of screen size
	 * @param cameraWidth The width of the camera resolution (1280px currently)
	 * @param screenWidth The width of the screen resolution
	 * @return The converted x-coord in terms of camera resolution
	 */
	public static int convertDeviceXToCameraX(float mX, int cameraWidth, int screenWidth) {
		return (int) (mX * cameraWidth) / screenWidth;
	}


	/**
	 * Given an y-coordinate in terms of the screen resolution (which can be higher or lower than the
	 * image resolution), this function converts the coordinate in terms of the camera resolution, so that
	 * overlays and templates are drawn/created in the right positions.
	 *
	 * @param mY           The y-coordinate of the touched location, in terms of screen size
	 * @param cameraHeight The height of the camera resolution (1280px currently)
	 * @param screenHeight The height of the screen resolution
	 * @return The converted y-coord in terms of camera resolution
	 */
	public static int convertDeviceYToCameraY(float mY, int cameraHeight, int screenHeight) {
		return (int) (mY * cameraHeight) / screenHeight;
	}


	/**
	 * Given a list of required permissions, this function checks whether all of the permissions required
	 * have been granted, and returns true if so. If at least one function has <b>not</b> been granted,
	 * this application will return false.
	 *
	 * @param context     The activity that this function was called in
	 * @param permissions The list of required permissions
	 * @return True if all required permissions have been granted; false otherwise.
	 */
	public static boolean hasPermissions(Context context, String... permissions) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * This function checks that all the grant results for the required permissions are "PERMISSION
	 * GRANTED". The return value is used to decide whether or not to start an intent that requires
	 * the permissions. If at least one permission has been denied (grantResult[i] = PERMISSION_DENIED),
	 * then this indicates we cannot start the intent.
	 *
	 * @param grantResults The results for each of the required permission - PERMISSION_GRANTED or
	 *                     PERMISSION_DENIED, depending on users action
	 * @return True if all required permissions have been granted, false otherwise.
	 */
	public static boolean allPermissionsGranted(int[] grantResults) {

		if (grantResults.length > 0) {
			for (int result : grantResults) {
				if (result == PackageManager.PERMISSION_DENIED) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * This function is called when the app wants to query the amount of free space available on the
	 * device. This is done to ensure that the video file does not become corrupt due to no free space.
	 *
	 * @return Number of bytes available on external storage
	 */
	public static long getAvailableSpaceInBytes() {
		long availableSpace = -1L;
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

		return availableSpace;
	}

	/**
	 * This function is called when the app wants to query the amount of free space available on the
	 * device. This is done to ensure that the video file does not become corrupt due to no free space.
	 *
	 * @return Number of gigabytes available on external storage
	 */
	public static double getAvailableSpaceInGB(){
		final double SIZE_KB = 1024.0;
		final double SIZE_GB = SIZE_KB * SIZE_KB * SIZE_KB;
		double availableSpace = -1.0;
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
		return availableSpace/SIZE_GB;
	}
}
