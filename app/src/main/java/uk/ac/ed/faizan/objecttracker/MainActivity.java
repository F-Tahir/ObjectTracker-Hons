package uk.ac.ed.faizan.objecttracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

	private String TAG = "object:tracker";


	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
	{
		@Override
		public void onManagerConnected(int status)
		{
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
					Log.i(TAG, "OpenCV Manager Connected");
					// Can make OpenCV API calls from now on
					break;
				case LoaderCallbackInterface.INIT_FAILED:
					Log.i(TAG, "Init Failed");
					break;
				case LoaderCallbackInterface.INSTALL_CANCELED:
					Log.i(TAG, "Install Cancelled");
					break;
				case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
					Log.i(TAG, "Incompatible Version");
					break;
				case LoaderCallbackInterface.MARKET_ERROR:
					Log.i(TAG, "Market Error");
					break;
				default:
					Log.i(TAG, "OpenCV Manager Install");
					super.onManagerConnected(status);
					break;
			}
		}
	};

	@Override
	protected void onResume()
	{
		super.onResume();

		//initialize OpenCV manager
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Find views in activity_main.xml identified by the ID specified in the parameters
		Button startTrackingButton = (Button) findViewById(R.id.start_tracking_button);
		Button viewRecordingsButton = (Button) findViewById(R.id.view_recordings_button);
		Button preferencesButton = (Button) findViewById(R.id.preferences_button);

        /* Attach an onClickListener to each of the 3 buttons to detect clicks and carry out
		relevant actions
         */
		startTrackingButton.setOnClickListener(this);
		viewRecordingsButton.setOnClickListener(this);
		preferencesButton.setOnClickListener(this);
	}



	@Override
	public void onClick(View view)
	{

		switch (view.getId()) {
			case R.id.start_tracking_button:
				Intent i = new Intent(this, TrackingActivity.class);
				startActivity(i);
				break;

			case R.id.view_recordings_button:

				// If the device has a file explorer app installed, then check if the folder exists.
				// If not, show a toast, and if it does, open the folder with relevant file explorer app.
				Uri videoFolderURI = Uri.parse(Environment.getExternalStorageDirectory() + "/ObjectTracker/");
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(videoFolderURI, "resource/folder");

				if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
					File videoFolder = new File(videoFolderURI.toString());
					if (!videoFolder.exists()) {
						Toast.makeText(this, "There are no recordings to view.", Toast.LENGTH_LONG).show();
					} else {
						startActivity(intent);
					}

					// If user has no file manager app, notify the user to install one.
				} else {
					Toast.makeText(this, "Please install a File Manager application", Toast.LENGTH_LONG)
						.show();
				}
				break;
			case R.id.preferences_button:
				// Start an intent to open the preferences window (may be removed)
				break;
			default:
				break;
		}
	}

}