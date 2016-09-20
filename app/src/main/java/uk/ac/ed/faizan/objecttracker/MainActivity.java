package uk.ac.ed.faizan.objecttracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views in activity_main.xml identified by the ID specified in the parameters
        Button startTrackingButton = (Button) findViewById(R.id.start_tracking_button);
        Button viewRecordingsButton = (Button) findViewById(R.id.view_recordings_button);
        Button preferencesButton = (Button) findViewById(R.id.preferences_button);

        /* Attach an onClickListener to each of the 3 buttons to detect clicks and carry out
        relevant actions
         */
        startTrackingButton.setOnClickListener(this);
        viewRecordingsButton.setOnClickListener(this);
        preferencesButton.setOnClickListener(this);


    }


    @Override
    public void onClick(View view) {

        switch(view.getId()) {
            case R.id.start_tracking_button:
                Intent i = new Intent(this, TrackingActivity.class);
                startActivity(i);
                break;
            case R.id.view_recordings_button:
                // Start an intent to open the file storage system
                break;
            case R.id.preferences_button:
                // Start an intent to open the preferences window (may be removed)
                break;
            default:
                break;
        }
    }

}
