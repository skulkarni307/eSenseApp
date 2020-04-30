package com.heu.esenseapp;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

// Data recording tools
public class RecordUtil {



    // This class stores static variables, use a private constructor to avoid class initialization
    private RecordUtil(){

    }

    public static AudioDeviceInfo findAudioDevice(Context context,int deviceFlag, int deviceType) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adis = manager.getDevices(deviceFlag);
        Log.i(Constants.TAG, "findAudioDevice: adis = " + adis);
        for (AudioDeviceInfo adi : adis) {
            Log.i(Constants.TAG, "findAudioDevice: adi.getType() = " + adi.getType());
            if (adi.getType() == deviceType) {
                return adi;
            }
        }
        return null;
    }

    // Recording thread
    public static void recordTask(MainActivity activity, String voiceType, String userType) {
        Log.i(Constants.TAG, "RecordTask2: AsyncTask");

        try {
            final String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.dirname + voiceType + "/" + userType + ".pcm";
            File file = new File(fileName);
            if (file.exists() && file.isFile()){
                file.delete();
            }
            // Open the output stream to the specified file
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            // According to several defined configurations, to obtain the appropriate buffer size
            final int bufferSize = AudioRecord.getMinBufferSize(Constants.frequence, Constants.channelConfig, Constants.audioEncoding);
            //int bufferSize = 640;
            Log.i(Constants.TAG, "RecordTask: dataSize=" + bufferSize);//1280
            // Instantiate AudioRecord // MediaRecorder.AudioSource.VOICE_COMMUNICATION
            //AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, frequence, channelConfig, audioEncoding, bufferSize);
            //AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, frequence, channelConfig, audioEncoding, bufferSize);
            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, Constants.frequence, Constants.channelConfig, Constants.audioEncoding, bufferSize);
            AudioDeviceInfo audioDevice = RecordUtil.findAudioDevice(activity, AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
            if (null != audioDevice) {
                Log.i(Constants.TAG, "RecordTask: audioDevice = " + audioDevice.getType());
                record.setPreferredDevice(audioDevice);
            }

            //start recording
            record.startRecording();
            Constants.recordImuData = true;
            activity.notifyRecording();//Notify activity to set related variables

            byte audioData[] = new byte[bufferSize];

            // Define the loop and judge whether to continue recording based on the value of recordImuData
            while (Constants.recordImuData) {
                // Read bytes from bufferSize and return the number of shorts read
                int number = record.read(audioData, 0, bufferSize);
                dos.write(audioData, 0, number);
                //Log.i(Constants.TAG, "audioData number=" + number);
//                    System.out.println(Arrays.toString(audioData));
            }

            // End of recording
            record.stop();
            record.release();
            BluetoothUtil.getInstance(activity).closeSco();
            dos.flush();
            dos.close();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final String wavFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.dirname + voiceType + "/" + userType + ".wav";
            Log.d(Constants.TAG, "File stored in = " + wavFileName);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    WavUtils.convertWaveFile(fileName, wavFileName, Constants.frequence, bufferSize);
                }
            }).start();

            Log.i(Constants.TAG, "RecordTask2: over");
            Log.v(Constants.TAG, "The DOS available:" + file.getAbsolutePath());
            Log.v(Constants.TAG, "The DOS available:" + file.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}