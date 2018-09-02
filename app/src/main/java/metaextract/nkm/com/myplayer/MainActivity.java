package metaextract.nkm.com.myplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;

import org.apache.commons.compress.utils.IOUtils;

public class MainActivity extends Activity implements OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private ImageButton buttonForward, buttonBackward, buttonNext, buttonPrevious, buttonPlay, buttonRepeat, buttonShuffle;
    private ImageView songImageView;
    private TextView songTitleLabel, songCurrentDurationLabel, songTotalDurationLabel;

    ArithmeticFunctions fun = new ArithmeticFunctions();
    MyLambdaFunction[] LambdaFunctions = {fun.getAvg(), fun.getSD()};
    PredictAction p;
    String[] FunctionsName = {"_Avg", "_SD"};

    private int progress;
    private SeekBar seekBar;
    private Handler handler = new Handler();

    private int songId = 0;
    private boolean shuffle = false, repeat = false;
    private ArrayList<Song> songsList = new ArrayList<>();
    private String lastSongName = "...", currentSongName = "", songTotalDuration = "", appStartingTime;

    private Utilities utils;
    private SendToWear sendToWear;
    private MediaPlayer mediaPlayer;
    private static Calendar calendar;

    private ManageSensors manageSensorsAcc, manageSensorsGravity,
            manageSensorsPressure, manageSensorsMagneticField, manageSensorsOrientation,
            manageSensorsRotationVector, manageSensorsActivity, manageSensorsSongList,
            manageSensorsHeartRate, manageSensorsStepCounter, manageSensorsGps;


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private final static String KEY_LOCATION = "location";
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    private Location currentLocation;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LocationSettingsRequest locationSettingsRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    private Boolean requestingLocationUpdates;
    private String lastUpdateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appStartingTime = getTimeString();
        buttonPlay = findViewById(R.id.buttonPlay);
        buttonNext = findViewById(R.id.buttonNext);
        buttonForward = findViewById(R.id.buttonForward);
        buttonBackward = findViewById(R.id.buttonBackward);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonRepeat = findViewById(R.id.buttonRepeat);
        buttonShuffle = findViewById(R.id.buttonShuffle);
        songImageView = findViewById(R.id.songImage);
        songTitleLabel = findViewById(R.id.songTitle);
        seekBar = findViewById(R.id.songProgressBar);
        songCurrentDurationLabel = findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = findViewById(R.id.songTotalDurationLabel);

        utils = new Utilities();
        mediaPlayer = new MediaPlayer();
        ButtonClickFromWear.getInstance(this);
        mediaPlayer.setOnCompletionListener(this);
        sendToWear = SendToWear.getInstance(this);
        seekBar.setOnSeekBarChangeListener(this);

        manageSensorsGps = ManageSensors.getInstanceGps(this);
        manageSensorsAcc = ManageSensors.getInstanceAcc(this);
        manageSensorsGravity = ManageSensors.getInstanceGravity(this);
        manageSensorsPressure = ManageSensors.getInstancePressure(this);
        manageSensorsHeartRate = ManageSensors.getInstanceHeartRate(this);
        manageSensorsOrientation = ManageSensors.getInstanceOrientation(this);
        manageSensorsStepCounter = ManageSensors.getInstanceStepCounter(this);
        manageSensorsMagneticField = ManageSensors.getInstanceMagneticField(this);
        manageSensorsRotationVector = ManageSensors.getInstanceRotationVector(this);

        requestingLocationUpdates = false;
        lastUpdateTime = "";

        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        startLocationUpdates();
        manageSensorsGps.setSongName(currentSongName);

        // Checking for permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        // If there is permission then we will get the song list
        if (permissionCheck == 0) {
            songsList = getPlayList();
            // Sort abc....
            Collections.sort(songsList, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    return a.getTitle().compareTo(b.getTitle());
                }
            });
            if (songsList.size() > 0) {
                currentSongName = songsList.get(1).getTitle();
            }
            // Create SongList file
            manageSensorsSongList = new ManageSensors(this, "SongList");
            manageSensorsSongList.addSongList(songsList);

            // Create Activity file
            manageSensorsActivity = new ManageSensors(this, "Activity");
            writeToActivity("App start");

            if (p == null) {
                DataVector dv = new DataVector();
                p = new PredictAction(dv.getVectorAttributes(), FunctionsName);
            }
        }

        // Checks whether there was a request for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // If permission denied
            // No explanation needed, we can request the permission.
            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an app-defined int constant.
            // The callback method gets the result of the request.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block this thread waiting for the user's response!
                // After the user sees the explanation, try again to request the permission.
            }
            // Requesting permission
            else ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    /**
     * Getting approval for permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            // READ_EXTERNAL_STORAGE
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED //READ_EXTERNAL_STORAGE
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED //BODY_SENSORS
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED)//ACCESS_COARSE_LOCATION
                {

                    if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
                        if (grantResults.length <= 0) {
                            Log.i(TAG, "User interaction was cancelled.");
                        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            if (requestingLocationUpdates) {
                                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                                startLocationUpdates();
                            }
                        }
                    }

                    songsList = getPlayList();
                    //Sort abc....
                    Collections.sort(songsList, new Comparator<Song>() {
                        public int compare(Song a, Song b) {
                            return a.getTitle().compareTo(b.getTitle());
                        }
                    });

                    if (songsList.size() > 0) {
                        currentSongName = songsList.get(0).getTitle();
                    }
                    // Create SongList file
                    manageSensorsSongList = new ManageSensors(this, "SongList");
                    manageSensorsSongList.addSongList(songsList);

                    // Create Activity file
                    manageSensorsActivity = new ManageSensors(this, "Activity");
                    writeToActivity("App install");

                    File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File dir = new File(root.getAbsolutePath() + "/temp");
                    dir.mkdirs();
                    File file = new File(dir, "knn.model");

                    try {
                        FileOutputStream outputStream = new FileOutputStream(file);
                        InputStream inputStream = getResources().openRawResource(getResources().getIdentifier("knn", "raw", getPackageName()));
                        IOUtils.copy(inputStream, outputStream);
                        inputStream.close();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //String[] FunctionsName = {"_Avg", "_SD"};
                    DataVector dv = new DataVector();
                    p = new PredictAction(dv.getVectorAttributes(), FunctionsName);

                    // Permission was granted, yay! Do the contacts-related task you need to do.
                } else {
                    // Permission denied, boo! Disable the functionality that depends on this permission.
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }
            }
            // Other 'case' lines to check for other permissions this app might request
        }
    }

    /**
     * Creates array of the song list.
     *
     * @return - array representing the song list.
     */
    public ArrayList<Song> getPlayList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        @SuppressLint("Recycle")
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Get columns
            int columnIndex = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            // Add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisData = musicCursor.getString(columnIndex);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisDuration = utils.milliSecondsToTimer(musicCursor.getLong(durationColumn));

                byte[] PicSong;
                Bitmap songImage = null;
                String genre;
                mediaMetadataRetriever.setDataSource(thisData);
                genre = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                PicSong = mediaMetadataRetriever.getEmbeddedPicture();
                try {
                    songImage = BitmapFactory.decodeByteArray(PicSong, 0, PicSong.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                this.songsList.add(new Song(thisId, thisTitle, thisArtist, thisData, thisAlbum, genre, thisDuration, songImage));
            }
            while (musicCursor.moveToNext());
        }
        return songsList;
    }

    /**
     * Function to play a song
     *
     * @param songIndex - index of song
     */
    public void playSong(int songIndex) {
        // Play song
        try {
            sendToWear.sendMessage("Act", "Start");
            Uri uri = Uri.parse(songsList.get(songIndex).getData());
            lastSongName = currentSongName;
            currentSongName = songsList.get(songIndex).getTitle();
            manageSensorsAcc.setSongName(currentSongName + "-Acc");
            manageSensorsGps.setSongName(currentSongName + "-Gps");
            manageSensorsGravity.setSongName(currentSongName + "-Gravity");
            manageSensorsPressure.setSongName(currentSongName + "-Pressure");
            manageSensorsHeartRate.setSongName(currentSongName + "-HeartRate");
            manageSensorsStepCounter.setSongName(currentSongName + "-StepCounter");
            manageSensorsOrientation.setSongName(currentSongName + "-Orientation");
            manageSensorsMagneticField.setSongName(currentSongName + "-MagneticField");
            manageSensorsRotationVector.setSongName(currentSongName + "-RotationVector");
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Displaying Song title
            String songTitle = songsList.get(songIndex).getTitle();
            songTitleLabel.setText(songTitle);
            if (songsList.get(songIndex).getImage() == null) {
                songImageView.setImageResource(R.drawable.pic_cd);
            } else {
                songImageView.setImageBitmap(songsList.get(songIndex).getImage());
            }

            // Changing Button Image to pause image
            buttonPlay.setImageResource(R.drawable.icon_pause);
            // Set Progress bar values
            seekBar.setProgress(0);
            seekBar.setMax(100);
            // Updating progress bar
            updateProgressBar();
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update timer on seek bar
     */
    public void updateProgressBar() {
        handler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread - updating the seek bar
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration;
            long currentDuration;
            try {
                totalDuration = mediaPlayer.getDuration();
                currentDuration = mediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                totalDuration = 0;
                currentDuration = 0;
            }
            // Displaying Total duration time
            songTotalDuration = " " + utils.milliSecondsToTimer(totalDuration);
            songTotalDurationLabel.setText(songTotalDuration);
            // Displaying time completed playing
            String songCurrentDuration = " " + utils.milliSecondsToTimer(currentDuration);
            songCurrentDurationLabel.setText(songCurrentDuration);
            // Updating seek bar
            progress = (utils.getProgressPercentage(currentDuration, totalDuration));
            seekBar.setProgress(progress);
            // Running this thread after 100 milliseconds
            handler.postDelayed(this, 100);
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mediaPlayer.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);
        manageSensorsActivity.activityFile(currentSongName, songId, songTotalDuration, lastSongName, progress + "% - " + seekBar.getProgress() + "%", "Progress bar");

        // Forward or backward to certain seconds.
        mediaPlayer.seekTo(currentPosition);

        // Update timer progress again.
        updateProgressBar();
    }

    /**
     * Receiving song index from playlist view and play the song
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        requestingLocationUpdates = false;
                        updateLocation();
                        break;
                }
                break;
            case 100:
                songId = Objects.requireNonNull(data.getExtras()).getInt("songTitle");
                // play selected song
                playSong(songId);
                writeToActivity("Choose song");
                break;
        }
    }

    /**
     * Click button from wear
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras().getInt("play") == 1) {
            buttonPlay.performClick();
        }
        if (intent.getExtras().getInt("next") == 2) {
            buttonNext.performClick();
        }
        if (intent.getExtras().getInt("previous") == 3) {
            buttonPrevious.performClick();
        }
        if (intent.getExtras().getInt("backward") == 4) {
            buttonBackward.performClick();
        }
        if (intent.getExtras().getInt("forward") == 5) {
            buttonForward.performClick();
        }
    }

    /**
     * First time when app is start and clicked play or
     * In the song completeness, checks for repeat/shuffle/next , and works accordingly
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (repeat) {
            playSong(songId);
        } else if (shuffle) {
            Random rand = new Random();
            songId = rand.nextInt(songsList.size() - 1);
            playSong(songId);
        } else if (songId < (songsList.size() - 1)) {
            playSong(songId++);
            //songId++;
        } else {
            songId = 0;
            playSong(songId++);
        }
        writeToActivity("Do nothing");
    }

    public void play(View view) {
        // Check for already playing
        if (mediaPlayer.isPlaying()) {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                sendToWear.sendMessage("Act", "stop");
                writeToActivity("Stop");
                // Changing button image to play button
                buttonPlay.setImageResource(R.drawable.icon_play);
            }
        } else {
            // Start or resume song
            if (mediaPlayer != null) {
                mediaPlayer.start();
                sendToWear.sendMessage("Act", "start");
                writeToActivity("Play");
                // Changing button image to pause button
                buttonPlay.setImageResource(R.drawable.icon_pause);
            }
        }
    }

    public void next(View view) {
        // Check if there id a next song.
        if (songId < (songsList.size() - 1)) {
            playSong(songId + 1);
            songId = songId + 1;
        } else {
            // Play first song
            playSong(0);
            songId = 0;
        }
        writeToActivity("next");
    }

    public void previous(View view) {
        if (songId == 1) {
            songId = 0;
            playSong(songId);
        } else if (songId > 0) {
            playSong(songId - 1);
            songId = songId - 1;
        } else {
            // Play last song
            playSong(songsList.size() - 1);
            songId = songsList.size() - 1;
        }
        writeToActivity("previous");
    }


    public void forward(View view) {
        // Get current song position
        int currentPosition = mediaPlayer.getCurrentPosition();
        // Check if seekForward time is lesser than song duration
        int seekForwardTime = 5000;
        if (currentPosition + seekForwardTime <= mediaPlayer.getDuration()) {
            // Forward song
            mediaPlayer.seekTo(currentPosition + seekForwardTime);
        } else {
            // Forward to end position
            mediaPlayer.seekTo(mediaPlayer.getDuration());
        }
        writeToActivity("forward");
    }

    public void backward(View view) {
        // Get current song position
        int currentPosition = mediaPlayer.getCurrentPosition();
        // Check if seekBackward time is greater than 0 sec
        int seekBackwardTime = 5000;
        if (currentPosition - seekBackwardTime >= 0) {
            // Backward song
            mediaPlayer.seekTo(currentPosition - seekBackwardTime);
        } else {
            // Backward to starting position
            mediaPlayer.seekTo(0);
        }
        writeToActivity("Backward");

    }

    public void repeat(View view) {
        if (repeat) {
            repeat = false;
            Toast.makeText(getApplicationContext(), "Repeat of", Toast.LENGTH_SHORT).show();
            writeToActivity("Repeat off");
            buttonRepeat.setImageResource(R.drawable.icon_repeat);
        } else {
            repeat = true;
            Toast.makeText(getApplicationContext(), "Repeat on", Toast.LENGTH_SHORT).show();
            writeToActivity("Repeat on");
            buttonRepeat.setImageResource(R.drawable.icon_repeat_on);
        }
    }

    public void shuffle(View view) {
        if (shuffle) {
            shuffle = false;
            Toast.makeText(getApplicationContext(), "Shuffle off", Toast.LENGTH_SHORT).show();
            writeToActivity("Shuffle off");
            buttonShuffle.setImageResource(R.drawable.icon_shuffle);

        } else {
            shuffle = true;
            Toast.makeText(getApplicationContext(), "Shuffle on", Toast.LENGTH_SHORT).show();
            writeToActivity("Shuffle on");
            buttonShuffle.setImageResource(R.drawable.icon_shuffle_on);
        }
    }

    public void playListClick(View view) {
        Intent i = new Intent(getApplicationContext(), PlayListActivity.class);
        startActivityForResult(i, 100);
    }

    public void predictionClick(View view) {
        Toast toast = Toast.makeText(getApplicationContext(), "Predicting action...", Toast.LENGTH_SHORT);
        toast.show();
        String song = songsList.get(songId).getTitle();
        String res = p.getPrediction(song, appStartingTime, getTimeString());
        Toast toastResult = Toast.makeText(getApplicationContext(), "You should do:" + "\n" + res, Toast.LENGTH_SHORT);
        toastResult.show();
    }

    public void writeToActivity(String activity) {
        manageSensorsActivity.activityFile(currentSongName, songId, songTotalDuration, lastSongName, progress + "%", activity);
    }

    @SuppressLint("DefaultLocale")
    public static String getDateString() {
        calendar = Calendar.getInstance();
        return String.format("%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
    }

    @SuppressLint("DefaultLocale")
    public static String getTimeString() {
        calendar = Calendar.getInstance();
        return String.format("%02d:%02d:%d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }


    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                requestingLocationUpdates = savedInstanceState.getBoolean(KEY_REQUESTING_LOCATION_UPDATES);
            }
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                currentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                lastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }
            updateLocation();
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocation();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    Log.i(TAG, "All location settings are satisfied.");

                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    updateLocation();
                })
                .addOnFailureListener(this, e -> {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " + "location settings ");
                            try {
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.i(TAG, "PendingIntent unable to execute request.");
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings.";
                            Log.e(TAG, errorMessage);
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            requestingLocationUpdates = false;
                    }
                    updateLocation();
                });
    }

    private void updateLocation() {
        if (currentLocation != null) {
            float gpsData[] = {(float) currentLocation.getLatitude(), (float) currentLocation.getLongitude()};
            manageSensorsGps.addSensorData("Gps", gpsData);
        }
    }

    private void stopLocationUpdates() {
        if (!requestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }
        mFusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(this, task -> requestingLocationUpdates = false);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, currentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, lastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
        } else {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    protected void onRestart() {
        //enable the next code to create training File for machine learning:

        /*
        ArithmeticFunctions fun = new ArithmeticFunctions();
        MyLambdaFunction [] LambdaFunctions = {fun.getAvg(),fun.getSD(),fun.getMax(),fun.getMin()};
        String [] FunctionsName = {"_Avg","_SD"};
        */

        /*
        ActivityReader ar = new ActivityReader(appStartingTime);
        ar.extractData(LambdaFunctions, FunctionsName);
        appStartingTime = getTimeString();
        */

        super.onRestart();
        ActivityReader ar = new ActivityReader(appStartingTime);
        ar.updateClassifier(LambdaFunctions, p);
        appStartingTime = getTimeString();
        // res == the action the machine predicts.

    }

    @Override
    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
        updateLocation();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        writeToActivity("Destroy");
    }
}