package uk.ac.ed.faizan.objecttracker;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Application Utilities. Consists of functions used to create the media and data files used during
 * recording.
 *
 */
public final class Utils
{
	private static final String TAG = Utils.class.getSimpleName();

	private static ThreadLocal<SimpleDateFormat> sFileDateFormat = new ThreadLocal<SimpleDateFormat>(){
		@Override
		protected SimpleDateFormat initialValue()
		{
			return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);
		}
	};

	private static ThreadLocal<SimpleDateFormat> sDirDateFormat = new ThreadLocal<SimpleDateFormat>(){
		@Override
		protected SimpleDateFormat initialValue()
		{
			return new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		}
	};


	/**
	 * Checks to see whether the device has accessible external storage (i.e. it is mounted).
	 *
	 * @return True if the stoage is mounted, false otherwise.
	 */
	public static boolean isExternalStorageMounted()
	{
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}


	/**
	 * Creates a non-clashable video output file in the root ObjectTracker folder
	 *
	 * @param time This parameter ensures a file with a non-clashable name is created.
	 */
	@Nullable
	public static File getVideoFile(long time)
	{
		return getExternalFile(time, "mp4");
	}

	/**
	 * Creates a non-clashable data output (.yml) file in the root ObjectTracker folder
	 *
	 * @param time This parameter ensures a file with a non-clashable name is created.
	 */
	@Nullable
	public static File getDataFile(long time)
	{
		return getExternalFile(time, "yml");
	}


	/**
	 * Creates the root ObjectTracker directory if it does not exist. Within that directory, creates
	 * a folder with the naming format yyyyMMdd (i.e. a new folder for each day), so that the folders
	 * are not cluttered. Within this folder, creates a furhter file depending on the <i>ext</i> parameter.
	 *
	 * @param time This parameter ensures a file with a non-clashable name is created.
	 * @param ext Can be .yml or .mp4. If .yml, creates the data file to store tracking info. If .mp4, creates
	 *            a video file.
	 * @return Returns the path to the newly created .mp4 or .yml file.
	 */
	@Nullable
	private static File getExternalFile(long time, String ext)
	{
		if(!isExternalStorageMounted()){
			return null;
		}

		File root = new File(Environment.getExternalStorageDirectory(), "ObjectTracker");
		File dir = new File(root, sDirDateFormat.get().format(time));
		if(!dir.exists() && !dir.mkdirs()){
			return null;
		}

		return new File(dir, String.format(Locale.ENGLISH, "%s.%s",
			sFileDateFormat.get().format(time), ext));
	}


	/**
	 * Appends data to the file created in getOutputDataFile(). This function is called in manual
	 * tracking mode, each time the user clicks on the screen. A circle is drawn, and the touch
	 * coordinates are stored using this function.
	 *
	 * The <i>xCoord</i> and <i>yCoord</i> parameters that are passed in are in terms of the video
	 * size, not the screen size (which can be different). The maths is done in the drawCircle() function
	 * in CameraPreview
	 *
 	 *
	 * @param dataFile The file to append data to
	 * @param timeStamp The time of touch (in terms of record time)
	 * @param xCoord The x-coordinate of the touch
	 * @param yCoord The y-coordinate of the touch
	 *
	 * @see CameraPreview
	 */
	public static void appendToFile(@NonNull File dataFile, int frameCount, String timeStamp, float xCoord, float yCoord)
	{
		try{
			if(!dataFile.exists() && !dataFile.createNewFile()) {
				return;
			}
		}
		catch(IOException e){
			Log.e(TAG, "appendToFile", e);
		}

		try {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(dataFile.toString(), true);
				byte[] buffer = String.format(Locale.UK, "framestamp: %d\n\ttimestamp: %s\n\tx: %d\n\ty: %d\n",
					frameCount, timeStamp, (int) xCoord, (int) yCoord).getBytes();
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
}
