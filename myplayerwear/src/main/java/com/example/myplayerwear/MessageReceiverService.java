package com.example.myplayerwear;

import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class MessageReceiverService extends WearableListenerService {

    private static final String TAG1 = "GETING DATA";
    private static final String TAG2 = "GETING MESSAGE";
    private boolean Playing = false;
    private ManageOfSensors manageOfSensors = ManageOfSensors.getInstance(this);


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        // Log.e(TAG1, "onDatadddddddddddddddddddddddddddddddddChanged()");

//        for (DataEvent dataEvent : dataEvents) {
//            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
//                DataItem dataItem = dataEvent.getDataItem();
//                Uri uri = dataItem.getUri();
//                String path = uri.getPath();
//
//                if (path.startsWith("/filter")) {
//                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
//                    int filterById = dataMap.getInt("filter");
//                    deviceClient.setSensorFilter(filterById);
//                }
//            }
//        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("Act")) {
            if ((new String(messageEvent.getData())).equals("start") && !Playing) {
                Log.e(TAG2, "Received Path: " + messageEvent.getPath());
                Log.d(TAG2, "Received message: " + new String(messageEvent.getData()));
                Playing = true;
                manageOfSensors.StartAllSensors();
            } else if ((new String(messageEvent.getData())).equals("stop")) {
                manageOfSensors.StopAllSensors();
                Log.e(TAG2, "Received Path: " + messageEvent.getPath());
                Log.d(TAG2, "Received message: " + new String(messageEvent.getData()));
            }
        }
    }


}
