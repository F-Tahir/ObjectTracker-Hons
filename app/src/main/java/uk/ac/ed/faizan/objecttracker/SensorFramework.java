package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.io.File;
import java.util.Arrays;


public class SensorFramework implements SensorEventListener {

	private static String TAG = SensorFramework.class.getSimpleName();
	private SensorManager mSensorManager;
	private Sensor mAccelerometer = null;
	private Sensor mGyroscope = null;
	private Context mContext = null;
	private File mDataFile;
	private CameraPreview mCameraPreview;
	private static float[] accelValues = new float[3];
	private static float[] gyroValues = new float[3];



	/**
	 * Create a SensorFramework object, passing in the context that this constructor was called from.
	 * Also instantiate mSensorManager.
	 *
	 * This constructor is invoked only in CameraPreview's constructor.
	 *
	 * @see CameraPreview
	 */
	public SensorFramework(Context context, CameraPreview cameraPreview) {
		mContext = context;
		mCameraPreview = cameraPreview;

		if (mContext != null) {
			mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		}
	}

	/**
	 * This getter method is used to access the latest accelerometer readings.
	 * @return A 3-dimensional array containing the x, y and z components of the accelerometer readings
	 */
	public float[] getAccelValues() {
		return accelValues;
	}


	/**
	 * This getter method is used to access the latest gyroscope readings.
	 * @return A 3-dimensional array containing the x, y and z components of the gyroscope readings
	 */
	public float[] getGyroValues() {
		return gyroValues;
	}


	/**
	 * This is a setter method used to fill all the accelerometer sensor values to 0. Filling it
	 * to 0 simply means that there is no change from accelerometer readings, and will allow us
	 * to calculate camera trajectories more accurately. This is called each time the sensor is polled
	 * and accelValues is filled in - we then record the accelerometer readings into a yml file, and
	 * then reset the values. This means that if the sensor readings do not change between frames,
	 * then a value of 0 is recorded, as opposed to the last known reading, as this will make
	 * calculations inaccurate.
	 */
	public void setAccelValues() {
		Arrays.fill(accelValues, 0);
	}


	/**
	 * This is a setter method used to fill all the accelerometer sensor values to 0. Filling it
	 * to 0 simply means that there is no change from accelerometer readings, and will allow us
	 * to calculate camera trajectories more accurately. This is called each time the sensor is polled
	 * and accelValues is filled in - we then record the accelerometer readings into a yml file, and
	 * then reset the values. This means that if the sensor readings do not change between frames,
	 * then a value of 0 is recorded, as opposed to the last known reading, as this will make
	 * calculations inaccurate.
	 */
	public void setGryoValues() {
		Arrays.fill(gyroValues, 0);
	}

	/**
	 * This method is used to set the mAccelerometer member variable to the devices accelerometer.
	 * Because this method requires the mSensorManager member variable to be non-null, the SensorFramework
	 * constructor must be invoked before this method is called.
	 *
	 * hasAccelerometer() should always be called before this method.
	 *
	 * @
	 */
	private void setAccelerometer() {
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}


	/**
	 * This method is used to set the mGyroscope member variable to the devices accelerometer.
	 * Because this method requires the mSensorManager member variable to be non-null, the SensorFramework
	 * constructor must be invoked before this method is called.
	 *
	 * hasGyroscope() should always be called before this method.
	 */
	private void setGyroscope() {
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	}

	/**
	 * Call this method before attempting to poll an accelerometer - as the device may not contain one.
	 *
	 * @return True if device has an accelerometer, false otherwise.
	 */
	private boolean hasAccelerometer() {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
	}


	/**
	 * Call this method before attempting to poll a gyroscope - as the device may not contain one.
	 *
	 * @return True if device has an gyroscope, false otherwise.
	 */
	private boolean hasGyroscope() {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
	}


	/**
	 * This method is called to register the accelerometer and gyroscope's listeners. The method
	 * ensures that the device does contain the sensors, and if so, call the setAccelerometer()/setGyroscope()
	 * methods which instantiate the member variables. The sensors are then registered for callback info.
	 * This method is also responsible for setting the data file which the sensor data is recorded to.
	 *
	 * This method is called in CameraPreview.prepareVideoRecorder();
	 * @see CameraPreview
	 *
	 */
	public void setListeners(File file) {

		mDataFile = file;

		Log.i(TAG, "Listener has been set");

		if (hasAccelerometer() && hasGyroscope()) {
			setAccelerometer();
			setGyroscope();

			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
			mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

		} else {
			Log.i(TAG, "Attempting to register listener, but device does not have an accelerometer " +
				"or gyroscope.");
		}

	}


	/**
	 * Called after recording is stopped. Sensor data only needs to be accessed during active recording.
	 * This is called in CameraPreview.releaseMediaRecorder()
	 *
	 * @see CameraPreview
	 */
	public void unsetListeners() {
		Log.i(TAG, "Listener has been unset");
		mSensorManager.unregisterListener(this);
	}


	@Override
	public void onSensorChanged(SensorEvent event) {

		Sensor sensor = event.sensor;


		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			Log.i(TAG, "Accelerometer readings changed");
			accelValues = event.values;
		} else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			Log.i(TAG, "Gyroscope readings changed");
			gyroValues = event.values;
		}


	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Not in use
	}



}
