package uk.ac.ed.faizan.objecttracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;


public class DebugActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

	private SensorFramework mSensorFramework;
	private boolean debuggingInProcess = false;
	private int numberOfIterations;
	private int currentIteration;
	private File mDumpFile;
	private final static String TAG = DebugActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);

		findViewById(R.id.start_debug).setOnClickListener(this);
		mSensorFramework = new SensorFramework(this);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			// User wishes to start or stop debugging.
			case R.id.start_debug:

				RadioButton accelButton = (RadioButton) findViewById(R.id.accelerometer_button);
				RadioButton gyroButton = (RadioButton) findViewById(R.id.gyroscope_button);
				RadioButton gravButton = (RadioButton) findViewById(R.id.gravity_button);
				EditText numIterations = (EditText) findViewById(R.id.number_of_times);
				numberOfIterations = Integer.parseInt(numIterations.getText().toString());


				// Debugging not in process. Need to check which sensor user wishes to debug,
				// and how many iterations, then register the listener for the appropriate sensor.
				if (!debuggingInProcess) {

					debuggingInProcess = true;
					currentIteration = 1;
					mDumpFile = Utilities.getDebugFile(System.currentTimeMillis());

					// User wants to debug accelerometer
					if (accelButton.isChecked()) {
						if (mSensorFramework.hasAccelerometer()) {

							appendToDataFile(mDumpFile, 0, numberOfIterations);
							mSensorFramework.setAccelerometer();
							mSensorFramework.getSensorManager().registerListener(this, mSensorFramework.getAccelerometer(),
								SensorManager.SENSOR_DELAY_FASTEST);

						} else {
							Toast.makeText(this, "Device does not have accelerometer!", Toast.LENGTH_LONG).show();
						}

						// User wants to debug gyroscope
					} else if (gyroButton.isChecked()) {
						if (mSensorFramework.hasGyroscope()) {

							appendToDataFile(mDumpFile, 1, numberOfIterations);
							mSensorFramework.setGyroscope();
							mSensorFramework.getSensorManager().registerListener(this, mSensorFramework.getGyroscope(),
								SensorManager.SENSOR_DELAY_FASTEST);

						} else {
							Toast.makeText(this, "Device does not have gyroscope!", Toast.LENGTH_LONG).show();
						}

						// User wants to debug gravity sensor
					} else if (gravButton.isChecked()) {
						if (mSensorFramework.hasGravitySensor()) {

							appendToDataFile(mDumpFile, 2, numberOfIterations);
							mSensorFramework.setGravity();
							mSensorFramework.getSensorManager().registerListener(this, mSensorFramework.getGravitySensor(),
								SensorManager.SENSOR_DELAY_FASTEST);

						} else {
							Toast.makeText(this, "Device does not have gravity sensor!", Toast.LENGTH_LONG).show();
						}
					}

					((Button) v).setText("Stop Polling");

					// Debugging already in process, stop listening to sensor (even if we haven't reached
					// desired # of iterations)
				} else {
					Log.i(TAG, "Polling stopped");
					debuggingInProcess = false;
					mSensorFramework.getSensorManager().unregisterListener(this);
					Toast.makeText(this, "Polling finished at " + (currentIteration-1) + " iterations. Sensor " +
						"dump saved to " + mDumpFile.toString(), Toast.LENGTH_LONG).show();
					((Button) v).setText("Start Polling");
				}


			default:
				break;


		}
	}


	@Override
	public void onSensorChanged(SensorEvent event) {

		// Desired number of iterations reached - stop appending data to file
		if (currentIteration == numberOfIterations+1) {
			Toast.makeText(this, "Polling finished. Sensor dump saved to " + mDumpFile.toString(), Toast.LENGTH_LONG).show();
			mSensorFramework.getSensorManager().unregisterListener(this);
			debuggingInProcess = false;
			currentIteration = 1;

			// Change UI to show progress, as well as appending new data to the sensor dump file
		} else {
			((TextView) findViewById(R.id.progress)).setText(String.format(Locale.ENGLISH, "Progress:" +
				" %d/%d", currentIteration, numberOfIterations));
			appendToDataFile(mDumpFile, event.values, currentIteration);
		}

		currentIteration += 1;


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
	 */
	public static void appendToDataFile(@NonNull File dataFile, int sensor, int numIterations) {

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
				byte[] buffer = String.format(Locale.ENGLISH, "# General Debug Information\n\n" +
					"sensor: %s\nnumber_of_iterations: %d\n\n", sensorType, numIterations).getBytes();

				fos.write(buffer);
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
	public static void appendToDataFile(@NonNull File dataFile, float[] values, int iteration) {


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
				byte[] buffer = String.format(Locale.ENGLISH, "iteration: %d\n\tx: %f\n\ty: %f\n\tz: %f" +
					"\n\n", iteration, values[0], values[1], values[2]).getBytes();

				fos.write(buffer);
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




