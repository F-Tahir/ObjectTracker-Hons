package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;


/*
 * This class is responsible for setting up the SurfacePreview, getting the Camera instance,
 * updating the SurfaceView with images from the Camera, updating timestamp, and releasing resources
 */

public class CameraPreview implements SurfaceHolder.Callback {

    static SurfaceView mPreview;
    static TextView mTimestamp;
    static ImageView mRecordButton;
    static Context mContext;

    static SurfaceHolder mHolder;
    static Camera mCamera;
    static MediaRecorder mMediaRecorder;
    static File mOutputFile;
    static CamcorderProfile mProfile;

    static boolean isRecording = false;
    private final String TAG = "object:tracker";

    public static long startTime = 0L;
    public static long timeInMilliseconds = 0L;
    public static long timeSwapBuff = 0L;
    public static long updatedTime = 0L;
    static Timer mTimer;

    public CameraPreview(Context context, SurfaceView preview, TextView timestamp, ImageView recordButton) {
        mContext = context;
        mPreview = preview;
        mTimestamp = timestamp;
        mRecordButton = recordButton;

        mCamera = CameraHelper.getDefaultCameraInstance();
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);
        mMediaRecorder = new MediaRecorder();

        mTimer = new Timer(timestamp);
    }


    /* Called in onResume() to set up the camera and the surfaceView associated with it. The surface
     * view is used to preview the camera buffer before recording initializes.
     */
    public boolean setupCameraView() {

        if (mHolder == null) {
            Log.i(TAG, "mHolder is null");
            return false;
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        mCamera.startPreview();
        Log.i(TAG, "Live camera preview started");
        return true;
    }


    /* Called when the recording is stopped, or if the activity was paused, so we should make sure that
     * if the user was recording, then as well as stopping the recording in the onPause() method,
     * we also change the ic_stop icon back to ic_record, and set isRecording = false.
     */
    public void releaseMediaRecorder(){

        if (isRecording) {
            try {
                mMediaRecorder.stop();  // stop the recording

                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(mContext, new String[] {
                                mOutputFile.getPath() },
                        new String[] { "video/mp4" }, null);
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
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
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(mProfile);

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
                    mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());

            // Use the same size for recording profile.
            mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mProfile.videoFrameWidth = optimalSize.width;
            mProfile.videoFrameHeight = optimalSize.height;


            // likewise for the camera object itself.
            parameters.setPreviewSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            mCamera.setParameters(parameters);

            try {
                //mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(surfaceHolder);
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
}
