package uk.ac.ed.faizan.objecttracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;



public class DebugActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, AdapterView.OnItemSelectedListener {

	private SensorFramework mSensorFramework;
	private boolean debuggingInProcess = false;
	private int mPollDuration;
	private long mStartTime;
	private long mEndTime;
	private int mCurrentIteration;
	private int mPollingFrequency;
	private String mPollingFrequencyString;
	private File mDumpFile;
	private Spinner mSpinner;
	private int mMeasurementFlag; // 0 if polling for certain time, 1 if polling for certain iteration


	private final static String TAG = DebugActivity.class.getSimpleName();



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);

		findViewById(R.id.start_debug).setOnClickListener(this);

		mSpinner = (Spinner) findViewById(R.id.polling_frequency);
		// Create an ArrayAdapter using the string array and a default mSpinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
			R.array.debug_polling_frequency_entries, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);
		mSpinner.setOnItemSelectedListener(this);

		mPollingFrequency = SensorManager.SENSOR_DELAY_FASTEST; // Default value
		mSensorFramework = new SensorFramework(this);

		// Add "back" button to ActionBar
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// Show the Up button in the action bar.
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onPause() {
		if (debuggingInProcess) {
			debuggingInProcess = false;
			mSensorFramework.getSensorManager().unregisterListener(this);
			Toast.makeText(this, "Polling finished at " + (mCurrentIteration -1) + " iterations. Sensor " +
				"dump saved to " + mDumpFile.toString(), Toast.LENGTH_LONG).show();
			((Button) findViewById(R.id.start_debug)).setText("Start Polling");
		}
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

		if (position == 0) {
			Log.i(TAG, "Fastest");
			findViewById(R.id.userdefined_polling).setVisibility(View.INVISIBLE);
			mPollingFrequency = SensorManager.SENSOR_DELAY_FASTEST;
			mPollingFrequencyString = "fastest (predefined)";

		} else if (position == 1) {
			Log.i(TAG, "Normal");
			findViewById(R.id.userdefined_polling).setVisibility(View.INVISIBLE);
			mPollingFrequency = SensorManager.SENSOR_DELAY_NORMAL;
			mPollingFrequencyString = "normal (predefined)";

		} else if (position == 2) {
			Log.i(TAG, "Game");
			findViewById(R.id.userdefined_polling).setVisibility(View.INVISIBLE);
			mPollingFrequency = SensorManager.SENSOR_DELAY_GAME;
			mPollingFrequencyString = "game (predefined)";

		} else if (position == 3) {
			Log.i(TAG, "UI");
			findViewById(R.id.userdefined_polling).setVisibility(View.INVISIBLE);
			mPollingFrequency = SensorManager.SENSOR_DELAY_UI;
			mPollingFrequencyString = "ui (predefined)";

		} else if (position == 4) {
			Log.i(TAG, "user-defined");
			findViewById(R.id.userdefined_polling).setVisibility(View.VISIBLE);
			// mPollingFrequency and mPollingFrequency for user-defined is set when user starts polling.
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Do nothing
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			// User wishes to start or stop debugging.
			case R.id.start_debug:

				RadioButton accelButton = (RadioButton) findViewById(R.id.accelerometer_button);
				RadioButton gyroButton = (RadioButton) findViewById(R.id.gyroscope_button);
				RadioButton gravButton = (RadioButton) findViewById(R.id.gravity_button);
				RadioButton timeButton = (RadioButton) findViewById(R.id.time);
				EditText duration = (EditText) findViewById(R.id.number_of_times);


				// Debugging not in process. Need to check which sensor user wishes to debug,
				// and how many iterations, then register the listener for the appropriate sensor.
				if (!debuggingInProcess) {

					mPollDuration = Integer.parseInt(duration.getText().toString());

					// User-defined time
					if (mSpinner.getSelectedItemPosition() == 4) {
						// Defined in milliseconds, so convert to microseconds
						mPollingFrequency = Integer.parseInt(((EditText) findViewById(R.id.userdefined_polling)).getText().toString())*1000;
						mPollingFrequencyString = "user-defined (" + mPollingFrequency + " microseconds) ";
					}

					Log.i(TAG, mPollingFrequencyString);

					debuggingInProcess = true;
					mCurrentIteration = 1;
					mDumpFile = Utilities.getDebugFile(System.currentTimeMillis());

					// User wants to debug accelerometer
					if (accelButton.isChecked()) {
						if (mSensorFramework.hasAccelerometer()) {

							appendToDataFile(mDumpFile, 0, mPollDuration, mPollingFrequencyString);
							mSensorFramework.setAccelerometer();
							mSensorFramework.getSensorManager().registerListener(this, mSensorFramework.getAccelerometer(),
								mPollingFrequency);

						} else {
							Toast.makeText(this, "Device does not have accelerometer!", Toast.LENGTH_LONG).show();
						}

						// User wants to debug gyroscope
					} else if (gyroButton.isChecked()) {
						if (mSensorFramework.hasGyroscope()) {

							appendToDataFile(mDumpFile, 1, mPollDuration, mPollingFrequencyString);
							mSensorFramework.setGyroscope();
							mSensorFramework.getSensorManager().registerListener(this, mSensorFramework.getGyroscope(),
								mPollingFrequency);

						} else {
							Toast.makeText(this, "Device does not have gyroscope!", Toast.LENGTH_LONG).show();
						}

						// User wants to debug gravity sensor
					} else if (gravButton.isChecked()) {
						if (mSensorFramework.hasGravitySensor()) {

							appendToDataFile(mDumpFile, 2, mPollDuration, mPollingFrequencyString);
							mSensorFramework.setGravity();
							mSensorFramework.getSensorManager().registerListener(this, mSensorFramework.getGravitySensor(),
								mPollingFrequency);

						} else {
							Toast.makeText(this, "Device does not have gravity sensor!", Toast.LENGTH_LONG).show();
						}
					}

					if (timeButton.isChecked()) {
						mMeasurementFlag = 0;
						mStartTime = System.currentTimeMillis();
						mEndTime = (mPollDuration * 1000) + mStartTime; // extra 20ms for processing
						Log.i("Polling duration: " + mPollDuration, TAG);
						Log.i("Start time: "+ mStartTime, TAG);
						Log.i("End time: "+ mEndTime, TAG);
					} else {
						mMeasurementFlag = 1;
					}

					((Button) v).setText("Stop Polling");

					// Debugging already in process, stop listening to sensor (even if we haven't reached
					// desired # of iterations)
				} else {
					Log.i(TAG, "Polling stopped");
					debuggingInProcess = false;
					mSensorFramework.getSensorManager().unregisterListener(this);
					Toast.makeText(this, "Polling finished at " + (mCurrentIteration -1) + " iterations. Sensor " +
						"dump saved to " + mDumpFile.toString(), Toast.LENGTH_LONG).show();
					((Button) v).setText("Start Polling");
				}


			default:
				break;


		}
	}




	@Override
	public void onSensorChanged(SensorEvent event) {


		// Measuring in terms of a certain number of iterations
		if (mMeasurementFlag == 1) {
			if (mCurrentIteration == mPollDuration + 1) {
				Toast.makeText(this, "Polling finished. Sensor dump saved to " + mDumpFile.toString(), Toast.LENGTH_LONG).show();
				mSensorFramework.getSensorManager().unregisterListener(this);
				((Button) findViewById(R.id.start_debug)).setText("Start Polling");
				debuggingInProcess = false;
				mCurrentIteration = 1;

				// Change UI to show progress, as well as appending new data to the sensor dump file
			} else {
				((TextView) findViewById(R.id.progress)).setText(String.format(Locale.ENGLISH, "Progress:" +
					" %d/%d", mCurrentIteration, mPollDuration));
				appendToDataFile(mDumpFile, event.values, mCurrentIteration);
			}
		} else {
			Log.i("ere", TAG);
			if (System.currentTimeMillis() > mEndTime) {
				Toast.makeText(this, "Polling finished. Sensor dump saved to " + mDumpFile.toString(), Toast.LENGTH_LONG).show();
				mSensorFramework.getSensorManager().unregisterListener(this);
				((Button) findViewById(R.id.start_debug)).setText("Start Polling");
				debuggingInProcess = false;
				mCurrentIteration = 1;

				// Change UI to show progress, as well as appending new data to the sensor dump file
			} else {
				((TextView) findViewById(R.id.progress)).setText(String.format(Locale.ENGLISH, "Number of polls:" +
					" %d", mCurrentIteration));
				appendToDataFile(mDumpFile, event.values, mCurrentIteration);
			}
		}

		mCurrentIteration += 1;


	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do nothing
	}


	/**
	 * This function appends basic debug information (such as type of sensor being debugged. and number of iterations)
	 * to the start of the dump file.
	 *
	 * @param dataFile File to append to
	 * @param sensor Sensor being debugged
	 * @param numIterations Number of times to poll the sensor
	 * @param pollingFrequencyString The string representing the sensor polling frequency (predefined or user-defined)
	 */
	public void appendToDataFile(@NonNull File dataFile, int sensor, int numIterations, String pollingFrequencyString) {

		String sensorType;
		if (sensor == 0) {
			sensorType = "accelerometer";
		} else if (sensor == 1) {
			sensorType = "gyroscope";
		} else if (sensor == 2) {
			sensorType = "gravity";
		} else {
			sensorType = "unknown";
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
				if (mMeasurementFlag == 1) {
					byte[] buffer = String.format(Locale.ENGLISH, "# General Debug Information\n\n" +
							"sensor: %s\nnumber_of_iterations: %d \npolling_frequency: %s\n\n", sensorType,
						numIterations, pollingFrequencyString).getBytes();
					fos.write(buffer);
				} else {
					byte[] buffer = String.format(Locale.ENGLISH, "# General Debug Information\n\n" +
							"sensor: %s\ntime_to_poll_for: %d \npolling_frequency: %s\n\n", sensorType,
						mPollDuration, pollingFrequencyString).getBytes();
					fos.write(buffer);
				}

				fos.flush();

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
	 * This function appends basic debug information (such as type of sensor being debugged. and number of iterations)
	 * to the start of the dump file.
	 *
	 * @param dataFile File to append to
	 * @param values Values from the sensor being debugged
	 * @param iteration Current poll iteration
	 */
	public void appendToDataFile(@NonNull File dataFile, float[] values, int iteration) {


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

				if (mMeasurementFlag == 1) {
					byte[] buffer = String.format(Locale.ENGLISH, "iteration: %d\n\tx: %f\n\ty: %f\n\tz: %f" +
						"\n\n", iteration, values[0], values[1], values[2]).getBytes();
					fos.write(buffer);

				} else {
					byte[] buffer = String.format(Locale.ENGLISH, "time: %d\n\tx: %f\n\ty: %f\n\tz: %f" +
						"\n\n", System.currentTimeMillis() - mStartTime, values[0], values[1], values[2]).getBytes();
					fos.write(buffer);
				}
				fos.flush();

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




