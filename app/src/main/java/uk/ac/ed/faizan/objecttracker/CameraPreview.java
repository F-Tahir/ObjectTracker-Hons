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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;





/*
 * This class is responsible for setting up the SurfacePreview, getting the Camera instance,
 * updating the SurfaceView with images from the Camera, updating timestamp, and releasing resources
 */

public class CameraPreview implements View.OnTouchListener,
    CameraBridgeViewBase.CvCameraViewListener2 {

    public final String TAG = CameraPreview.class.getSimpleName();
    private CameraControl mCameraControl;
    private SurfaceView mOverlayView;
    private SurfaceHolder mOverlayHolder;
    private SensorFramework mSensorFramework;

    private TextView mTimestamp;
    private ImageView mRecordButton;
    private Button mFreezeButton;
    private Button mModeButton;
    private Button mMethodButton;
    private Context mContext;

    private Mat mCameraMat;
    private Mat mTemplateMat;
    private Mat mResult;
    private Mat mCameraMatResized;
    private Mat mTemplateMatResized;
    private Mat mCorrectedTemplateMat;
    private Point mMatchLoc;
    private Point mLastFrameLoc;
    private int mMatchMethod;
    private int dX; // Used for template matching to search smaller region
    private int dY;
    private int convertedX; // Coordinate of new manually corrected template
    private int convertedY;

    private File mDataFile;
    File mMediaFile;
    MediaRecorder mMediaRecorder;
    Timer mTimer;
    StorageSpace mStorageSpace;
    int frameCount = 0;

    boolean isRecording = false;
    boolean isFlashOn = false;
    boolean isPreviewFrozen = false;
    private boolean correctTemplate = false;
    private int mTrackingMode; // 0 for manual tracking, 1 for automatic

    // Values used for touch positions during manual tracking
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static Canvas canvas;

    // Used to resize the input image to speed up template matching.
    private static double resizeRatio = 0.5;


    public CameraPreview(Context context, CameraControl preview, Button freezeButton, Button modeButton,
                         SurfaceView overlay, TextView timestamp, ImageView recordButton, TextView storage,
                        Button methodButton) {
        mContext = context;
        mCameraControl = preview;
        mCameraControl.setCvCameraViewListener(this);
        mSensorFramework = new SensorFramework(context, this);

        mOverlayView = overlay;
        mOverlayHolder = mOverlayView.getHolder();
        mOverlayHolder.setFormat(PixelFormat.TRANSPARENT);
        mOverlayView.setZOrderOnTop(true);

        mTimestamp = timestamp;
        mRecordButton = recordButton;
        mModeButton = modeButton;
        mFreezeButton = freezeButton;
        mMethodButton = methodButton;

        mTimer = new Timer(timestamp);
        mStorageSpace = new StorageSpace(storage);

        mCameraControl.setOnTouchListener(this);

        // Used only for template matching (automatic tracking), but initialize anyway.
        mCameraMatResized = new Mat();
        mTemplateMatResized = new Mat();
    }

    /**
     * Retrieve the current frame from camera feed. We can then use this frame to select a template.
     * @return Mat object that wraps the current camera frame.
     */
    public Mat getCameraMat() {
        return mCameraMat;
    }


      /**
     * Used to set the template after user has carried out the template selection process.
     *
     * @param templateMat A bitmap object converted to a Mat, used for template matching
     */
    public void setTemplateMat(Mat templateMat) {
        mTemplateMat = templateMat;
    }


    /**
     * This function is called when the user hits the record button. This function will set up the
     * audio and video source, video framerate, encoding bitrate, create the video and data files,
     * and finally start recording if everything is successful. <br><br>
     * <b>Importantly</b>, this function sets the video (output resolution) size to the device's
     * preferred size by calling the built in getPreferredSize() method. The returned value of this
     * method differs over different devices.
     *
     * @param trackingMode 0 if manual tracking is selected, 1 for automatic tracking.
     * @param matchMethod Passed in from TrackingActivity, range of 6 values deciding which formula to mathc with.
     * @return boolean States whether or not the MediaRecorder preview was successful.
     */
    public boolean prepareVideoRecorder(final int trackingMode, int matchMethod, long time) {

        mTrackingMode = trackingMode;

        // Get the template from root if tracking mode is 1 (automatic tracking)
        if (mTrackingMode == 1) {

            Imgproc.cvtColor(mTemplateMat, mTemplateMat, Imgproc.COLOR_BGR2RGBA);
            mMatchMethod = matchMethod;
            mMatchLoc = null;

            // Resize the template image (as well as input image), so that template matching is faster.
            Imgproc.resize(mTemplateMat, mTemplateMatResized, new org.opencv.core.Size(),
                resizeRatio, resizeRatio, Imgproc.INTER_AREA);

            // Used to create a smaller region image for matchTemplate();
            dX = (int) (mTemplateMatResized.cols()*1.5);
            dY = (int) (mTemplateMatResized.rows()*1.5);
        }

        mMediaRecorder = new MediaRecorder();

        // TODO: Possibly provide an option to disable audio source so no audio is recorded.
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        // The aspect ratio should be identical to that in JavaCameraView, to avoid scaling issues.
        mMediaRecorder.setVideoSize(1280, 720);
        mMediaRecorder.setVideoEncodingBitRate(4 * 1000 * 1000);

        // Sets to 30 but does not check if phone supports 30, so need to add this
        // Will think about using a CamcorderProfile instead.
        mMediaRecorder.setVideoFrameRate(30);

        mCameraControl.lockAutoExposure();

        // Create the data and video file output
        mMediaFile = Utilities.getVideoFile(time);
        mDataFile = Utilities.getDataFile(time);

        mSensorFramework.setListeners(Utilities.getSensorDataFile(time));


        // Utilities.getVideoFile() returns false if storage is not writable, so check this.
        if (mMediaFile == null) {
            Toast.makeText(mContext, "Recording failed. Please ensure storage is writable.",
                Toast.LENGTH_LONG).show();
            Log.i(TAG, "Output file was not created successfully!");
            return false;
        }

        mMediaRecorder.setOutputFile(mMediaFile.getPath());

        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

            public void onInfo(MediaRecorder mediaRec, int error, int extra) {
                // TODO Auto-generated method stub

                if(error==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {

                    Toast.makeText(mContext, "Out of space - recording stopped. Saved in " +
                        mMediaFile, Toast.LENGTH_LONG).show();


                    // Set buttons to full alpha and release resources/change booleans and icons
                    releaseMediaRecorder();
                    Utilities.reconfigureUIButtons(mModeButton, mFreezeButton, mMethodButton, isRecording, trackingMode);

                }
            }
        });

        // Specify 5MB less so we have space for things such as the .yml file
        mMediaRecorder.setMaxFileSize(Utilities.getAvailableSpaceInBytes() - 5242880);

        // Try to prepare/start the media recorder
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


                // Stop listening for sensor data
                mSensorFramework.unsetListeners();

            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                throw new AssertionError(e);
            }

            mTimestamp.setText(R.string.timestamp);

            // Stop updating UI threads
            mTimer.customHandler.removeCallbacks(mTimer.updateTimerThread);
            mStorageSpace.customHandler.removeCallbacks(mStorageSpace.updateStorageSpaceThread);

            mRecordButton.setImageResource(R.drawable.ic_record);
            isRecording = false;
        }

        if (mMediaRecorder != null) {

            // clear recorder configuration and release the recorder object
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

        }
    }

    /* Functions implemented from setCvCameraListener*/
    @Override
    public void onCameraViewStarted(int width, int height) {
        mCameraMat = new Mat();
    }

    @Override
    // Free any resources here to avoid memory leaks.
    public void onCameraViewStopped() {

        if (mCameraMat != null)
            mCameraMat.release();

        if (mTemplateMat != null)
            mTemplateMatResized.release();

        if (mCameraMatResized != null)
            mCameraMatResized.release();

        if (mTemplateMat != null)
            mTemplateMat.release();

        if (mResult != null)
            mResult.release();
    }

    /* We do our OpenCV frame processing here.
    * When the camera preview is frozen (i.e. user hits freeze button), then disableView() is
    * called, and consequently, this callback will no longer be executed.
    *
    * Note: Preview can only be frozen whilst user is not recording.
    */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Don't update the frame if camera preview is frozen - return early.
        if (isPreviewFrozen) {
            return null;
        }
        frameCount++;

        mCameraMat = inputFrame.rgba();

        // TODO: Move as much out of here as possible
        if (isRecording) {

            // Automatic tracking
            if (mTrackingMode == 1) {

                // Downscale the image to speed up template matching (INTER_LINEAR is fastest resize method)
                Imgproc.resize(mCameraMat, mCameraMatResized, new org.opencv.core.Size(), resizeRatio,
                    resizeRatio, Imgproc.INTER_LINEAR);


                int result_cols = mCameraMat.cols() - mTemplateMat.cols() + 1;
                int result_rows = mCameraMat.rows() - mTemplateMat.rows() + 1;
                mResult = new Mat(result_rows, result_cols, CvType.CV_32F);

                int topLeftX = -1;
                int topLeftY = -1;

                // Search only a small region around the last known point, to speed up matching
                if (frameCount >= 2) {
                    topLeftX = getTopLeftXCoordOfRegion();
                    topLeftY = getTopLeftYCoordOfRegion();
                    Imgproc.matchTemplate(getROIFromImage(mCameraMatResized, topLeftX, topLeftY),
                        mTemplateMatResized, mResult, mMatchMethod);

                    // First frame, don't know the current location yet.
                } else {
                    Imgproc.matchTemplate(mCameraMatResized, mTemplateMatResized, mResult, mMatchMethod);
                }


                // If we normalize the result image when CCORR is used as matching method, matching is
                // very inaccurate.
                if (mMatchMethod != Imgproc.TM_CCORR && mMatchMethod != Imgproc.TM_CCORR_NORMED) {
                    Core.normalize(mResult, mResult, 0, 1, Core.NORM_MINMAX, -1, new Mat());
                }

                MinMaxLocResult mmr = Core.minMaxLoc(mResult);

                if (mMatchMethod == Imgproc.TM_SQDIFF || mMatchMethod == Imgproc.TM_SQDIFF_NORMED) {
                    mLastFrameLoc = mmr.minLoc;
                } else {
                    mLastFrameLoc = mmr.maxLoc;
                }

                mMatchLoc = new Point();
                // Coord is in terms of a small region, we want it in terms of full image
                if (frameCount >= 2 && topLeftX != -1 && topLeftY != -1) {
                    mMatchLoc.x = mLastFrameLoc.x + topLeftX;
                    mMatchLoc.y = mLastFrameLoc.y + topLeftY;

                    // mLastFrameLoc is used in the next frame to create the roi. This last frame location
                    // is in terms of the resized image.
                    mLastFrameLoc.x = mMatchLoc.x;
                    mLastFrameLoc.y = mMatchLoc.y;

                    // Need to scale coordinates back to full sized image up as we are working with images
                    // that are scaled down.
                    mMatchLoc.x = mMatchLoc.x*(1.0/resizeRatio);
                    mMatchLoc.y = mMatchLoc.y*(1.0/resizeRatio);

                    // First frame
                } else {
                    mMatchLoc.x = mLastFrameLoc.x*(1.0/resizeRatio);
                    mMatchLoc.y = mLastFrameLoc.y*(1.0/resizeRatio);
                }


                // Draw a boundary around the detected object.
                Imgproc.rectangle(mCameraMat, mMatchLoc, new Point((mMatchLoc.x + mTemplateMat.cols()),
                    (mMatchLoc.y + mTemplateMat.rows())), new Scalar(TrackingActivity.r, TrackingActivity.g,
                    TrackingActivity.b, TrackingActivity.a), 2);


                // Append center coord of rectangle, as well as time and frame number to .yml file
                // Add mTemplate.cols()/2.0 because mMatchLoc.x/y returns top left coordinate, we want center
                Utilities.appendToDataFile(mDataFile, frameCount, mTimer.ymlTimestamp, (int) (mMatchLoc.x +
                    (mTemplateMat.cols()/2.0f)), (int) (mMatchLoc.y + (mTemplateMat.cols()/2.0f)));


                // correctTemplate boolean is set in onTouch method. The new template is stored in
                // mCorrectedTemplateMat. So resize that object, and store the result in mTemplateMatResized
                if (correctTemplate) {

                    // Set mLastFrameLoc.x and mLastFrameLoc.y to center of new template, used for
                    // next frame to extract region. Converting in terms of downsampled image.
                    mLastFrameLoc.x = convertedX/(1.0/resizeRatio);
                    mLastFrameLoc.y = convertedY/(1.0/resizeRatio);

                    Imgproc.resize(mCorrectedTemplateMat, mTemplateMatResized, new org.opencv.core.Size(),
                        resizeRatio, resizeRatio, Imgproc.INTER_AREA);
                    correctTemplate = false;

                    // Otherwise just update template
                } else {
                    if (isNewTemplateInRange((int)mMatchLoc.x, (int)mMatchLoc.y)) {

                        Rect roi = new Rect((int) mMatchLoc.x, (int) mMatchLoc.y, mTemplateMat.width(), mTemplateMat.height());
                        mTemplateMat = new Mat(mCameraMat, roi);

                        Imgproc.resize(mTemplateMat, mTemplateMatResized, new org.opencv.core.Size(),
                            resizeRatio, resizeRatio, Imgproc.INTER_LINEAR);
                    }
                }
            }
        }
        return mCameraMat;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                float mX = event.getX();
                float mY = event.getY();

                // Check to see if the current holder actually exists and is active
                if (mOverlayHolder.getSurface().isValid()) {

                    // If recording, and manual tracking is selected, this indicates to draw a circle
                    if (isRecording && mTrackingMode == 0) {

                        drawCircle(mX, mY);

                        // Otherwise if tracking mode is automatic, carry ut template correction
                    } else if (isRecording && mTrackingMode == 1) {
                        templateCorrection(mX, mY);
                    }
                }
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
     * screen resolution. If this is not done, then for example, a 2560x1440 screen recording at 1920x1080
     * will pose issues when storing coordinates. If the user touches the bottom right corner, 2560x1440~
     * will be stored, as opposed to 1920x1080, which is the video resolution. It is obvious that this
     * poses issues. Hence, this method also does the conversion for us. It is also worth nothing that
     * this method is only called when user is recording, and manual tracking is selected. Checks
     * are done in the onTouch method, so no checks are needed to be done here.
     *
     * @param mX The x-coordinate of the touched position
     * @param mY The y-coordinate of the touched position
     */
    private void drawCircle(float mX, float mY) {

        // Lock the canvas so that it can be drawn on
        canvas = mOverlayHolder.lockCanvas();
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(TrackingActivity.overlayColor);

        // Clear any previous circles
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        canvas.drawCircle(mX, mY, 60, paint);
        mOverlayHolder.unlockCanvasAndPost(canvas);

        // Convert coordinates with respect to device resolution
        int convertedX = Utilities.convertDeviceXToCameraX(mX, mCameraControl.getFrameWidth(),
            mCameraControl.getWidth());
        int convertedY = Utilities.convertDeviceYToCameraY(mY, mCameraControl.getFrameHeight(),
            mCameraControl.getHeight());

        // Finally append to file
        Utilities.appendToDataFile(mDataFile, frameCount, mTimer.ymlTimestamp, convertedX, convertedY);
    }

    /**
     * This method can only be called when the user is recording in automatic tracking mode. onTouch()
     * checks for these conditions.
     *
     * This methods responsbility is to update the template manually, when a user clicks on a location
     * within bounds of the screen. The touched position will be the center point of the new template,
     * denoted by the mX and mY params. The new manually corrected template will be the same size as the
     * original template.
     *
     * This method will throw an error and return correctly if the user tries to select a template
     * region that is not within bounds of the screen.
     * @param mX The x coordinate of the touched location on screen.
     * @param mY The y coordinate of the touched location on screen.
     */
    private void templateCorrection(float mX, float mY) {

        // Convert the touched coordinate in terms of the camera res (1280x720)
        convertedX = Utilities.convertDeviceXToCameraX(mX, mCameraControl.getFrameWidth(),
            mCameraControl.getWidth());
        convertedY = Utilities.convertDeviceYToCameraY(mY, mCameraControl.getFrameHeight(),
            mCameraControl.getHeight());

        // We have the center coordinate, but we want the top left coordinate, so do some maths
        int startingX = (int) (convertedX - (mTemplateMat.width()/2.0f));
        int startingY = (int) (convertedY - (mTemplateMat.height()/2.0f));


        // Ensure the new template is within boundary of camera resolution, otherwise throw an error
        if (!isNewTemplateInRange(startingX, startingY)) {
            Toast.makeText(mContext, "New template will not be within range - template correction failed!",
                Toast.LENGTH_LONG).show();

        } else {
            // This boolean is used in onCameraFrame to retrieve the Mat and resize it.
            // The corrected Mat is stored in mCorrectedTemplateMat.
            correctTemplate = true;

            Rect roi = new Rect(startingX, startingY, mTemplateMat.width(), mTemplateMat.height());
            mCorrectedTemplateMat = new Mat(mCameraMat, roi);

            // Note that resizing is done in onCameraFrame() - if correctTemplate is true, then onCameraFrame
            // resizes the Mat stored in mCorrectedTemplateMat.
        }
    }


    /**
     * This method is used to check that when manual template correction is carried out, the new template
     * is within the correct range. Returns true if the range is valid, and false otherwise.
     *
     * @param x The starting(top-left) x coordinate of new template
     * @param y The starting (top-left) y coordinate of new template
     * @return True if new template is within range, false otherwise
     */
    private boolean isNewTemplateInRange(int x, int y) {
        if (x + mTemplateMat.width() < 0 || x + mTemplateMat.width() > 1280 || x < 0) {
            return false;
        } else if (y + mTemplateMat.height() < 0 || y + mTemplateMat.height() > 720 || y < 0) {
            return false;
        }

        return true;
    }


	/**
     * This function is called during template matching (automatic tracking) to obtain the top left X
     * coordinate of the region of interest. The region of interest is used to extract a smaller image from
     * the full sized image, increasing the speed of matchTemplate().
     *
     * @return The top left x-coordinate of the smaller region image
     */
    private int getTopLeftXCoordOfRegion() {
        return Math.max(0, (int) (mLastFrameLoc.x - (mTemplateMatResized.cols()/2.0) - dX));
    }

    /**
     * This function is called during template matching (automatic tracking) to obtain the top left y
     * coordinate of the region of interest. The region of interest is used to extract a smaller image from
     * the full sized image, increasing the speed of matchTemplate().
     *
     * @return The top left y-coordinate of the smaller region image
     */
    private int getTopLeftYCoordOfRegion() {
        return Math.max(0, (int) (mLastFrameLoc.y - (mTemplateMatResized.rows()/2.0) - dY));
    }


	/**
	 * This function is used to speed up template matching. After knowing the position of the object
     * from the last frame, this position is used to extract a region of interest, so that we can create
     * a smaller region to search from, rather than searching the entire image.
     *
     * @param mFullRegionImg The full image from the camera feed (downsized according to resizeRatio)
     * @param topLeftX The top left x-coordinate of the region of interest, calculated by calling getLeftXCoord()
     * @param topLeftY The top left y-coordinate of the region of interest, calculated by calling getLeftYCoord()
     * @return A new Mat that contains a small region of the full image. This is passed into matchTemplate
     */
    private Mat getROIFromImage(Mat mFullRegionImg, int topLeftX, int topLeftY) {

        // Create the ROI - http://answers.opencv.org/question/120681/opencv4android-mask-for-matchtemplate/
        int width;
        int height;

        // Make sure width/height isn't going to be greater than the entire image size
        if (topLeftX + (2*dX + mTemplateMatResized.cols()) > mFullRegionImg.cols()) {
            width = mFullRegionImg.cols() - topLeftX;
        } else {
            width = (2*dX + mTemplateMatResized.cols());
        }

        if (topLeftY + (2*dY + mTemplateMatResized.rows()) > mFullRegionImg.height()) {
            height = mFullRegionImg.rows() - topLeftY;
        } else {
            height = (2*dY + mTemplateMatResized.rows());
        }

        // Create the new image and return it
        Rect smallerRegionROI = new Rect(topLeftX, topLeftY, width, height);
        return new Mat(mFullRegionImg, smallerRegionROI);
    }
}