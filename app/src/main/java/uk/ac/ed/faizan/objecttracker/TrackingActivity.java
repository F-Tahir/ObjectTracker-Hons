package uk.ac.ed.faizan.objecttracker;

import android.app.Activity;
import android.content.DialogInterface;
import android.media.CamcorderProfile;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaRecorder;
import android.hardware.Camera;


import java.io.File;
import java.io.IOException;
import java.util.List;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

public class TrackingActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback {


    private final static String TAG = "object:tracker"; // For debugging purposes

    private boolean isRecording = false;
    private boolean isRecordingPaused = false;

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private SurfaceView mTangoCameraPreview;
    private File mOutputFile;
    private SurfaceHolder mHolder;
    private CamcorderProfile profile;

    public static long startTime = 0L;
    public static long timeInMilliseconds = 0L;
    public static long timeSwapBuff = 0L;
    public static long updatedTime = 0L;
    Timer timer;


    // Default color for overlay color when tracking objects (Red)
    private int overlayColor = 0xffff0000;

    // Inflate the layout and set the camera view when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up UI to make full screen, low profile soft-keys, etc.
        setupScreen();

        // Inflate layout
        setContentView(R.layout.activity_tracking);

        // Pass in the timestamp widget into the timer construct, so the timestamp
        // can be updated when recording
        timer = new Timer((TextView) findViewById(R.id.timestamp));


        // Initialize the Tango's camera view to the TangoCameraPreview defined in activity_tracking.xml
        mTangoCameraPreview = (SurfaceView) findViewById(R.id.camera_preview);
        mHolder = mTangoCameraPreview.getHolder();
        mHolder.addCallback(this);


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
        releaseMediaRecorder(); // release resources such as preview
        releaseCamera(); // release the camera so that other applications can use it
    }

    // Called after onCreate() in an Android activity lifecycle.
    @Override
    protected void onResume() {
        Log.i(TAG, "In resume");
        super.onResume();

        // Set up camera and surface view to show camera preview
        setupCameraView();

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
                if (isRecording) {
                    // release the MediaRecorder object, change icons, set isRecording = false;
                    releaseMediaRecorder();
                    mCamera.lock();         // take camera access back from MediaRecorder

                    ((TextView) findViewById(R.id.timestamp)).setText(R.string.timestamp);
                    Timer.customHandler.removeCallbacks(timer.updateTimerThread);

                } else {
                    // Prepare the camera in a separate task (as it can take time)
                    // This method is also responsible for changing isRecording, and icons
                    new MediaPrepareTask().execute(null, null, null);

                    Timer.startTime = SystemClock.uptimeMillis();
                    Timer.customHandler.postDelayed(timer.updateTimerThread, 0);


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



    /* Called in onResume() to set up the camera and the surfaceView associated with it. The surface
     * view is used to preview the camera buffer before recording initializes.
     */
    private boolean setupCameraView() {

        // Calls the back camera by default
        mCamera = CameraHelper.getDefaultCameraInstance();

        try {
            mCamera.setPreviewDisplay(mTangoCameraPreview.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        mCamera.startPreview();
        Log.i(TAG, "Live camera preview started");
        return true;
    }

    // Called when the recording is stopped, or if the activity was paused, so we should make sure that
    // if the user was recording, then as well as stopping the recording in the onPause() method,
    // we also change the ic_stop icon back to ic_record, and set isRecording = false.
    private void releaseMediaRecorder(){

        if (isRecording) {
            try {
                mMediaRecorder.stop();  // stop the recording

                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(this, new String[] {
                                mOutputFile.getPath() },
                        new String[] { "video/mp4" }, null);
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
            }

            ImageView recordButton = (ImageView) findViewById(R.id.record_button);
            recordButton.setImageResource(R.drawable.ic_record);
            isRecording = false;
        }

        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    // Called only when activity is paused, so that other applications can access the camera.
    private void releaseCamera(){
        if (mCamera != null){
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }


    private boolean prepareVideoRecorder(){


        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera for MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);

        // Step 4: Set output file (handled in CameraHelper)
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        if (mOutputFile == null) {
            return false;
        }
        mMediaRecorder.setOutputFile(mOutputFile.getPath());
        mMediaRecorder.setPreviewDisplay(mHolder.getSurface());

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        if (mCamera != null) {

            // We need to make sure that our preview and recording video size are supported by the
            // camera. Query camera to find all the sizes and choose the optimal size given the
            // dimensions of our preview surface.
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                    mSupportedPreviewSizes, mTangoCameraPreview.getWidth(), mTangoCameraPreview.getHeight());

            // Use the same size for recording profile.
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            profile.videoFrameWidth = optimalSize.width;
            profile.videoFrameHeight = optimalSize.height;


            // likewise for the camera object itself.
            parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
            mCamera.setParameters(parameters);

            try {
                //mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

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
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording

                mMediaRecorder.start();
                Log.i(TAG, "Successfully started.");



            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
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
                isRecording = true;
            }


        }

    }

}




