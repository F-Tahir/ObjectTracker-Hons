package uk.ac.ed.faizan.objecttracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.opencv.core.Mat;
import org.opencv.android.Utils;

import java.util.List;


public class  TrackingActivity extends Activity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();


    private CameraPreview mCameraPreview;
    private CameraControl mCameraControl;
    private TemplateSelection mTemplateSelection;

    // trackingMode 0 states manual mode, 1 states automatic mode
    int trackingMode = 0;
    boolean templateSelectionInitialized = false;

    private final int REQUEST_PERMISSIONS = 1;
    private String[] permissionList = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA};

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

        // Register onClickListeners for various views
        findViewById(R.id.select_color_button).setOnClickListener(this);
        findViewById(R.id.record_button).setOnClickListener(this);
        findViewById(R.id.flash_button).setOnClickListener(this);
        findViewById(R.id.freeze_button).setOnClickListener(this);
        findViewById(R.id.tracking_mode_button).setOnClickListener(this);

        mCameraControl = (CameraControl) findViewById(R.id.camera_preview);
        mTemplateSelection = (TemplateSelection) findViewById(R.id.select_template);
    }

    // Called when activity becomes obscured.
    @Override
    protected void onPause() {
        super.onPause();

        // This releases the camera
        if (mCameraControl != null) {
            mCameraControl.disableView();
        }

        // release resources such as preview
        if (mCameraPreview != null) {
            mCameraPreview.releaseMediaRecorder();
        }
    }

    // Called after onCreate() in an Android activity lifecycle.
    @Override
    protected void onResume() {
        super.onResume();

        // Request runtime permissions on devices >= API 23, before starting tracking activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // At least one required permission not granted, request it.
            // Dialog pauses activity, so when permissions are set, this check is done again, as
            // the activity is resumed when dialog closes.
            if(!Utilities.hasPermissions(this, permissionList)) {
                ActivityCompat.requestPermissions(this, permissionList, REQUEST_PERMISSIONS);


                // All required permissions granted, start the camera preview
            } else {
                mCameraControl.enableView();

                // Pass in the timestamp widget and surfaceview into the mCameraPreview construct.
                mCameraPreview = new CameraPreview(
                    this,
                    mCameraControl,
                    (SurfaceView) findViewById(R.id.transparent_view),
                    (TextView) findViewById(R.id.timestamp),
                    (ImageView) findViewById(R.id.record_button));
            }

        } else {
            // API < 23, so permissions were already set on installation. No need to check permissions.
            mCameraControl.enableView();

            // Pass in the timestamp widget and surfaceview into the mCameraPreview construct.
            mCameraPreview = new CameraPreview(
                this,
                mCameraControl,
                (SurfaceView) findViewById(R.id.transparent_view),
                (TextView) findViewById(R.id.timestamp),
                (ImageView) findViewById(R.id.record_button));
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_PERMISSIONS) {


            // At least one required permission not granted, show toast and don't start intent
            if (!Utilities.allPermissionsGranted(grantResults)) {

                Toast.makeText(this, "This app requires camera, audio and storage permissions to start tracking.",
                    Toast.LENGTH_LONG).show();
                finish();

            }
            /* The else part is taken care of onResume() - the permissions dialog pauses the activity,
            and thus when permission setting is finished, the activity is resumed; onResume() is called.
            So once the permissions are set, onResume() is called and if all permissions were granted,
            onResume() enables the camera view and initializes variables.*/
        }
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
    // TODO: Clean up this click listener code
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.select_color_button:
                getColor();
                break;


            case R.id.record_button:

                if (mCameraPreview.isRecording) {
                    // release the MediaRecorder object.
                    mCameraPreview.releaseMediaRecorder();
                    Toast.makeText(this, "Saved in" +
                        mCameraPreview.mMediaFile, Toast.LENGTH_LONG).show();

                    // Reconfigure UI to enable or disable MODE and freeze buttons, depending on
                    // what the tracking mode is (the function takes care of this for us).
                    Utilities.reconfigureUIButtons(findViewById(R.id.tracking_mode_button),
                        findViewById(R.id.freeze_button), trackingMode, mCameraPreview.isRecording);
                    break;
                }

                // Manual tracking and not recording.
                if (trackingMode == 0) {

                    Toast.makeText(this, "Now recording. Tap an object every 2-5 seconds to manually track it.",
                        Toast.LENGTH_LONG).show();

                    // Prepare the camera in a separate task (as it can take time)
                    new MediaPrepareTask().execute(null, null, null);


                    // Automatic tracking and not recording.
                } else {

                    // Set to true to state that we are in template selection phase
                    templateSelectionInitialized = true;
                    mTemplateSelection.setClearCanvas(false);

                    // Next step is done when Freeze button is pressed
                    Toast.makeText(this, "To select a template, focus the camera on the object," +
                        " then press the \"Live\" button.", Toast.LENGTH_LONG).show();

                    // Freeze button is disabled by default, so enable it.
                    // Enable it only for template selection, and nowhere else.
                    findViewById(R.id.freeze_button).setEnabled(true);
                    findViewById(R.id.freeze_button).setAlpha(1.0f);
                }
                break;


            // Deals with changing drawables and other resources when freeze is enabled/disabled.
            case R.id.freeze_button:
                Button freezeButton = (Button) v;

                // Pressed before selecting template
                if (!mCameraPreview.isRecording) {

                    if (!mCameraPreview.isPreviewFrozen) {
                        mCameraPreview.isPreviewFrozen = true;
                        freezeButton.setText(getResources().getString(R.string.freeze_enabled));
                        freezeButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_freeze_enabled, 0, 0);


                        // User has initialized automatic tracking, so start template selection -
                        // show instructions to user.
                        if (templateSelectionInitialized) {
                            findViewById(R.id.select_template).setVisibility(View.VISIBLE);
                            Toast.makeText(this, "Now select the template. For instructions, click on " +
                                "MODE > Help.", Toast.LENGTH_LONG).show();
                        }

                        // Pressed after template is selected
                    } else {
                        mCameraPreview.isPreviewFrozen = false;
                        freezeButton.setText(getResources().getString(R.string.freeze_disabled));
                        freezeButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_freeze_disabled, 0, 0);

                        // User is done with template selection, and has unfrozen the preview, so save
                        // the selected template by calling initializeTemplate()
                        if (templateSelectionInitialized) {
                            initializeTemplate();

                            // Set clearCanvas boolean to true so old rectangle can be overwritten
                            // TODO: Figure out why this doesn't call onDraw()

                            mTemplateSelection.setClearCanvas(true);
                            mTemplateSelection.invalidate();
                            findViewById(R.id.select_template).setVisibility(View.INVISIBLE);
                            templateSelectionInitialized = false;

                            Toast.makeText(this, "Template saved. Now recording", Toast.LENGTH_SHORT).show();
                        }
                    }


                }
                break;


            case R.id.flash_button:
                List<String> flashModes = mCameraControl.getFlashModes();
                Button flashButton = (Button) v;

                if (mCameraControl.hasCameraFlash()) {
                    if (!mCameraPreview.isFlashOn) {
                        mCameraControl.enableFlash(flashModes);
                        mCameraPreview.isFlashOn = true;
                        flashButton.setText(getResources().getString(R.string.flash_state_on));
                        flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash_on, 0, 0);

                    } else {
                        mCameraControl.disableFlash(flashModes);
                        mCameraPreview.isFlashOn = false;
                        flashButton.setText(getResources().getString(R.string.flash_state_off));
                        flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash_off, 0, 0);
                    }
                } else {
                    Toast.makeText(this, "Camera does not support flash!", Toast.LENGTH_LONG).show();
                }
                break;


            // TODO: Clean up this code a little
            case R.id.tracking_mode_button:
                PopupMenu popup = new PopupMenu(this, v);
                popup.inflate(R.menu.tracking_type_menu);

                // Set the menu item according to whatever is saved in trackingMode field.
                popup.getMenu().getItem(trackingMode).setChecked(true);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        switch (item.getItemId()) {

                            // Manual option selected
                            case R.id.manual:
                                item.setChecked(true);
                                trackingMode = 0;
                                Toast.makeText(TrackingActivity.this, "Tracking mode set to manual.",
                                    Toast.LENGTH_SHORT).show();

                                findViewById(R.id.freeze_button).setEnabled(false);
                                findViewById(R.id.freeze_button).setAlpha(0.5f);
                                return true;

                            // Automatic option selected
                            case R.id.automatic:
                                item.setChecked(true);

                                trackingMode = 1;
                                Toast.makeText(TrackingActivity.this, "Tracking mode set to automatic.",
                                    Toast.LENGTH_SHORT).show();

                                // We don't use alpha for colors in automatic tracking, so reset to 255.
                                overlayColor = overlayColor | 0xFF000000;

                                a = 255;
                                return true;

                            // Help button selected
                            case R.id.help:

                                AlertDialog.Builder adb = new AlertDialog.Builder(TrackingActivity.this);
                                adb.setView(R.layout.tracking_mode_help_layout);

                                adb.show();

                            default:
                                return false;
                        }
                    }
                });
                popup.show();

        }
    }


    /**
     * This method is called once the user acquires the object within the camera frame, and has selected
     * the rectangle region of the template. We then read the rectangle's coordinates in this function,
     * and then cut the template from the source image accordingly. The template is then converted
     * to a Mat object, and we set the mTemplateMat object in CameraPreview.java to this newly created
     * template Mat. We are then ready to start recording.
     */
    public void initializeTemplate() {
        TemplateSelection templateSelection = (TemplateSelection) findViewById(R.id.select_template);

        // Create a new bitmap with dimensions that are the same as the camera frame.
        Bitmap source = Bitmap.createBitmap(mCameraPreview.getCameraMat().width(),
            mCameraPreview.getCameraMat().height(), Bitmap.Config.ARGB_8888);

        // Copy the camera frame mat to the newly created bitmap
        Utils.matToBitmap(mCameraPreview.getCameraMat(), source);

        // Scale the coordinates in terms of image size, not screen size.
        double frameWidthRatio = (double) mCameraControl.getFrameWidth()/mCameraControl.getWidth();
        double frameHeightRatio = (double) mCameraControl.getFrameHeight()/mCameraControl.getHeight();

        int templateWidth =
            (int) (Math.abs(templateSelection.getLeftCoord() - templateSelection.getRightCoord())
                * frameWidthRatio);
        int templateHeight =
            (int) (Math.abs(templateSelection.getTopCoord() - templateSelection.getBottomCoord())
                * frameHeightRatio);

        // Get the region of the template selection, and crop the camera frame using this info, to
        // create a bitmap for the template. getLeftCoord() returns first x coord of template region,
        // getTopCoord() returns first y coord.
        Bitmap template = Bitmap.createBitmap(source, (int) (templateSelection.getLeftCoord()*frameWidthRatio),
            (int) (templateSelection.getTopCoord()*frameHeightRatio), templateWidth, templateHeight);

        // Create a new Mat and store template bitmap to it.
        Mat mat = new Mat();
        Utils.bitmapToMat(template, mat);

        // Set the template to the newly created mat
        mCameraPreview.setTemplateMat(mat);

        // Now attempt to start recording
        new MediaPrepareTask().execute(null, null, null);
    }



    /**
     * Create a dialog to select the color for tracking overlay. This tracking overlay includes
     * text, bounding boxes, circles (for manual tracking, etc).
     *
     */
    public void getColor() {

        // Alpha cannot change if we are using automatic tracking (as OpenCV does not support
        // alpha shapes on android YET), so disable the alpha slider if tracking mode is automatic.
        boolean showAlphaSlider = true;

        if (trackingMode == 1) {
            showAlphaSlider = false;
        }

        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Choose Color")
            .initialColor(overlayColor)
            .showAlphaSlider(showAlphaSlider)
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
            if (mCameraPreview.prepareVideoRecorder(trackingMode)) {
                // Camera is available, MediaRecorder is prepared,
                // now you can start recording. Goes to onPostExecute() with true as
                // a parameter.
                return true;

            } else {
                // Prepare didn't work, release the recorder
                mCameraPreview.releaseMediaRecorder();
                return false;
            }
        }


        /**
         * If the MediaRecorder preparations were a success, this method starts the MediaRecorder,
         * starts the timer, and changes the drawable resource and isRecording boolean. This method
         * is also responsible for disabling/enabling UI buttons such as the "Mode" and freeze button,
         * as they should not be enabled during recording, and should be enabled whilst not recording.
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
                    mCameraPreview.frameCount = 0;
                    mCameraPreview.mMediaRecorder.start();
                    ImageView recordButton = (ImageView) findViewById(R.id.record_button);
                    recordButton.setImageResource(R.drawable.ic_stop);
                    mCameraPreview.isRecording = true;

                    // Start updating the timestamp for recording if preparation is successful.
                    mCameraPreview.mTimer.startTime = SystemClock.uptimeMillis();
                    mCameraPreview.mTimer.customHandler.postDelayed(mCameraPreview.mTimer.updateTimerThread, 0);
                    Log.i(TAG, "MediaRecorder started properly");

                } catch (RuntimeException e) {
                    Log.i(TAG, "MediaRecorder did not start properly.");
                }

                Utilities.reconfigureUIButtons(findViewById(R.id.tracking_mode_button),
                    findViewById(R.id.freeze_button), trackingMode, mCameraPreview.isRecording);

                // This runnable is stopped in CameraPreview.releaseMediaRecorder();
            }

        }

    }

}