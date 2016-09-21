package uk.ac.ed.faizan.objecttracker;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import java.util.ArrayList;

public class TrackingActivity extends Activity {


    final static String TAG = "cameraPreview"; // For debugging purposes
    TangoCameraPreview mTangoCameraPreview;
    Tango mTango;
    boolean mTangoIsConnected;

    // Inflate the layout and set the camera view when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Initialize the Tango's camera view to the TangoCameraPreview defined in activity_tracking.xml
        mTangoCameraPreview = (TangoCameraPreview) findViewById(R.id.camera_preview);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        decorView.setSystemUiVisibility(uiOptions );
    }

    // Called after onCreate() in an Android activity lifecycle.
    @Override
    protected void onResume() {
        Log.i(TAG, "In resume");
        super.onResume();

        // Initialize Tango as a normal service.
        // Because we call mTango.disconnect() in onPause, the Tango service will unbind each
        // time the activity is obscured, so we must check to see if it needs binded again each time
        // onResume is called.
        if (!mTangoIsConnected) {
            // ***********New thread possibly not needed here.*********
            mTango = new Tango(TrackingActivity.this, new Runnable() {

                @Override
                public void run() {
                    try {
                        startTrackingPreview();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, "Tango out-of-date exception");
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Tango error exception");
                    }
                }
            });

        }
    }

    // Called when activity becomes obscured.
    @Override
    protected void onPause() {
        // If the camera is connected, then disconnect the camera and unbind the Tango service
        super.onPause();
        if (mTangoIsConnected) {
            mTango.disconnect();
            mTangoCameraPreview.disconnectFromTangoCamera();
            mTangoIsConnected = false;
        }
    }

    // Called within the onResume() method, and is responsible for connecting to the camera
    // and binding the Tango service.
    public void startTrackingPreview() {

        // Connect to the Tango Color (RGB) camera
        mTangoCameraPreview.connectToTangoCamera(mTango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
        // Bind the Tango service with default settings
        mTango.connect(mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT));
        mTangoIsConnected = true;

        // No need to add any coordinate frame pairs since we are not using
        // pose data. So just initialize.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // We are not using OnPoseAvailable for this app
            }

            @Override
            public void onFrameAvailable(int cameraId) {

                // Check if the frame available is for the camera we want and
                // update its frame on the camera preview.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    mTangoCameraPreview.onFrameAvailable();
                    // Can also add OpenCV code here.
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                // We are not using OnPoseAvailable for this app
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                // We are not using OnPoseAvailable for this app
            }
        });
    }



}
