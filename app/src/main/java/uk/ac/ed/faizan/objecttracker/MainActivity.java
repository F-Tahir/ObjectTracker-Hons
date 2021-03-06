package uk.ac.ed.faizan.objecttracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.List;



public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

	public final String TAG = MainActivity.class.getSimpleName();
	public Context mContext;
	private static final int REQUEST_PERMISSIONS = 1;
	private String[] permissionList = {
		Manifest.permission.WRITE_EXTERNAL_STORAGE,
		Manifest.permission.RECORD_AUDIO,
		Manifest.permission.CAMERA};



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
		mContext = this;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		if (requestCode == REQUEST_PERMISSIONS) {


			// At least one required permission not granted, show toast and don't start intent
			if (!Utilities.allPermissionsGranted(grantResults)) {

				Toast.makeText(this, "This app requires camera, audio and storage permissions to start tracking.",
					Toast.LENGTH_LONG).show();

				// All required permissions granted, start intent
			} else {
				Intent i = new Intent(this, TrackingActivity.class);
				startActivity(i);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.actionbar_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();

		if (id == R.id.debug_mode) {
			startActivity(new Intent(this, DebugActivity.class));
		} else if (id == R.id.about) {
			startActivity(new Intent(this, AboutActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}




	@Override
	public void onClick(View view)
	{

		switch (view.getId()) {
			case R.id.start_tracking_button:

				// Request runtime permissions on devices >= API 23, before starting tracking activity
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

					// At least one required permission not granted, request it.
					if(!Utilities.hasPermissions(this, permissionList)){
						ActivityCompat.requestPermissions(this, permissionList, REQUEST_PERMISSIONS);

						// We have all required permissions
					} else {
						Intent i = new Intent(this, TrackingActivity.class);
						startActivity(i);
					}

					// API < 23, no runtime permission check needed
				} else {
					Intent i = new Intent(this, TrackingActivity.class);
					startActivity(i);
				}

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

					Snackbar snackbar = Snackbar.make(view, "No file explorer installed. Install" +
						" ES File Explorer.",
						Snackbar.LENGTH_LONG);
					snackbar.setAction("INSTALL", new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							openFileExplorerApp(mContext);
						}
					});
					snackbar.show();

//					Toast.makeText(this, "Please install a File Manager application. ES File Explorer " +
//						"works well with this app.", Toast.LENGTH_LONG)
//						.show();
				}
				break;

			case R.id.preferences_button:
				startActivity(new Intent(this, SettingsActivity.class));
				break;


			default:
				break;
		}
	}


	public static void openFileExplorerApp(Context context) {
		// you can also use BuildConfig.APPLICATION_ID
		String appId = context.getPackageName();
		Intent rateIntent = new Intent(Intent.ACTION_VIEW,
			Uri.parse("market://details?id=" + "com.estrongs.android.pop"));
		boolean marketFound = false;

		// find all applications able to handle our rateIntent
		final List<ResolveInfo> otherApps = context.getPackageManager()
			.queryIntentActivities(rateIntent, 0);
		for (ResolveInfo otherApp: otherApps) {
			// look for Google Play application
			if (otherApp.activityInfo.applicationInfo.packageName
				.equals("com.android.vending")) {

				ActivityInfo otherAppActivity = otherApp.activityInfo;
				ComponentName componentName = new ComponentName(
					otherAppActivity.applicationInfo.packageName,
					otherAppActivity.name
				);
				// make sure it does NOT open in the stack of your activity
				rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// task reparenting if needed
				rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				// if the Google Play was already open in a search result
				//  this make sure it still go to the app page you requested
				rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				// this make sure only the Google Play app is allowed to
				// intercept the intent
				rateIntent.setComponent(componentName);
				context.startActivity(rateIntent);
				marketFound = true;
				break;

			}
		}

		// if GP not present on device, open web browser
		if (!marketFound) {
			Intent webIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://play.google.com/store/apps/details?id="+appId));
			context.startActivity(webIntent);
		}
	}

}
