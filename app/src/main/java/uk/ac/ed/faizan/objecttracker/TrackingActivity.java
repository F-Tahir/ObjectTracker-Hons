package uk.ac.ed.faizan.objecttracker;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

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


    private final static String TAG = "cameraPreview"; // For debugging purposes

    private TangoCameraPreview mTangoCameraPreview;
    private Tango mTango;
    private boolean mTangoIsConnected = false;
    private boolean surfacePreviewFrozen = false;

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

                // Check if the frame available is for the camera to update. onFrameAvailable() checks
                // both the fisheye and the color camera, so must specify which camera we want to check
                // for a frame change.
                // If user has frozen the camera by pressing "Freeze Camera" button, this method will
                // not listen for camera frame updates.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR && !surfacePreviewFrozen) {
                    mTangoCameraPreview.onFrameAvailable();
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

    /* This method is called when the flash icon is clicked.
    A popup menu is presented to select On, Off, or Auto.
     */
    public void showFlashPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.flash_menu);

        // Set "Auto" as the selected item as this is default
        popup.getMenu().getItem(0).setChecked(true);

        popup.show();
    }

    /* This method is executed when the (Un)freeze Camera button in the camera UI is clicked.
    * The boolean surfacePreviewFrozen is changed to True if user is freezing, and False if user is
    * unfreezing. If the boolean is true, then the onFrameAvailable() listener will not update
    * the surface preview, so the user can easily select a template when preview is frozen.
    *
    * To-do: perhaps only show the Freeze button if automatic tracking is enabled.*/
    public void freezeSurfacePreview(View v) {

        Button freezeButton = (Button) v;

        // Executed if user wishes to freeze the surface preview
        if (!surfacePreviewFrozen) {
            surfacePreviewFrozen = true;
            freezeButton.setText(R.string.unfreeze_camera);
        // Executed if user wishes to unfreeze surface preview
        } else {
            surfacePreviewFrozen = false;
            freezeButton.setText(R.string.freeze_camera);
        }
    }



}
