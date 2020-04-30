package com.heu.esenseapp;

import android.media.AudioFormat;

public class Constants {
    // This class stores all static variables, use a private constructor to avoid class initialization
    private Constants(){

    }

    public static final String TAG = "HEU-IOT-eSense";
    // Dynamic permissions request code
    public static final int REQUEST_ACCESS_COARSE_LOCATION_PERMISSION = 0x43;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 0x44;
    public static final int REQUEST_RECORD_AUDIO = 0x45;

    //left: eSense-0808 ;  right: eSense-1153
    // About the ID of the headset, you can use nRF Connect (an APP). Only the ID of the corresponding headset can be used to connect the BLE of the headset (for details, please visit esense.io for the official document)
    // The left earphone is equipped with an IMU sensor, using BLE (Bluetooth Low Energy), and the right ear is collecting microphone data (using traditional Bluetooth), so just connect the left earphone here.
    // Practical use: Turn on the mobile phone's Bluetooth to connect the right earphone 1153, then use this APP to connect the left earphone. In this order, you can use the right headset to collect microphone data, and the left headset to collect IMU data.
    // Otherwise, the left earphone may collect the IMU and microphone at the same time, which will cause the IMU acquisition rate to be much lower than the set value.
    public static final String leftEarbudName = "eSense-0808";


    // Set the directory name where the data file is stored
    public static final String dirname="/0ESenseData/";

    // Set the IMU sampling frequency
    public static final int sampleingRate = 100;

    // handler message type: stop collecting data
    public static final int MSG_TYPE_STOP=0;

    // handler message type: receive data, write to file
    public static final int MSG_TYPE_RECEIVE_DATA=1;

    public static final int frequence = 8000; //Sound recording frequency, unit Hz.
    public static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;//Mono
    public static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;// Save format

    // Whether to record data
    public static boolean recordImuData= false;





}
