package com.heu.esenseapp;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.heu.esenseapp.io.esense.esenselib.ESenseData;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Tools for manipulating CSV files
 */
public class CsvUtil {

    public static final String mComma = ",";
    public static final String TAG = "HEU-IOT-eSense";

    private static String mImuFileName = null;
    private static BufferedOutputStream mImuOutputStream;
    private int i=0;

    public static void open(String userStr, String voiceStr) {
        String folderName = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path != null) {
                folderName = path + Constants.dirname+voiceStr+"/";
            }
        }

        File fileRobo = new File(folderName);
        if (!fileRobo.exists()) {
            fileRobo.mkdirs();
        }
        mImuFileName = folderName + userStr + ".csv";
        File csvFile = new File(mImuFileName);
        if (csvFile.exists() && csvFile.isFile()){
            csvFile.delete();
        }
        try {
            mImuOutputStream = new BufferedOutputStream(new FileOutputStream(csvFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void writeCsv(long timesamp, double accel_x, double accel_y, double accel_z, double gyro_x, double gyro_y, double gyro_z) {
        StringBuilder mImuStringBuilder = new StringBuilder();
        mImuStringBuilder = new StringBuilder();
        mImuStringBuilder.append(timesamp);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(accel_x);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(accel_y);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(accel_z);
        mImuStringBuilder.append(mComma);

        mImuStringBuilder.append(gyro_x);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(gyro_y);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(gyro_z);
        mImuStringBuilder.append("\n");
        try {
            mImuOutputStream.write(mImuStringBuilder.toString().getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void flush() {
        if (mImuOutputStream != null) {
            try {
                mImuOutputStream.flush();
                mImuOutputStream.close();
                mImuOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // IO read and write is very slow, if you directly write the IMU data into the file, there will be data loss. So use sub-threads to read and write data, use handlers to communicate with the main thread
    public static final class LooperThread extends Thread {
        int i=0;
        public Handler handler;
        //private int i = 0;

        @Override
        public void run() {
            super.run();

            Looper.prepare();

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    i = i + 1;
                    //Log.d(TAG, String.valueOf(i));
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case Constants.MSG_TYPE_STOP:
                            float time = (float) msg.obj;
                            Log.d(TAG, "Time = " + time +"ms; "+" imu " + "Data collection times =  "+i+";" + "Set sampling frequency = " + msg.arg1 +"Hz; " + "Actual acquisition frequency = "+ i/time+"Hz");
                            flush();
                            break;

                        case Constants.MSG_TYPE_RECEIVE_DATA:
                            Bundle bundle = msg.getData();
                            ESenseData data = bundle.getParcelable("ESENSE_DATA");
                            writeCsv(data.getTimestamp(), data.accel[0], data.accel[1], data.accel[2], data.gyro[0], data.gyro[1], data.gyro[2]);
                            //Log.d(TAG, data.accel[0]+"--"+data.gyro[0]);
                            break;
                    }
                }
            };
            Looper.loop();// loop () will call the handleMessage (Message msg) method of the handler, so write it below;
        }
    }
}
