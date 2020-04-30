package com.heu.esenseapp;

import android.util.Log;

import com.heu.esenseapp.io.esense.esenselib.ESenseConfig;
import com.heu.esenseapp.io.esense.esenselib.ESenseEventListener;

// The implementation class of ESenseEventListener
public class ESenseEventListenerImpl implements ESenseEventListener {
    private MainActivity activity;
    public ESenseEventListenerImpl(MainActivity activity){
        this.activity=activity;
    }
    // Call getBatteryVoltage () for successful callback
    @Override
    public void onBatteryRead(double voltage) {
        Log.d(Constants.TAG, "------ voltage is " + String.valueOf(voltage) + "------");
    }

    // When using the registerEventListener () method to listen to the headset button event, this method will be called back if the headset button is clicked
    @Override
    public void onButtonEventChanged(boolean pressed) {
        // The button click event of the headset sometimes fails to return, or the delay is very large, a few seconds
        Log.d(Constants.TAG, "------ A button event is triggered ------" + (pressed == true));
    }

    // Call getAdvertisementAndConnectionInterval () successful callback
    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval, int maxAdvertisementInterval, int minConnectionInterval, int maxConnectionInterval) {
        Log.d(Constants.TAG, "------ AdvertisementInterval range is ( " + String.valueOf(minAdvertisementInterval) + "---" + String.valueOf(maxAdvertisementInterval) + ") ------");
        Log.d(Constants.TAG, "------ ConnectionInterval range is ( " + String.valueOf(minConnectionInterval) + "---" + String.valueOf(maxConnectionInterval) + ") ------");
    }

    // Call getDeviceName () for successful callback
    @Override
    public void onDeviceNameRead(String deviceName) {
        Log.d(Constants.TAG, "------ the device name is " + deviceName + " ------");
    }

    //
    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        Log.d(Constants.TAG, "------ read Sensor config ------");
        activity.setConfig(config);
        //manager.setSensorConfig(config);
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
        activity.setOffSet(offsetX, offsetY, offsetZ);


        //Log.d(Constants.TAG, "offsetX = " + offsetX / config.getAccSensitivityFactor() + "; offsetY = " + offsetY / config.getAccSensitivityFactor() + "; offsetZ = " + offsetZ / config.getAccSensitivityFactor());
    }
}
