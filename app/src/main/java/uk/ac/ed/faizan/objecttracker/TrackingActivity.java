package uk.ac.ed.faizan.objecttracker;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
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

import com.flask.colorpicker.ColorPickerView;

public class TrackingActivity extends Activity implements View.OnClickListener{


    private final static String TAG = "cameraPreview"; // For debugging purposes

    private TangoCameraPreview mTangoCameraPreview;
    private Tango mTango;
    private boolean mTangoIsConnected = false;


    private boolean isPreviewFrozen = false;
    private boolean isRecording = false;
    private boolean mIsPaused = false;

    // Default color for overlay color when tracking objects
    private int overlayColor = 0xffff0000;

    // Inflate the layout and set the camera view when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupScreen();

        // Inflate laoyut
        setContentView(R.layout.activity_tracking);

        // Initialize the Tango's camera view to the TangoCameraPreview defined in activity_tracking.xml
        mTangoCameraPreview = (TangoCameraPreview) findViewById(R.id.camera_preview);


        // Register and set onClickListeners for various views
        Button freezeButton =  (Button) findViewById(R.id.freeze_button);
        Button colorButton =  (Button) findViewById(R.id.select_color_button);
        ImageView flashButton = (ImageView) findViewById(R.id.flashlight_button);

        freezeButton.setOnClickListener(this);
        colorButton.setOnClickListener(this);
        flashButton.setOnClickListener(this);
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

    /* Called when activity is created, to make activity full screen, hide status bars, and ensure
    that screen never times out due to user settings.
     */
    public void setupScreen() {
        // Hide title and status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Make hardware (back, home, recent app) buttons low profile
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // Screen never times out
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        // Not using pose data so this is not used, but needs initialized for connectListener
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
                // for a frame change. Will not update is isPreviewFrozen = true (false by default)
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR && !isPreviewFrozen) {
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

    // View v refers to the widget that was clicked. A second method is then called depending on the
    // type of view clicked. For instance, if the "Freeze Camera" button was clicked, then
    // case R.id.freeze_button would be executed
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.freeze_button:
                Button freezeButton = (Button) v;
                freezeSurfacePreview(freezeButton);
                break;
            case R.id.flashlight_button:
                ImageView flashButton = (ImageView) v;
                showFlashPopupMenu(flashButton);
                break;
            case R.id.select_color_button:
                Button colorButton  = (Button) v;
                getColor(colorButton);
                break;
        }
    }

    /* This method is called when the flash icon is clicked.
     * A popup menu is presented to select On, Off, or Auto.
     */
    public void showFlashPopupMenu(ImageView button) {
        PopupMenu popup = new PopupMenu(this, button);
        popup.inflate(R.menu.flash_menu);


        // Set "Auto" as the selected item as this is default
        popup.getMenu().getItem(0).setChecked(true);

        popup.show();
    }


    /*
    * Called when (Un)freeze camera is clicked. Depending on current setting, camera preview is frozen
    * or unfrozen. Freezing the camera preview is useful for auto tracking when user needs to select
    * a template
    */
    public void freezeSurfacePreview(Button button) {

        // Executed if user wishes to freeze the surface preview
        if (!isPreviewFrozen) {
            isPreviewFrozen = true;
            button.setText(R.string.unfreeze_camera);
        // Executed if user wishes to unfreeze surface preview
        } else {
            isPreviewFrozen = false;
            button.setText(R.string.freeze_camera);
        }
    }

    /*
     * Create a dialog to select the color for tracking overlay (e.g. a bounding box).
     * Default color is red (#FF0000) as defined in global variable selectedColor above.
     */
    public void getColor(Button button) {
        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose Color")
                .initialColor(overlayColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)

                .setPositiveButton("Ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {

                        if (overlayColor != selectedColor) {
                            overlayColor = selectedColor;
                            Toast.makeText(TrackingActivity.this,
                                    "Color selection saved.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(TrackingActivity.this,
                                "Color selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                })
                .build()
                .show();
    }
}




