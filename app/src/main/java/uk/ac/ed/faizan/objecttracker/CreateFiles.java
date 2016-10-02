package uk.ac.ed.faizan.objecttracker;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class is responsible for creating a root directory called "ObjectDirectory" once, when
 * the first recording is taken. Thereafter, in the root directory, each time a new recording is taken,
 * a new folder is created. This folder is named such that it is always unique and can never clash
 * with an existing folder.
 *
 * This class also contains methods to create names for the (for the actual recording) and the
 * text file (to store the object data). Note that MediaRecorder's setOutputFile method is responsible
 * for creating the media file, and createNewFile() in getOutputDataFile is responsible for creating the
 * data file.
 *
 * This class also has a method that takes a file name, x and y coordinates and a timestamp,
 * and appends the data to the given file.
 */
public class CreateFiles {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    static final String TAG = "object:tracker";

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        Log.i(TAG, "Not writeable");
        return false;
    }

    // Return a formatted date, used to create a new directory
    public static String getDate() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    // Create a new folder for each recording and return the folder name.
    public static File getOutputFolder(String timeStamp) {

        boolean createRootDirSuccessful = false;
        boolean createSecondaryDirSuccessful = false;

        if (!isExternalStorageWritable()) {
            Log.i(TAG, "External storage is not writeable");
            return null;
        }
        // Create an "ObjectTracker" folder in SDCard root
        File root = Environment.getExternalStorageDirectory();
        File dir = new File (root.toString() + "/ObjectTracker");

        createRootDirSuccessful = dir.exists() || dir.mkdirs();

        // Within the "ObjectTracker" folder, create a new folder for each recording, named timeStamp.
        // If mkdir is successful, return it.
        if (createRootDirSuccessful) {
            Log.i(TAG, "Root directory has been created successfully.");

            File secondaryDir = new File(dir.toString() + "/" + "RECORDING_" + timeStamp);
            createSecondaryDirSuccessful = secondaryDir.mkdirs();

            if (createSecondaryDirSuccessful) {
                Log.i(TAG, "Secondary directory has been created successfully.");
                return secondaryDir;
            } else {
                Log.i(TAG, "Secondary directory failed to create.");
                return null;
            }
        } else {
            Log.i(TAG, "Root directory was not successfully created.");
            return null;
        }

    }

    /**
     * Creates a media file in the {@code Environment.DIRECTORY_PICTURES} directory. The directory
     * is persistent and available to other applications like gallery.
     *
     * @param type Media type. Can be video or image.
     * @return A file object pointing to the newly created file.
     */
    public static File getOutputMediaFile(int type, File rootFolder, String date){

        // Check to see if the passed in directory exists (i.e. was it created)
        if (!rootFolder.exists()){
            Log.i(TAG, "Root directory was not successfully created");
            return null;
        }

        // Create a non-conflicting media file name
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(rootFolder.toString() + File.separator +
                    "IMG_" + date +  ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(rootFolder.toString() + File.separator +
                    "VID_" + date + ".mp4");
        } else {
            return null;
        }

        Log.i(TAG, "Media file is +" +mediaFile);
        return mediaFile;
    }


    // Creates a file for the data (coordinates of object). Another method takes this file name
    // and writes to it.
    public static File getOutputDataFile(File rootFolder, String date) {

        // Check to see if the passed in directory exists (i.e. was it created)
        if (!rootFolder.exists()){
            Log.i(TAG, "Root was not successfully created");
            return null;
        }

        // If root folder was created successfully, then create a data file within that folder.
        File dataFile = new File(rootFolder.getPath() + File.separator +
                "DATA_" + date +  ".yml");
        Log.i(TAG, "Data file is " + dataFile);

        try {
            dataFile.createNewFile();
        } catch(IOException e) {
            Log.i(TAG, "File was not created successfully");
            return null;
        }
        return dataFile;
    }

    public static void appendToFile(File dataFile, String timeStamp, float xCoord, float yCoord) {
        FileOutputStream fos = null;

        if  (!dataFile.exists()) {

        return;
    }

        try {
            fos = new FileOutputStream(dataFile.toString(), true);
            byte[] buffer = String.format(Locale.UK, "timestamp: %s\n\tx: %d\n\ty: %d\n",
                    timeStamp, (int) xCoord, (int) yCoord).getBytes();
            fos.write(buffer);
            Log.i(TAG, "Wrote to " + dataFile);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "Data file was not found - check that it was created properly.");
        } catch (IOException e) {
            Log.i(TAG, "IOException occured when trying to close FileOutputStream for data file.");
        } finally {
            try {
                fos.close();
            } catch (IOException e){
                Log.i(TAG, "IOException occured when trying to close FileOutputStream for data file.");
            }
        }

    }
}
