package com.example.myplayerwear;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;


public class MagneticField implements SensorEventListener {


    private static final String TAG = "MagneticField";

    private Sensor mySensor;
    private SensorManager SM;
    private SendToPhone STP;

    public MagneticField(Context context) {
        SM = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        mySensor = SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        STP = SendToPhone.getInstance(context);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            DataShow.print("MagneticField", event);
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
            if (mySensor != null) {
                SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Magnetic Field found");
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
