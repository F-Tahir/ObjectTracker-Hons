package uk.ac.ed.faizan.objecttracker;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
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

import java.util.List;


public class  TrackingActivity extends Activity implements View.OnClickListener {

    private CameraPreview cameraPreview;
    private final String TAG = getClass().getSimpleName();
    CameraView mCameraPreview;

    // Default color for overlay color when tracking objects (Red)
    static int overlayColor = 0xffff0000;

    // Colours to be passed into OpenCV constructs
    static int a = 255;
    static int r = 255;
    static int g = 0;
    static int b = 0;


    // Inflate the layout and set the camera view when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Set up UI to make full screen, low profile soft-keys, etc.
        setupScreen();

        // Inflate layout
        setContentView(R.layout.activity_tracking);

        // Register and set onClickListeners for various views
        Button colorButton =  (Button) findViewById(R.id.select_color_button);
        ImageView recordButton = (ImageView) findViewById(R.id.record_button);
        Button flashButton = (Button) findViewById(R.id.flash_button);

        colorButton.setOnClickListener(this);
        recordButton.setOnClickListener(this);
        flashButton.setOnClickListener(this);
    }

    // Called when activity becomes obscured.
    @Override
    protected void onPause() {
        super.onPause();

        // This releases the camera
        if (mCameraPreview != null) {
            mCameraPreview.disableView();
        }
        cameraPreview.releaseMediaRecorder(); // release resources such as preview

    }

    // Called after onCreate() in an Android activity lifecycle.
    @Override
    protected void onResume() {
        Log.i(TAG, "In resume");
        super.onResume();
        mCameraPreview = (CameraView) findViewById(R.id.camera_preview);
        mCameraPreview.enableView();
        // Pass in the timestamp widget and surfaceview into the cameraPreview construct.
        cameraPreview = new CameraPreview(
                this,
                mCameraPreview,
                (SurfaceView) findViewById(R.id.transparent_view),
                (TextView) findViewById(R.id.timestamp),
                (ImageView) findViewById(R.id.record_button));
    }

    /**
     * Called at the start of activity creation to configure the screen. Sets the screen to full-size,
     * turns on immersive-screen mode, and ensures that the screen never times out.
     */
    public void setupScreen() {
        // Hide title and status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Screen never times out
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    // View v refers to the widget that was clicked. A second method is then called depending on the
    // type of view clicked. For instance, if the "Freeze Camera" button was clicked, then
    // case R.id.freeze_button would be executed
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.select_color_button:
                Button colorButton = (Button) v;
                getColor(colorButton);
                break;

            case R.id.record_button:
                if (CameraPreview.isRecording) {
                    // release the MediaRecorder object.
                    cameraPreview.releaseMediaRecorder();
                    Toast.makeText(this, "Saved in" +
                            CameraPreview.mMediaFile, Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(this, "Now recording. Tap an object every 2-5 seconds to manually track it.",
                            Toast.LENGTH_LONG).show();

                    // Prepare the camera in a separate task (as it can take time)
                    new MediaPrepareTask().execute(null, null, null);
                }
                break;

            case R.id.flash_button:
                List<String> flashModes = mCameraPreview.getFlashModes();
                if (CameraPreview.isFlashOn) {
                    if (mCameraPreview.hasCameraFlash()) {
                        mCameraPreview.disableFlash(flashModes);
                    }
                    CameraPreview.isFlashOn = false;
                } else {
                    CameraPreview.isFlashOn = true;
                    if (mCameraPreview.hasCameraFlash()) {
                        mCameraPreview.enableFlash(flashModes);
                        Log.i(TAG, "Camera has flash");
                    } else {
                        Toast.makeText(this, "Flash not available on this device", Toast.LENGTH_LONG).show();
                    }
                }
        }
    }

    /**
     * Create a dialog to select the color for tracking overlay. This tracking overlay includes
     * text, bounding boxes, circles (for manual tracking, etc).
     *
     * @param button The view that has to be clicked to bring up the color selection pane.
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
                            a = Color.alpha(overlayColor);
                            r = Color.red(overlayColor);
                            g = Color.green(overlayColor);
                            b = Color.blue(overlayColor);

                            Log.i(TAG, "overlayColor is " + Integer.toHexString(overlayColor));
                            Log.i(TAG, "a is " + a);
                            Log.i(TAG, "r is " + r);
                            Log.i(TAG, "g is " + g);
                            Log.i(TAG, "b is " + b);
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
     * operation.<br><br>
     * <b>Important:</b> This ASyncTask's onPostExecute method is responsible for setting the timer,
     * changing the drawable resource to a stop button, and setting the isRecording boolean to true.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

		/**
         * Calls the prepareVideoRecorder() method in an AsyncTask so it does not slow down the UI
         * thread.
         *
         * @return boolean returns whether or not the MediaRecorder preparation was successful
         */
        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (cameraPreview.prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording

                return true;

            } else {
                // prepare didn't work, release the recorder
                cameraPreview.releaseMediaRecorder();
                return false;
            }
        }


		/**
         * If the MediaRecorder preparations were a success, this method starts the MediaRecorder,
         * starts the timer, and changes the drawable resource and isRecording boolean.
         *
         * @param result Whether or not the essential MediaRecorder preparations were a success.
         */
        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                TrackingActivity.this.finish();

                // If doInBackground() returns true, then recording was
                // successful, so change record state and icons
            } else {

                Log.i(TAG, "Prepare was successful, now attempting to start");
                try {
                    CameraPreview.mMediaRecorder.start();
                    ImageView recordButton = (ImageView) findViewById(R.id.record_button);
                    recordButton.setImageResource(R.drawable.ic_stop);
                    CameraPreview.isRecording = true;

                    // Start updating the timestamp for recording if preparation is successful.
                    Timer.startTime = SystemClock.uptimeMillis();
                    Timer.customHandler.postDelayed(CameraPreview.mTimer.updateTimerThread, 0);
                    Log.i(TAG, "MediaRecorder started properly");
                } catch (RuntimeException e) {
                    Log.i(TAG, "MediaRecorder did not start properly.");
                }



                // This runnable is stopped in CameraPreview.releaseMediaRecorder();
            }

        }

    }

}