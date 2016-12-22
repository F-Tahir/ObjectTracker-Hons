package uk.ac.ed.faizan.objecttracker;


import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.util.Locale;

import static android.content.ContentValues.TAG;


/* Helper methods to start and stop the storage space update */
public class StorageSpace {

	Handler customHandler;
	private TextView storageSpace;
	private static final String TAG = StorageSpace.class.getName();

	public StorageSpace(TextView storageSpace) {
		customHandler = new Handler();
		this.storageSpace = storageSpace;
	}

	public Runnable updateStorageSpaceThread = new Runnable() {

		public void run() {

			// This time is shown in the camera app (excludes milliseconds)
			storageSpace.setText(String.format(Locale.ENGLISH, "Free Space: %.2f GB",
				Utilities.getAvailableSpaceInGB()));

			// Update thread every 5 seconds
			customHandler.postDelayed(this, 5000);

		}
	};
}
