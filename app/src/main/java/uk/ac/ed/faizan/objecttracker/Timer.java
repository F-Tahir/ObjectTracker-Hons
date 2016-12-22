
package uk.ac.ed.faizan.objecttracker;

import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;


import java.util.Locale;


/* Helper methods to start and stop the timer that is displayed when user is recording a video */
public class Timer {

    long startTime = 0L;
    private TextView timerValue;
    String ymlTimestamp = "00:00:00:000";

    Handler customHandler;

    // When timer object is created, set timerValue to be the ID of the timestamp in the layout.
    public Timer(TextView timerValue) {
        this.timerValue = timerValue;
        customHandler = new Handler();

        // Initialize the ymlTimestamp when a Timer object is created
        ymlTimestamp = "00:00:00:000";
    }

    public Runnable updateTimerThread = new Runnable() {

        private long timeInMilliseconds = 0L;
        private long timeSwapBuff = 0L;
        private long updatedTime = 0L;

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (timeInMilliseconds / 1000) % 60 ;
            int mins = (int) ((timeInMilliseconds / (1000*60)) % 60);
            int hours   = (int) ((timeInMilliseconds / (1000*60*60)) % 24);
            int milliseconds = (int) (updatedTime % 1000);


            // This time is shown in the camera app (excludes milliseconds)
            timerValue.setText(String.format(Locale.ENGLISH, "%s:%02d:%02d",  hours, mins, secs));

            // This time is saved into the YML file (includes milliseconds)
            ymlTimestamp = timerValue.getText().toString() + String.format(Locale.ENGLISH, ":%03d", milliseconds);

            customHandler.postDelayed(this, 0);

        }

    };
}
