package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;

import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.List;

public class CameraView extends JavaCameraView {

    public final String TAG = "object:tracker";
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /*
     * This method checks to see if the specified camera (mCamera = BACK CAMERA by default) has
     * an integrated flash facility.
     */
    public boolean hasCameraFlash() {
        Camera.Parameters p = mCamera.getParameters();
        return p.getFlashMode() != null;
    }

    public Size getPreferredSize() {
        return mCamera.getParameters().getPreferredPreviewSizeForVideo();
    }


	public void setPreviewSize(int width, int height) {
		Camera.Parameters params = mCamera.getParameters();
		params.setPreviewSize(width, height);
		mCamera.setParameters(params);
	}
    /*
     * This method returns all the supported flash modes. It does not check if the device actually
     * supports flash, so could return null. Because of this, it is recommended to call hasCameraFlash()
     * before calling this method.
     */
    public List<String> getFlashModes() {
        return mCamera.getParameters().getSupportedFlashModes();
    }

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

    public void disableFlash(List<String> flashModes) {
        Camera.Parameters parameters2 = mCamera.getParameters();
        parameters2.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters2);
    }






}
