package metaextract.nkm.com.myplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

import static metaextract.nkm.com.myplayer.MainActivity.getDateString;
import static metaextract.nkm.com.myplayer.MainActivity.getTimeString;


public class ManageSensors extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    private static ManageSensors manageSensorsHeartRate, manageSensorsGps, manageSensorsStepCounter,
            manageSensorsAcc, manageSensorsGravity, manageSensorsPressure, manageSensorsMagneticField,
            manageSensorsOrientation, manageSensorsRotationVector;

    private Context context;
    private FileManager fileManager;
    private float stepOffset;
    double latitude, longitude;

    public static synchronized ManageSensors getInstanceHeartRate(Context context) {
        if (manageSensorsHeartRate == null) {
            manageSensorsHeartRate = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsHeartRate;
    }

    public static synchronized ManageSensors getInstanceGps(Context context) {
        if (manageSensorsGps == null) {
            manageSensorsGps = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsGps;
    }

    public static synchronized ManageSensors getInstanceStepCounter(Context context) {
        if (manageSensorsStepCounter == null) {
            manageSensorsStepCounter = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsStepCounter;
    }

    public static synchronized ManageSensors getInstanceAcc(Context context) {
        if (manageSensorsAcc == null) {
            manageSensorsAcc = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsAcc;
    }

    public static synchronized ManageSensors getInstanceGravity(Context context) {
        if (manageSensorsGravity == null) {
            manageSensorsGravity = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsGravity;
    }

    public static synchronized ManageSensors getInstancePressure(Context context) {
        if (manageSensorsPressure == null) {
            manageSensorsPressure = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsPressure;
    }

    public static synchronized ManageSensors getInstanceMagneticField(Context context) {
        if (manageSensorsMagneticField == null) {
            manageSensorsMagneticField = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsMagneticField;
    }

    public static synchronized ManageSensors getInstanceOrientation(Context context) {
        if (manageSensorsOrientation == null) {
            manageSensorsOrientation = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsOrientation;
    }

    public static synchronized ManageSensors getInstanceRotationVector(Context context) {
        if (manageSensorsRotationVector == null) {
            manageSensorsRotationVector = new ManageSensors(context.getApplicationContext());
        }
        return manageSensorsRotationVector;
    }

    private ManageSensors(Context context) {
        this.context = context;
    }

    ManageSensors(Context context, String filename) {
        this.context = context;
        fileManager = new FileManager(filename, false);
    }

    public void setSongName(String songName) {
        fileManager = new FileManager(songName, false);
//        if (fileManager.file.toString().contains("-Gps")) {
//            Intent intent = new Intent(getApplicationContext(), GpsService.class);
//            startService(intent);
//        }
    }

    /**
     * The function writes to file named SongList.
     *
     * @param songsList - song list.
     */
    public void addSongList(ArrayList<Song> songsList) {
        fileManager.deleteFile();
        int i = 1;
        fileManager.writeInternalFileCsvNewLINE("Date: " + getDateString(), true);
        for (Song song : songsList) {
            long id = song.getId();
            String title = song.getTitle();
            String artist = song.getArtist();
            String data = song.getData();
            String album = song.getAlbum();
            String genre = song.getGenre();
            String Duration = song.getDuration();
            fileManager.writeInternalFileCsvNewLINE(Integer.toString(i), true);
            fileManager.writeInternalFileCsvSameLine(Long.toString(id), true);
            fileManager.writeInternalFileCsvSameLine(title, true);
            fileManager.writeInternalFileCsvSameLine(artist, true);
            fileManager.writeInternalFileCsvSameLine(album, true);
            fileManager.writeInternalFileCsvSameLine(Duration, true);
            fileManager.writeInternalFileCsvSameLine(genre, true);
            fileManager.writeInternalFileCsvSameLine(data, true);
            i++;
        }
    }

    /**
     * The function writes to file named Activity.
     *
     * @param currentSongName - current song name.
     * @param songId          - id of the song.
     * @param timeOfSong      - duration of the song.
     * @param lastSongName    - last song that was played.
     * @param progress        - position of the last song in the SeekBar.
     * @param activity        - activity that was performed: play, stop etc.
     */
    public void activityFile(String currentSongName, int songId, String timeOfSong, String lastSongName, String progress, String activity) {
        fileManager.writeInternalFileCsvNewLINE(getDateString(), true);
        fileManager.writeInternalFileCsvSameLine(getTimeString(), true);
        fileManager.writeInternalFileCsvSameLine(Integer.toString(songId), true);
        fileManager.writeInternalFileCsvSameLine(currentSongName, true);
        fileManager.writeInternalFileCsvSameLine(timeOfSong, true);
        fileManager.writeInternalFileCsvSameLine(lastSongName, true);
        fileManager.writeInternalFileCsvSameLine(progress, true);
        fileManager.writeInternalFileCsvSameLine(activity, true);
    }

    /**
     * The function writes to file named .
     *
     * @param SensorTypeString - sensor type.
     * @param values           - data of the sensor.
     */
    public void addSensorData(String SensorTypeString, float[] values) {
        fileManager.writeInternalFileCsvNewLINE(getDateString(), true);
        fileManager.writeInternalFileCsvSameLine(getTimeString(), true);
        switch (SensorTypeString) {
            case "Accelerometer":
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[1]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[2]), true);
                break;
            case "Gravity":
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[1]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[2]), true);
                break;
            case "Pressure":
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                break;
            case "MagneticField":
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[1]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[2]), true);
                break;
            case "Orientation":
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[1]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[2]), true);
                break;
            case "RotationVector":
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[1]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[2]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[3]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[4]), true);
            case "HeartRate":
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                break;
            case "StepCounter":
                if (stepOffset == 0) {
                    stepOffset = values[0];
                }
                float step = values[0] - stepOffset;
                fileManager.writeInternalFileCsvSameLine(Float.toString(step), true);
                break;
            case "Gps":
                fileManager.writeInternalFileCsvSameLine(SensorTypeString, true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[0]), true);
                fileManager.writeInternalFileCsvSameLine(Float.toString(values[1]), true);
                break;
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            latitude = Double.valueOf(intent.getStringExtra("latitude"));
            longitude = Double.valueOf(intent.getStringExtra("longitude"));

            float gpsArray[] = {(float) longitude, (float) latitude};
            manageSensorsGps.addSensorData("Gps", gpsArray);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(GpsService.str_receiver));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }
}