package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;

import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.List;

import static android.R.attr.width;

public class CameraView extends JavaCameraView {

    public final String TAG = "object:tracker";
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
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


    /**
     * Call this method to check to see if the camera supports a flash. If the developer wants to enable
     * flash, it is recommended to call this function before calling enableFlash(). This simply ensures
     * that the list returned from getFlashModes() is non-empty.
     *
     * @return True if camera has a built-in flash, false otherwise.
     */
    public boolean hasCameraFlash() {
        Camera.Parameters p = mCamera.getParameters();
        return p.getFlashMode() != null;
    }


    /*
     ** This method returns all the supported flash modes. It does not check if the device actually
     * supports flash, so could return null. Because of this, it is recommended to call hasCameraFlash()
     * before calling this method.
     *
     * @retuen A list of all the supported flashes. This list could return null.
     */
    public List<String> getFlashModes() {
        return mCamera.getParameters().getSupportedFlashModes();
    }

	/**
     * This function is called when the flash button is pressed by the user, while the flash is disabled.
     * First check to see if a suitable flash mode exists within the device, and if so, enable it
     *
     * @param flashModes A list of flash modes that the device supports
     */
    public void enableFlash(List<String> flashModes) {
        if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(p);
            mCamera.startPreview();
            Log.i(TAG, "Flash on");

        } else if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            mCamera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            mCamera.startPreview();
            Log.i(TAG, "Flash auto");
        }
    }


    public void setFps() {
        // Should check to see if camera supports this range, and if not, select next closest range.
        Camera.Parameters p = mCamera.getParameters();
        p.setPreviewFpsRange( 30000, 30000 ); // 30 fps
        mCamera.setParameters(p);
    }

    public void lockAutoExposure() {
        Camera.Parameters p = mCamera.getParameters();
        if ( p.isAutoExposureLockSupported() )
            p.setAutoExposureLock( true );
        mCamera.setParameters( p );
    }

    /**
     * This function is called when the flash button is pressed by the user while flash is enabled.
     * The function ensures that the camera contains a FLASH_MODE_OFF in its list of flash modes,
     * before setting the flash parameter to this mode. <br>This is <b>VERY</b>unlikely to occur, because enableFlash()
     * has to be called before disableFlash(), and enableFlash() checks to see if a flash on mode exists.
     * If a flash on mode exists, then a corresponding flash off mode has to exist.
     *
     * @param flashModes A list of flash modes that the device supports
     */
    public void disableFlash(List<String> flashModes) {
        if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            Camera.Parameters parameters2 = mCamera.getParameters();
            parameters2.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters2);
        }
    }






}
