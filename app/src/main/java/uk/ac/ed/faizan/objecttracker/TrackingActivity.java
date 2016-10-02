package uk.ac.ed.faizan.objecttracker;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;


import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

public class TrackingActivity extends Activity implements View.OnClickListener {

    private CameraPreview cameraPreview;
    private final static String TAG = "object:tracker"; // For debugging purposes

    // Default color for overlay color when tracking objects (Red)
    static int overlayColor = 0xffff0000;

    // Inflate the layout and set the camera view when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up UI to make full screen, low profile soft-keys, etc.
        setupScreen();

        // Inflate layout
        setContentView(R.layout.activity_tracking);

        // Register and set onClickListeners for various views
        Button freezeButton =  (Button) findViewById(R.id.freeze_button);
        Button colorButton =  (Button) findViewById(R.id.select_color_button);
        ImageView flashButton = (ImageView) findViewById(R.id.flashlight_button);
        ImageView recordButton = (ImageView) findViewById(R.id.record_button);

        freezeButton.setOnClickListener(this);
        colorButton.setOnClickListener(this);
        flashButton.setOnClickListener(this);
        recordButton.setOnClickListener(this);
    }

    // Called when activity becomes obscured.
    @Override
    protected void onPause() {
        // If the camera is connected, then disconnect the camera and unbind the Tango service
        super.onPause();

        // If user presses home buttoh whilst recording, the recording will save and state set to normal.
        // All of this is handled in releaseMediaRecorder();
        cameraPreview.releaseMediaRecorder(); // release resources such as preview
        cameraPreview.releaseCamera(); // release the camera so that other applications can use it
    }

    // Called after onCreate() in an Android activity lifecycle.
    @Override
    protected void onResume() {
        Log.i(TAG, "In resume");
        super.onResume();

        // Pass in the timestamp widget and surfaceview into the cameraPreview construct.
        cameraPreview = new CameraPreview(
                this,
                (SurfaceView) findViewById(R.id.camera_preview),
                (SurfaceView) findViewById(R.id.transparent_view),
                (TextView) findViewById(R.id.timestamp),
                (ImageView) findViewById(R.id.record_button));


        // Set up camera and surface view to show camera preview
        cameraPreview.setupCameraView();

    }

    /* Called when activity is created, to make activity full screen, hide status bars, and ensure
    that screen never times out whilst recording.
     */
    public void setupScreen() {
        // Hide title and status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Make hardware (back, home, recent app) buttons low profile
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        // Screen never times out
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    // View v refers to the widget that was clicked. A second method is then called depending on the
    // type of view clicked. For instance, if the "Freeze Camera" button was clicked, then
    // case R.id.freeze_button would be executed
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.flashlight_button:
                ImageView flashButton = (ImageView) v;
                showFlashPopupMenu(flashButton);
                break;
            case R.id.select_color_button:
                Button colorButton = (Button) v;
                getColor(colorButton);
                break;
            case R.id.record_button:
                if (CameraPreview.isRecording) {
                    // release the MediaRecorder object, change icons, set isRecording = false;,
                    // and stop the timestamp from updating (and reset to 0)
                    cameraPreview.releaseMediaRecorder();
                    CameraPreview.mCamera.lock();         // take camera access back from MediaRecorder
                    Toast.makeText(this, "Saved in" +
                            CameraPreview.mMediaFile, Toast.LENGTH_LONG).show();

                } else {
                    // Prepare the camera in a separate task (as it can take time)
                    // This method is also responsible for changing isRecording, icons, and
                    // configfuring timestamp
                    Toast.makeText(this, "Now recording. Tap an object every 2-5 seconds to manually track it.",
                            Toast.LENGTH_LONG).show();
                    new MediaPrepareTask().execute(null, null, null);

                }
                break;
        }
    }


    /* This method is called when the flash icon is clicked.
     * A popup menu is presented to select On, Off, or Auto.
     */
    public void showFlashPopupMenu(ImageView image) {
        PopupMenu popup = new PopupMenu(this, image);
        popup.inflate(R.menu.flash_menu);


        // Set "Auto" as the selected item as this is default
        popup.getMenu().getItem(0).setChecked(true);

        popup.show();
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


    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        // The return value states whether the MediaRecorder was successfully started or not.
        // This value is passed into the onPostExecute method.
        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (cameraPreview.prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording

                CameraPreview.mMediaRecorder.start();
                Log.i(TAG, "Successfully started.");

            } else {
                // prepare didn't work, release the camera
                cameraPreview.releaseMediaRecorder();
                return false;
            }
            return true;
        }


        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                TrackingActivity.this.finish();

            // If doInBackground() returns true, then recording was
            // successful, so change record state and icons
            } else {
                ImageView recordButton = (ImageView) findViewById(R.id.record_button);
                recordButton.setImageResource(R.drawable.ic_stop);
                CameraPreview.isRecording = true;

                // Start updating the timestamp for recording if preparation is successful.
                Timer.startTime = SystemClock.uptimeMillis();
                Timer.customHandler.postDelayed(CameraPreview.mTimer.updateTimerThread, 0);
            }

        }

    }

}




