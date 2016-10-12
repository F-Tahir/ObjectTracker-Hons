
package uk.ac.ed.faizan.objecttracker;

import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;

import org.opencv.android.FpsMeter;


/* Helper methods to start and stop the timer that is displayed when user is recording a video */
public class Timer {

    public static long startTime = 0L;
    public static long timeInMilliseconds = 0L;
    public static long timeSwapBuff = 0L;
    public static long updatedTime = 0L;
    TextView timerValue;
    static String ymlTimestamp = "00:00:00:000";
    public static Handler customHandler = new Handler();

    // When timer object is created, set timerValue to be the ID of the timestamp in the layout.
    public Timer(TextView timerValue) {
        this.timerValue = timerValue;
    }

    public Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (timeInMilliseconds / 1000) % 60 ;
            int mins = (int) ((timeInMilliseconds / (1000*60)) % 60);
            int hours   = (int) ((timeInMilliseconds / (1000*60*60)) % 24);
            int milliseconds = (int) (updatedTime % 1000);


            // This time is shown in the camera app (excludes milliseconds)
            timerValue.setText("" + hours + ":"
                    + String.format("%02d", mins) + ":"
                    + String.format("%02d", secs));


            // This time is saved into the YNL file (includes milliseconds)
            ymlTimestamp = timerValue.getText().toString() + String.format(":%03d", milliseconds);

            customHandler.postDelayed(this, 0);

        }

    };
}
