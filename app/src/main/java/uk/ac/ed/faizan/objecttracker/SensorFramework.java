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
	private Sensor mGravity = null;

	private Context mContext = null;
	private File mDataFile;
	private CameraPreview mCameraPreview;
	private float[] mLinearAccelValues = new float[3];
	private float[] mGravityValues = new float[3]; // Used to create a low-pass filter to isolate gravity force
	private float[] mGyroValues = new float[3];

	private boolean initialGravityReading = false;



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
	 * Create a SensorFramework object, used in DebugActivity (no CasmeraPreview object)
	 *
	 * @see CameraPreview
	 */
	public SensorFramework(Context context) {
		mContext = context;

		if (mContext != null) {
			mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		}
	}

	public SensorManager getSensorManager() {
		return mSensorManager;
	}

	/**
	 * This getter method is used to access the latest accelerometer readings.
	 * @return A 3-dimensional array containing the x, y and z components of the accelerometer readings
	 */
	public float[] getLinearAccelValues() {
		return mLinearAccelValues;
	}


	/**
	 * This getter method is used to access the latest gyroscope readings.
	 * @return A 3-dimensional array containing the x, y and z components of the gyroscope readings
	 */
	public float[] getGyroValues() {
		return mGyroValues;
	}

	/**
	 * This getter method is used to access the latest gravity readings.
	 * @return A 3-dimensional array containing the x, y and z components of the gravity readings
	 */
	public float[] getGravityValues() {
		return mGravityValues;
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
	public void setAccelerometer() {
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}



	/**
	 * This method is used to set the mGyroscope member variable to the devices accelerometer.
	 * Because this method requires the mSensorManager member variable to be non-null, the SensorFramework
	 * constructor must be invoked before this method is called.
	 *
	 * hasGyroscope() should always be called before this method.
	 */
	public void setGyroscope() {
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	}



	/**
	 * This method is used to set the mGravity member variable to the devices accelerometer.
	 * Because this method requires the mSensorManager member variable to be non-null, the SensorFramework
	 * constructor must be invoked before this method is called.
	 *
	 * hasGyroscope() should always be called before this method.
	 */
	public void setGravity() {
		mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
	}


	public Sensor getAccelerometer() {
		return mAccelerometer;
	}


	public Sensor getGyroscope() {
		return mGyroscope;
	}

	public Sensor getGravitySensor() {
		return mGravity;
	}

	/**
	 * Call this method before attempting to poll an accelerometer - as the device may not contain one.
	 *
	 * @return True if device has an accelerometer, false otherwise.
	 */
	public boolean hasAccelerometer() {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
	}


	/**
	 * Call this method before attempting to poll a gyroscope - as the device may not contain one.
	 *
	 * @return True if device has an gyroscope, false otherwise.
	 */
	public boolean hasGyroscope() {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
	}


	/**
	 * Call this method before attempting to poll a gravity sensor - as the device may not contain one.
	 *
	 * @return True if device has a gravity sensor, false otherwise.
	 */
	public boolean hasGravitySensor() {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null;
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

		if (hasAccelerometer() && hasGyroscope() && hasGravitySensor()) {
			setAccelerometer();
			setGyroscope();
			setGravity();

			Log.i(TAG, "Listeners have been set");


			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
			mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
			mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_FASTEST);

		} else {
			Log.i(TAG, "Attempting to register listener, but device does not have an accelerometer, " +
				"gravity sensor, or gyroscope.");
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
		initialGravityReading = false;
	}


	@Override
	public void onSensorChanged(SensorEvent event) {

		Sensor sensor = event.sensor;


		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && initialGravityReading) {
			mLinearAccelValues[0] = event.values[0] - mGravityValues[0];
			mLinearAccelValues[1] = event.values[1] - mGravityValues[1];
			mLinearAccelValues[2] = event.values[2] - mGravityValues[2];
			Log.i(TAG, "Linear acceleration values set");

		} else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			mGyroValues = event.values;

		} else if (sensor.getType() == Sensor.TYPE_GRAVITY) {
			Log.i(TAG, "Gravity values set");
			mGravityValues = event.values;
			initialGravityReading = true;
		}


	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Not in use
	}



}
