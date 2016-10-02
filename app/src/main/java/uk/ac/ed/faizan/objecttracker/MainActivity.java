package uk.ac.ed.faizan.objecttracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

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
                Uri videoFolderURI = Uri.parse(Environment.getExternalStorageDirectory() + "/ObjectTracker/");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(videoFolderURI, "resource/folder");

                // If the device has a file explorer app installed, then check if the folder exists.
                // If not, show a toast, and if it does, open the folder with relevant file explorer app.
                if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                    File videoFolder = new File(videoFolderURI.toString());
                    if (!videoFolder.exists()) {
                        Toast.makeText(this, "There are no recordings to view.",  Toast.LENGTH_LONG).show();
                    } else {
                        startActivity(intent);
                    }

                // If user has no file manager app, notify the user to install one.
                } else {
                    Toast.makeText(this, "Please install a File Manager application", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case R.id.preferences_button:
                // Start an intent to open the preferences window (may be removed)
                break;
            default:
                break;
        }
    }

}
