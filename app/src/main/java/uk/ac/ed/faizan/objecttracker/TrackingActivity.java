package uk.ac.ed.faizan.objecttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.Locale;


public class TrackingActivity extends Activity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();


    private CameraPreview mCameraPreview;
    private CameraControl mCameraControl;
    private TemplateSelection mTemplateSelection;
    private SharedPreferences mSharedPreferences;
    private Toast mToast;

    // trackingMode 0 states manual mode, 1 states automatic mode. Read from sharedPrefs to set these
    int trackingMode;
    int matchMethod;



    static boolean templateSelectionInitialized = false;
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

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set up UI to make full screen, low profile soft-keys, etc.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setupScreen();
        setContentView(R.layout.activity_tracking);

        findViewById(R.id.select_color_button).setOnClickListener(this);
        findViewById(R.id.record_button).setOnClickListener(this);
        findViewById(R.id.flash_button).setOnClickListener(this);
        findViewById(R.id.freeze_button).setOnClickListener(this);
        findViewById(R.id.tracking_mode_button).setOnClickListener(this);
        findViewById(R.id.template_initialization_cancel).setOnClickListener(this);
        findViewById(R.id.matching_method_button).setOnClickListener(this);
        findViewById(R.id.help_button).setOnClickListener(this);

        mCameraControl = (CameraControl) findViewById(R.id.camera_preview);
        mTemplateSelection = (TemplateSelection) findViewById(R.id.select_template);

        trackingMode = Integer.parseInt(mSharedPreferences.getString("pref_key_tracking_mode", "0"));
        matchMethod = Integer.parseInt(mSharedPreferences.getString("pref_key_matching_method", "5"));


        // Configure the UI icons and text according to match method
        if (trackingMode == 0) {
            ((TextView) findViewById(R.id.tracking_mode_text)).setText(getString(R.string.manual_mode));
        } else {
            ((TextView) findViewById(R.id.tracking_mode_text)).setText(getString(R.string.automatic_mode));
        }
        Utilities.reconfigureUIButtons(findViewById(R.id.tracking_mode_button), findViewById(R.id.freeze_button),
            findViewById(R.id.matching_method_button), false, trackingMode);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setupScreen();
    }



    // Called after onCreate() in an Android activity lifecycle.
    @Override
    protected void onResume() {
        Log.i(TAG, "In resume");
        super.onResume();

        ( (TextView) findViewById(R.id.storage_space)).setText(String.format(Locale.ENGLISH,
            "Free Space: %.2f GB", Utilities.getAvailableSpaceInGB()));

        mCameraControl = (CameraControl) findViewById(R.id.camera_preview);
        mTemplateSelection = (TemplateSelection) findViewById(R.id.select_template);
        mCameraControl.enableView();

        // Set the max value of the zoom bar programatically - whatever the camera supports
        SeekBar zoombar = (SeekBar) findViewById(R.id.camerazoom);
        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (mCameraControl.checkZoomSupport()) {
                    Log.i(TAG, "Camera max zoom is " + mCameraControl.getMaxZoomVal());
                    seekBar.setProgress(progress);

                    // SeekBAr's max value is 100, so set zoom progress in accordance to the max value
                    double zoomProgress = ((mCameraControl.getMaxZoomVal()/100.0)*progress);
                    mCameraControl.setZoomVal((int) Math.floor(zoomProgress));
                } else {
                    Toast.makeText(TrackingActivity.this, "Zoom not supported.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        mCameraPreview = new CameraPreview(
            this,
            trackingMode,
            mCameraControl,
            mSharedPreferences,
            (Button) findViewById(R.id.freeze_button),
            (Button) findViewById(R.id.tracking_mode_button),
            (SurfaceView) findViewById(R.id.transparent_view),
            (TextView) findViewById(R.id.timestamp),
            (ImageView) findViewById(R.id.record_button),
            (TextView) findViewById(R.id.storage_space),
            (Button) findViewById(R.id.matching_method_button));
    }

    // Called when activity becomes obscured.
    @Override
    protected void onPause() {
        super.onPause();

        // release resources such as preview
        if (mCameraPreview != null) {
            mCameraPreview.releaseMediaRecorder(false);
        }

    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();



        if (mCameraControl != null) {
            mCameraControl.disableView();
        }

        // If user stops activity during template initialisation, then set UI and booleans back to normal.
        if (templateSelectionInitialized) {
            cancelTemplateInitialisation();
        }
    }



    /**
     * Called at the start of activity creation to configure the screen. Sets the screen to full-size,
     * turns on immersive-screen mode, and ensures that the screen never times out.
     */
    public void setupScreen() {
        // Hide title and status bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (mSharedPreferences.getBoolean("pref_key_screen_brightness", false)) {
            this.getWindow().getAttributes().screenBrightness = 1F;
        }

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
                getColor();
                break;


            case R.id.record_button:

                if (mCameraPreview.isRecording) {
                    // release the MediaRecorder object.
                    mCameraPreview.releaseMediaRecorder(false);
                    break;
                }

                // Don't record if less than 100MB storage
                if (Utilities.getAvailableSpaceInGB() < 0.1) {
                    Toast.makeText(this, "Insufficient storage - at least 100MB needed. Recording failed",
                        Toast.LENGTH_LONG).show();
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

                    findViewById(R.id.tracking_mode_button).setEnabled(false);
                    findViewById(R.id.matching_method_button).setEnabled(false);
                    findViewById(R.id.tracking_mode_button).setAlpha(0.5f);
                    findViewById(R.id.matching_method_button).setAlpha(0.5f);


                    findViewById(R.id.template_initialization_cancel).setVisibility(View.VISIBLE);

                    // Next step is done when Freeze button is pressed
                    // If a previous toast is showing, cancel it and show the new one.
                    if (mToast != null) {
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(this, "To select a template, focus the camera on the object," +
                        " then press the \"Live\" button.", Toast.LENGTH_LONG);
                    mToast.show();

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

                            if (mToast != null) {
                                mToast.cancel();
                            }
                            findViewById(R.id.select_template).setVisibility(View.VISIBLE);
                            mToast = Toast.makeText(this, "Now select the template. For instructions, click on " +
                                "Help.", Toast.LENGTH_LONG);
                            mToast.show();
                        }

                        // Pressed after template is selected
                    } else {

                        // If manual tracking, change icon regardless, no verification of template matching
                        // required
                        if (trackingMode == 0) {
                            mCameraPreview.isPreviewFrozen = false;
                            freezeButton.setText(getResources().getString(R.string.freeze_disabled));
                            freezeButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_freeze_disabled, 0, 0);
                        }

                        // User is done with template selection, and has unfrozen the preview, so save
                        // the selected template by calling initializeTemplate()
                        if (templateSelectionInitialized) {
                            boolean successful = initializeTemplate();

                            // Only change icons and hide cancel button etc if template matching was successful.
                            if (successful) {
                                mTemplateSelection.setClearCanvas(true);
                                mTemplateSelection.invalidate();
                                findViewById(R.id.select_template).setVisibility(View.INVISIBLE);
                                templateSelectionInitialized = false;

                                mCameraPreview.isPreviewFrozen = false;
                                freezeButton.setText(getResources().getString(R.string.freeze_disabled));
                                freezeButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_freeze_disabled, 0, 0);

                                findViewById(R.id.template_initialization_cancel).setVisibility(View.INVISIBLE);
                                if (mToast != null) {
                                    mToast.cancel();
                                }
                                mToast = Toast.makeText(this, "Template saved. Now recording", Toast.LENGTH_SHORT);
                                mToast.show();

                            } else {
                                Toast.makeText(this, "Template not selected properly, perhaps the template" +
                                    " was too small.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }


                }
                break;

            // TODO: Fill in description for "Matching Methods"
            case R.id.help_button:
                AlertDialog.Builder adb = new AlertDialog.Builder(TrackingActivity.this);
                adb.setView(R.layout.tracking_mode_help_layout);

                adb.show();
                break;


            // Template initialization cancelled - change booleans, and icons
            case R.id.template_initialization_cancel:
                cancelTemplateInitialisation();
                break;


            case R.id.flash_button:
                List<String> flashModes = mCameraControl.getFlashModes();
                Button flashButton = (Button) v;

                if (!mCameraPreview.isFlashOn) {
                    if (mCameraControl.enableFlash(flashModes)) {
                        mCameraPreview.isFlashOn = true;
                        flashButton.setText(getResources().getString(R.string.flash_state_on));
                        flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash_on, 0, 0);
                    } else {
                        Toast.makeText(this, "Your device does not support flash.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (mCameraControl.disableFlash(flashModes)) {
                        mCameraPreview.isFlashOn = false;
                        flashButton.setText(getResources().getString(R.string.flash_state_off));
                        flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash_off, 0, 0);
                    } else {
                        Toast.makeText(this, "Flash cannot be turned off. Please exit the application.",
                            Toast.LENGTH_LONG).show();
                    }
                }
                break;


            case R.id.matching_method_button:
                PopupMenu _popup = new PopupMenu(this, v);
                _popup.inflate(R.menu.match_method_menu);

                // Set the menu item according to whatever is saved in trackingMode field.
                _popup.getMenu().getItem(matchMethod).setChecked(true);

                _popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {

                            case R.id.sqdiff:
                                if (!item.isChecked()) item.setChecked(true);
                                matchMethod = Imgproc.TM_SQDIFF;
                                return true;

                            case R.id.sqdiff_normed:
                                if (!item.isChecked()) item.setChecked(true);
                                matchMethod = Imgproc.TM_SQDIFF_NORMED;
                                return true;

                            case R.id.ccorr:
                                if (!item.isChecked()) item.setChecked(true);
                                matchMethod = Imgproc.TM_CCORR;
                                return true;

                            case R.id.ccorr_normed:
                                if (!item.isChecked()) item.setChecked(true);
                                matchMethod = Imgproc.TM_CCORR_NORMED;
                                return true;

                            case R.id.ccoeff:
                                if (!item.isChecked()) item.setChecked(true);
                                matchMethod = Imgproc.TM_CCOEFF;
                                return true;

                            case R.id.ccoef_normed:
                                if (!item.isChecked()) item.setChecked(true);
                                matchMethod = Imgproc.TM_CCOEFF_NORMED;
                                return true;


                            default:
                                return false;
                        }
                    }
                });
                _popup.show();
                break;



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

                                ((TextView) findViewById(R.id.tracking_mode_text)).setText(getString(
                                    R.string.manual_mode));

                                Utilities.reconfigureUIButtons(findViewById(R.id.tracking_mode_button),
                                    findViewById(R.id.freeze_button), findViewById(R.id.matching_method_button),
                                    mCameraPreview.isRecording, trackingMode);
                                return true;

                            // Automatic option selected
                            case R.id.automatic:
                                item.setChecked(true);
                                trackingMode = 1;
                                Log.i(TAG, "Tracking mode is 1");

                                ((TextView) findViewById(R.id.tracking_mode_text)).setText(getString(
                                    R.string.automatic_mode));

                                Utilities.reconfigureUIButtons(findViewById(R.id.tracking_mode_button),
                                    findViewById(R.id.freeze_button), findViewById(R.id.matching_method_button),
                                    mCameraPreview.isRecording, trackingMode);

                                // We don't use alpha for colors in automatic tracking, so reset to 255.
                                overlayColor = overlayColor | 0xFF000000;

                                a = 255;
                                return true;



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
     *
     * @return True if template selection was carried out correctly, false otherwise
     */
    public boolean initializeTemplate() {
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

        // Template selection was unsuccessful (user selected too small of an area)
        if (templateWidth < 15 || templateHeight < 15) {
            return false;
        }

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

        // Now attempt to start recording. Check if template is null before?
        new MediaPrepareTask().execute(null, null, null);
        return true;

    }


	/**
     * This function is called if user decides to cancel the template initialisation process once it
     * has been started. The function is responsible for restoring default boolean values, as well as
     * reconfiguring the UI to what it was prior to starting template initialisation.
     */
    private void cancelTemplateInitialisation() {
        Button freeze = (Button) findViewById(R.id.freeze_button);
        templateSelectionInitialized = false;
        mCameraPreview.isPreviewFrozen = false;
        Toast.makeText(this, "Template initialization cancelled", Toast.LENGTH_SHORT).show();

        // Clear tracking boundary and set tracking canvas to invisible
        mTemplateSelection.setClearCanvas(true);
        mTemplateSelection.invalidate();
        findViewById(R.id.select_template).setVisibility(View.INVISIBLE);

        // Change the freeze button
        freeze.setText(getResources().getString(R.string.freeze_disabled));
        freeze.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_freeze_disabled, 0, 0);
        findViewById(R.id.template_initialization_cancel).setVisibility(View.INVISIBLE);

        Utilities.reconfigureUIButtons(findViewById(R.id.tracking_mode_button),
            findViewById(R.id.freeze_button), findViewById(R.id.matching_method_button),
            mCameraPreview.isRecording, trackingMode);
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
            if (mCameraPreview.prepareVideoRecorder(trackingMode, matchMethod, System.currentTimeMillis())) {
                // Camera is available, MediaRecorder is prepared,
                // now you can start recording. Goes to onPostExecute() with true as
                // a parameter.
                return true;

            } else {
                // Prepare didn't work, release the recorder
                mCameraPreview.releaseMediaRecorder(false);
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

                    // Start updating the timestamp and available storage space for recording
                    // if preparation is successful.
                    mCameraPreview.mTimer.startTime = SystemClock.uptimeMillis();
                    mCameraPreview.mTimer.customHandler.postDelayed(mCameraPreview.mTimer.updateTimerThread, 0);

                    mCameraPreview.mStorageSpace.customHandler.postDelayed(mCameraPreview.
                        mStorageSpace.updateStorageSpaceThread, 0);

                } catch (RuntimeException e) {
                    Log.i(TAG, "MediaRecorder did not start properly.");
                }

                Utilities.reconfigureUIButtons(findViewById(R.id.tracking_mode_button),
                    findViewById(R.id.freeze_button),  findViewById(R.id.matching_method_button),
                    mCameraPreview.isRecording, trackingMode);

                // This runnable is stopped in CameraPreview.releaseMediaRecorder();
            }

        }

    }

}