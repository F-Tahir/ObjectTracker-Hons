
package uk.ac.ed.faizan.objecttracker;

import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;


/* Helper methods to start and stop the timer that is displayed when user is recording a video */
public class Timer {

    public static long startTime = 0L;
    public static long timeInMilliseconds = 0L;
    public static long timeSwapBuff = 0L;
    public static long updatedTime = 0L;
    TextView timerValue;
    public static Handler customHandler = new Handler();

    // When timer object is created, set timerValue to be the ID of the timestamp in the layout.
    public Timer(TextView timerValue) {
        this.timerValue = timerValue;
    }

    public Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int hours = secs/3600;
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timerValue.setText("" + hours + ":"
                    + String.format("%02d", mins) + ":"
                    + String.format("%02d", secs));
            customHandler.postDelayed(this, 0);
        }

    };
}
