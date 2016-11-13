package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import java.io.File;
import java.io.IOException;




/*
 * This class is responsible for setting up the SurfacePreview, getting the Camera instance,
 * updating the SurfaceView with images from the Camera, updating timestamp, and releasing resources
 */

public class
CameraPreview implements View.OnTouchListener,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private final String TAG = "object:tracker";
    private CameraControl mCameraControl;
    private SurfaceView mOverlayView;
    private SurfaceHolder mHolder;
    private SurfaceHolder mOverlayHolder;
    private TextView mTimestamp;
    private ImageView mRecordButton;
    private Context mContext;
    private int mTrackingMode; // 0 for manual tracking, 1 for automatic
    private Mat mCameraMat;
    private Mat mTemplateMat;

    MediaRecorder mMediaRecorder;
    public Timer mTimer;

    private File mDataFile;
    File mMediaFile;

    // Used only in automatic tracking for template matching.


    boolean isRecording = false;
    boolean isFlashOn = false;
    boolean isPreviewFrozen = false;




    // Booleans used for touch positions during manual tracking
    private float mX;
    private float mY;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static Canvas canvas;
    private static int frameCount = 0;


    public CameraPreview(Context context, CameraControl preview, SurfaceView overlay,
                         TextView timestamp, ImageView recordButton, int trackingMode) {
        mContext = context;
        mCameraControl = preview;
        mHolder = mCameraControl.getHolder();
        mCameraControl.setCvCameraViewListener(this);


        mOverlayView = overlay;
        mOverlayHolder = mOverlayView.getHolder();
        mOverlayHolder.setFormat(PixelFormat.TRANSPARENT);
        mOverlayView.setZOrderOnTop(true);

        mTimestamp = timestamp;
        mRecordButton = recordButton;

        mTimer = new Timer(timestamp);

        mCameraControl.setOnTouchListener(this);
    }


    /**
     * This function is called when the user hits the record button. This function will set up the
     * audio and video source, video framerate, encoding bitrate, create the video and data files,
     * and finally start recording if everything is successful. <br><br>
     * <b>Importantly</b>, this function sets the video (output resolution) size to the device's
     * preferred size by calling the built in getPreferredSize() method. The returned value of this
     * method differs over different devices.
     * @return boolean States whether or not the MediaRecorder preview was successful.
     */
    public boolean prepareVideoRecorder(int trackingMode) {

        mTrackingMode = trackingMode;

        // Get the template from root if tracking mode is 1 (automatic tracking)
        // TODO: Possibly remove once I implement template matching in realtime
        if (mTrackingMode == 1) {
            mTemplateMat = Highgui.imread(Utils.getTemplateFile().toString());
        }

        frameCount = 0;

        mMediaRecorder = new MediaRecorder();

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        // Set video size to preferred width and height, which is specific for each device.
        Size preferredSize = mCameraControl.getPreferredSize();
        // mMediaRecorder.setVideoSize(preferredSize.width, preferredSize.height);
        mMediaRecorder.setVideoSize(1280, 720);
        mMediaRecorder.setVideoEncodingBitRate(4 * 1000 * 1000);

        // Sets to 30 but does not check if phone supports 30, so need to add this
        // Will think about using a CamcorderProfile instead.
        mMediaRecorder.setVideoFrameRate(30);

        // Lock exposure to increase fps
        mCameraControl.lockAutoExposure();


        // Create the data and video file output
        long now = System.currentTimeMillis();
        mMediaFile = Utils.getVideoFile(now);
        mDataFile = Utils.getDataFile(now);
        if (mMediaFile == null) {
            Log.i(TAG, "Output file was not created successfully!");
            return false;
        }
        mMediaRecorder.setOutputFile(mMediaFile.getPath());


        // Try to prepare/start themedia recorder
        try {
            mMediaRecorder.prepare();
            Log.i(TAG, "Prepare was successful");

            mCameraControl.setRecorder(mMediaRecorder);

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


    /** Called when the recording is stopped, or if the activity was paused, so we should make sure that
     * if the user was recording, then as well as stopping the recording in the onPause() method,
     * we also change the ic_stop icon back to ic_record, and set isRecording = false.
     */
    public synchronized void releaseMediaRecorder(){

        frameCount = 0;

        // If recording is stopped, clear the canvas so that no circles are present
        // after recording
        if (canvas != null) {
            canvas = mOverlayHolder.lockCanvas();
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            mOverlayHolder.unlockCanvasAndPost(canvas);
            Log.i(TAG, "Canvas cleared");
        } else {
            Log.i(TAG, "Canvas is null");
        }


        if (isRecording) {
            try {
                mCameraControl.releaseRecording();
                mMediaRecorder.stop();  // stop the recording

                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(mContext, new String[] {
                                mMediaFile.getPath() },
                        new String[] { "video/mp4" }, null);
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                throw new AssertionError(e);
            }

            mTimestamp.setText(R.string.timestamp);
            mTimer.customHandler.removeCallbacks(mTimer.updateTimerThread);

            mRecordButton.setImageResource(R.drawable.ic_record);
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
        }
    }

    /* Functions implemented from setCvCameraListener*/
    @Override
    public void onCameraViewStarted(int width, int height) {
        mCameraMat = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mCameraMat.release();

        // Used in automatic tracking only.
        // TODO: Remove once I implement a way to select template in realtime
        if (mTemplateMat != null) mTemplateMat.release();
    }

    /* We do our OpenCV frame processing here.
    * When the camera preview is frozen (i.e. user hits freeze button), then disableView() is
    * called, and consequently, this callback will no longer be executed.
    *
    * Note: Preview can only be frozen whilst user is not recording.
    */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mCameraMat = inputFrame.rgba();
        if (isRecording) {

            frameCount++;
            // RGBA order as opposed to ARGB.
            // /*putText(mCameraMat, Integer.toString(frameCount), new Point(100, 500), 3, 1,
            //new Scalar(TrackingActivity.r, TrackingActivity.g, TrackingActivity.b, TrackingActivity.a), 2);*/
        }
        return mCameraMat;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mX = event.getX();
                mY = event.getY();

                // Check to see if the current holder actually exists and is active
                if (mOverlayHolder.getSurface().isValid()) {
                    drawCircle();
                }
                break;
        }
        return true;
    }


	/**
     * A method used for Manual tracking mode only. This method reacts to a users touch when recording.
     * The touch location's x and y coordinates are parsed, and stored into the corresponding .yml
     * file for the recording. A circle is then drawn on the screen to mark the touch location.
     * This circle's colour can be changed using the interface.<br><br>
     *
     * <b>Note</b> that the x and y coordinates are stored in terms of the camera resolution, <i>not</i> the
     * screen resolution.If this is not done, then for example, a 2560x1440 screen recording at 1920x1080
     * will pose issueswhen storing coordinates. If the user touches the bottom right corner, 2560x1440~
     * will be stored,as opposed to 1920x1080, which is the video resolution. It is obvious that this
     * poses issues.
     */
    private void drawCircle() {

        // Draw only if an active recording is taking place, and tracking mode is manual.
        Log.i(TAG, "tracking mode is " + mTrackingMode);
        if (isRecording && mTrackingMode == 0) {

            // Lock the canvas so that it can be drawn on
            canvas = mOverlayHolder.lockCanvas();
            paint.setStyle(Paint.Style.FILL);

            // Choose fill color to be the one selected by user, or red by default.
            paint.setColor(TrackingActivity.overlayColor);

            // Clear any previous circles
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            canvas.drawCircle(mX, mY, 60, paint);
            mOverlayHolder.unlockCanvasAndPost(canvas);

            Utils.appendToFile(mDataFile, frameCount, mTimer.ymlTimestamp,
                (mX * mCameraControl.getFrameWidth())/ mCameraControl.getWidth(),
                (mY * mCameraControl.getFrameHeight())/ mCameraControl.getHeight());

        }
    }

}