package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.util.List;


/*
 * This class is responsible for setting up the SurfacePreview, getting the Camera instance,
 * updating the SurfaceView with images from the Camera, updating timestamp, and releasing resources
 */

public class CameraPreview implements SurfaceHolder.Callback, View.OnTouchListener,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private final String TAG = "object:tracker";

    static CameraBridgeViewBase mCameraView;
    static SurfaceView mOverlayView;
    static SurfaceHolder mHolder;
    static SurfaceHolder mOverlayHolder;

    static TextView mTimestamp;
    static ImageView mRecordButton;
    static Context mContext;

    static Camera mCamera;
    static MediaRecorder mMediaRecorder;
    static File mMediaFile;
    static File mDataFile;
    static File mRootFolder;

    static CamcorderProfile mProfile;
    static Canvas canvas;

    static boolean isRecording = false;

    static Timer mTimer;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Coordinates of touched position;
    float mX;
    float mY;
    int frameCount = 0;


    public CameraPreview(Context context, CameraBridgeViewBase preview, SurfaceView overlay,
                         TextView timestamp, ImageView recordButton) {
        mContext = context;
        mCameraView = preview;

        mCameraView.enableView();
        mCameraView.setCvCameraViewListener(this);

        mHolder = mCameraView.getHolder();
        mHolder.addCallback(this);

        mOverlayView = overlay;
        mOverlayHolder = mOverlayView.getHolder();
        mOverlayHolder.setFormat(PixelFormat.TRANSPARENT);
        mOverlayView.setZOrderOnTop(true);

        mTimestamp = timestamp;
        mRecordButton = recordButton;

        // Causes errors on the Google Tango device
//        try {
//            releaseCamera();
//            mCamera = CameraHelper.getDefaultCameraInstance();
//        } catch (Exception e) {
//            Log.i(TAG, "Camera failed to open successfully");
//        }


        mTimer = new Timer(timestamp);
        mCameraView.setOnTouchListener(this);
    }


    /* Called in onResume() to set up the camera and the surfaceView associated with it. The surface
     * view is used to preview the camera buffer before recording initializes.
     *
     * CURRENTLY NOT USED - mCameraPreview.enableView(); does this for us.
     */
    public boolean setupCameraView() {

        if (mHolder == null) {
            return false;
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        mCamera.startPreview();
        return true;
    }


    /* Called when the recording is stopped, or if the activity was paused, so we should make sure that
     * if the user was recording, then as well as stopping the recording in the onPause() method,
     * we also change the ic_stop icon back to ic_record, and set isRecording = false.
     */
    public void releaseMediaRecorder(){

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
                mMediaRecorder.stop();  // stop the recording

                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(mContext, new String[] {
                                mMediaFile.getPath() },
                        new String[] { "video/mp4" }, null);
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mMediaFile.delete();
            }

            mTimestamp.setText(R.string.timestamp);
            Timer.customHandler.removeCallbacks(mTimer.updateTimerThread);

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
            mCamera.lock();
        }
    }

    // Called only when activity is paused, so that other applications can access the camera.
    public void releaseCamera(){
        if (mCamera != null){
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean prepareVideoRecorder(){

        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera for MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(mProfile);

        // Step 4: Set output file (handled in CameraHelper)
        String date = CreateFiles.getDate();
        mRootFolder = CreateFiles.getOutputFolder(date);
        Log.i(TAG, "Output folder is " + mRootFolder.toString());

        mMediaFile = CreateFiles.getOutputMediaFile(
                CreateFiles.MEDIA_TYPE_VIDEO, mRootFolder, date);
        mDataFile = CreateFiles.getOutputDataFile(mRootFolder, date);


        if (mMediaFile == null) {
            Log.i(TAG, "Output file was not created successfully!");
            return false;
        }

        mMediaRecorder.setOutputFile(mMediaFile.getPath());
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

        Log.i(TAG, "Surface created for the first time");
        if (mCamera != null) {

            // We need to make sure that our preview and recording video size are supported by the
            // camera. Query camera to find all the sizes and choose the optimal size given the
            // dimensions of our preview surface.
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                    mSupportedPreviewSizes, mCameraView.getWidth(), mCameraView.getHeight());

            // Use the same size for recording profile.
            mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mProfile.videoFrameWidth = optimalSize.width;
            mProfile.videoFrameHeight = optimalSize.height;


            // likewise for the camera object itself.
            parameters.setPreviewSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            mCamera.setParameters(parameters);

            try {
                mCamera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if (isRecording) frameCount++;
        return inputFrame.rgba();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mX = event.getX();
                mY = event.getY();
                Log.i(TAG, "In here");
                Log.i(TAG, "mX is " + Float.toString(mX));
                Log.i(TAG, "mY is " + Float.toString(mY));
                Log.i(TAG, "Framecount is " + Integer.toString(frameCount));

                // Check to see if the current holder actually exists and is active
                if (mOverlayHolder.getSurface().isValid()) {
                    drawCircle();
                }
                break;
        }
        return true;
    }

    private void drawCircle() {

        // Draw only if an active recording is taking place
        if (isRecording) {

            // Lock the canvas so that it can be drawn on
            canvas = mOverlayHolder.lockCanvas();
            paint.setStyle(Paint.Style.FILL);

            // Choose fill color to be the one selected by user, or red by default.
            paint.setColor(TrackingActivity.overlayColor);

            // Clear any previous circles
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            canvas.drawCircle(mX, mY, 60, paint);
            mOverlayHolder.unlockCanvasAndPost(canvas);
            CreateFiles.appendToFile(mDataFile, Timer.ymlTimestamp, mX, mY);
        }
    }

}