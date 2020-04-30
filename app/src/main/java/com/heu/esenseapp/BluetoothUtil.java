package com.heu.esenseapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;


// Bluetooth tools
public class BluetoothUtil {

    private String TAG = "BluetoothUtil";

    private static BluetoothUtil mBluetoothUtil;

    // The number of continuous connections when the first time sco is unsuccessful
    private static final int SCO_CONNECT_TIME = 5;
    private int mConnectIndex = 0;

    private AudioManager mAudioManager = null;
    static Context mContext;

    private BluetoothUtil() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
    }

    public static BluetoothUtil getInstance(Context context) {
        mContext = context;
        if (mBluetoothUtil == null) {
            mBluetoothUtil = new BluetoothUtil();
        }
        return mBluetoothUtil;
    }

    public void openSco(final IBluetoothConnectListener listener) {
        if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
            Log.e(TAG, "\n" + "The system does not support Bluetooth recording");
            listener.onError("Your device no support bluetooth record!");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                // The key to Bluetooth recording is to start the SCO connection before the headset microphone works
                mAudioManager.stopBluetoothSco();
                mAudioManager.startBluetoothSco();
                // It takes time to establish a Bluetooth SCO connection. After the connection is established, an ACTION_SCO_AUDIO_STATE_CHANGED message will be issued, and the subsequent logic will be entered by receiving this message.
                // It is also possible that the SCO has been established at this time, you will not receive the above message, you can start before BlueToothSco ()
                //stopBluetoothSco()
                mConnectIndex = 0;
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                        boolean bluetoothScoOn = mAudioManager.isBluetoothScoOn();
                        Log.i(TAG, "onReceive state=" + state + ",bluetoothScoOn=" + bluetoothScoOn);
                        if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {// Determine whether the value is: 1
                            Log.e(TAG, "onReceive success!");
                            mAudioManager.setBluetoothScoOn(true);// Open SCO
                            listener.onSuccess();
                            mContext.unregisterReceiver(this); // Cancel the broadcast, don't miss it
                        } else {// Wait for one second before trying to start SCO
                            Log.e(TAG, "onReceive failed index=" + mConnectIndex);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (mConnectIndex < SCO_CONNECT_TIME) {
                                mAudioManager.startBluetoothSco();// Try to connect again
                            } else {
                                listener.onError("open sco failed!");
                                mContext.unregisterReceiver(this);  // Cancel the broadcast, don't miss it
                            }
                            mConnectIndex++;
                        }
                    }
                }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            }
        }).start();

    }


    public void closeSco() {
        boolean bluetoothScoOn = mAudioManager.isBluetoothScoOn();
        Log.i(TAG, "bluetoothScoOn=" + bluetoothScoOn);
        if (bluetoothScoOn) {
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
        }
        mBluetoothConnectListener = null;
    }

    public interface IBluetoothConnectListener {
        void onError(String error);

        void onSuccess();
    }

    IBluetoothConnectListener mBluetoothConnectListener;
}
