package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.util.AttributeSet;
import android.hardware.Camera.Size;

import org.opencv.android.JavaCameraView;

import java.util.List;

public class CameraView extends JavaCameraView {

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }
    
}
