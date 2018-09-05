package com.example.myplayerwear;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;


public class Orientation implements SensorEventListener {

    private static final String TAG = "Orientation";
    private Sensor MySensor;
    private SensorManager SM;
    private SendToPhone STP;

    public Orientation(Context context) {
        SM = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        MySensor = SM.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        STP = SendToPhone.getInstance(context);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //print on datashow
        try {
            MainActivity.print("Orientation", event);
        } catch (Exception e) {
        }

        STP.sendSensorData(
                event.sensor.getStringType(),
                event.sensor.getType(),
                event.accuracy,
                event.timestamp,
                event.values);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void startMeasurement() {
        // Register sensor Listener
        if (SM != null) {
            if (MySensor != null) {
                SM.registerListener(this, MySensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Orientation found");
            }
        }
    }

    public void stopMeasurement() {
        // unRegister sensor Listener
        if (SM != null) {
            SM.unregisterListener(this);
        }
    }


}
