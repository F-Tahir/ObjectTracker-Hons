package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.List;



public class CameraControl extends JavaCameraView {

    public final String TAG = CameraControl.class.getSimpleName();
    public CameraControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * This function is called to determine what the maximum zoom value of the camera is. First check
     * whether the camera supports zoom. If not, return -1.
     *
     * @return A value determining maximum zoom value of camera, -1 if zoom is not supported.
     */
    public int getMaxZoomVal() {
        if (checkZoomSupport()) {
            return mCamera.getParameters().getMaxZoom();
        } else {
            return -1;
        }
    }

	/**
     * This function is called to check whether the camera supports software zoom.
     *
     * @return True if camera supports zoom, false otherwise.
     */
    public boolean checkZoomSupport() {
        return mCamera.getParameters().isZoomSupported();
    }

	/**
	 * This function is called to set the camera zoom value. The value is taken from the SeekBar widget
     * and is of a value between 0 and the  number returned by getMaxZoomVal()
     *
     * @param zoomVal the value used to set the zoom.
     */
    public void setZoomVal(int zoomVal) {
        Parameters params = mCamera.getParameters();
        params.setZoom(zoomVal);
        Log.i(TAG, "ZoomValue is: " +zoomVal);

        if (mCamera != null)
            mCamera.setParameters(params);
    }

	/**
     * Accessor function used to get a list of all the supported camera parameters, such as whether
     * focus is supported, zoom is supported, etc.
     *
     * @return A list of the devices' camera parameters
     */
    public Parameters getCameraParams() {
        return mCamera.getParameters();
    }

    /**
     * Modifier function used to set a list of camera parameters.
     *
     * @params params list of camera parameters that should be set
     */
    public void setCameraParams(Parameters params) {
        mCamera.setParameters(params);
    }


    /**
     * Calls the camera's getPreferredPreviewSizeForVideo() function. This function returns the optimal
     * preview size, and differs across devices, so it is recommended to use this value when setting the
     * preview size.
     *
     * @return The preferred width and height of the device's preview size.
     */
    public Size getPreferredSize() {
        return mCamera.getParameters().getPreferredPreviewSizeForVideo();
    }


    /*
     ** This method returns all the supported flash modes. It does not check if the device actually
     * supports flash, so could return null. Because of this, it is recommended to call hasCameraFlash()
     * before calling this method.
     *
     * @return A list of all the supported flashes. This list could return null.
     */
    public List<String> getFlashModes() {
        return mCamera.getParameters().getSupportedFlashModes();
    }

	/**
     * This function is called when the flash button is pressed by the user, while the flash is disabled.
     * First check to see if a suitable flash mode exists within the device, and if so, enable it
     *
     * @param flashModes A list of flash modes that the device supports
     * @return True if flash was enabled successfully, false otherwise
     */
    public boolean enableFlash(List<String> flashModes) {
        if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(p);
            mCamera.startPreview();
            return true;

        } else if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            mCamera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            mCamera.startPreview();
            return true;
        }
        // No relevant flash mode supported, so return false.
        Log.i(TAG, "Flash not turned on successfully. Perhaps not supported.");
        return false;
    }


    /**
     * This function is called when the flash button is pressed by the user while flash is enabled.
     * The function ensures that the camera contains a FLASH_MODE_OFF in its list of flash modes,
     * before setting the flash parameter to this mode. <br>This is <b>VERY</b>unlikely to occur, because enableFlash()
     * has to be called before disableFlash(), and enableFlash() checks to see if a flash on mode exists.
     * If a flash on mode exists, then a corresponding flash off mode has to exist.
     *
     * @param flashModes A list of flash modes that the device supports
     * @return True if flash was disabled successfully, false otherwise. Should never return false/
     */
    public boolean disableFlash(List<String> flashModes) {
        if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            Camera.Parameters parameters2 = mCamera.getParameters();
            parameters2.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters2);
            return true;
        }
        // Should not reach here, as this function can only be called after flash is turned on
        Log.e(TAG, "Flash not turned off successfully!");
        return false;
    }


    /**
     * Locks the cameras exposure
     */
    public void lockAutoExposure() {
        Camera.Parameters p = mCamera.getParameters();
        if ( p.isAutoExposureLockSupported() )
            p.setAutoExposureLock( true );
        mCamera.setParameters( p );
    }



}
