package com.heu.esenseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Spinner;
import android.widget.Toast;

import com.heu.esenseapp.io.esense.esenselib.ESenseConfig;
import com.heu.esenseapp.io.esense.esenselib.ESenseConnectionListener;
import com.heu.esenseapp.io.esense.esenselib.ESenseData;
import com.heu.esenseapp.io.esense.esenselib.ESenseEvent;
import com.heu.esenseapp.io.esense.esenselib.ESenseManager;
import com.heu.esenseapp.io.esense.esenselib.ESenseSensorListener;

import java.util.ArrayList;
import java.util.List;

//todo onRequestPermissionsResult handles dynamic permission callback logic, add permission judgment before each button event, if there is no permission, then apply for permission dynamically

//Use AudioRecord for recording, you need to implement the file stream reading and writing details, but you can directly save it as wav file format, refer to the following link
// https://blog.csdn.net/chezi008/article/details/53064604
// https://github.com/dgutkai/BTRecorder
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ESenseConnectionListener, ESenseSensorListener {


    private Spinner userSpinner;
    private Spinner voiceSpinner;
    private ArrayAdapter<String> userListAdapter;
    private ArrayAdapter<String> voiceListAdapter;

    private List<String> userList;
    private List<String> voiceList;

    private String userType;// Record the selected user
    private String voiceType;// Record the selected recording type

    private Button leftButton;
    private Button readSettingButton;
    private Button startRecordButton;
    private Button stopRecordButton;
    private Chronometer timer;
    private ESenseManager manager;
    private BluetoothAdapter mBluetoothAdapter;
    private ESenseConfig config;
    private CsvUtil.LooperThread csvThread;
    private long startTime;// Record the start time of data collection
    private int offsetX, offsetY, offsetZ;//


    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        leftButton = findViewById(R.id.left_button);
        readSettingButton = findViewById(R.id.read_settin_button);
        startRecordButton = findViewById(R.id.start_record_button);
        stopRecordButton = findViewById(R.id.stop_record_button);
        timer = (Chronometer) findViewById(R.id.timer);
        userSpinner = (Spinner) findViewById(R.id.user_type);
        voiceSpinner = (Spinner) findViewById(R.id.voice_type);

        userList = new ArrayList<>();
        voiceList = new ArrayList<>();

        userList.add(getString(R.string.user1));
        userList.add(getString(R.string.user2));
        userList.add(getString(R.string.user3));
        userList.add(getString(R.string.user4));
        userList.add(getString(R.string.user5));
        userList.add(getString(R.string.user6));
        userList.add(getString(R.string.user7));
        userList.add(getString(R.string.user8));
        userList.add(getString(R.string.user9));
        userList.add(getString(R.string.user10));

        voiceList.add(getString(R.string.Hey_Siri));
        voiceList.add(getString(R.string.OK_Google));
        voiceList.add(getString(R.string.Alexa));

        // Set the select user drop-down menu
        userListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userList);
        // Set the selection recording drop-down menu
        voiceListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, voiceList);

        userListAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        voiceListAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);

        userSpinner.setAdapter(userListAdapter);
        voiceSpinner.setAdapter(voiceListAdapter);

        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                userType = parent.getItemAtPosition(position).toString();
                Log.d("user type:", userType);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("user type:", userType);

            }
        });

        voiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                voiceType = parent.getItemAtPosition(position).toString();
                Log.e("voice type", voiceType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e("voice type", voiceType);
            }
        });

        userSpinner.setEnabled(true);
        voiceSpinner.setEnabled(true);


        leftButton.setOnClickListener(this);
        readSettingButton.setOnClickListener(this);
        startRecordButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        registerBluetoothReceiver();
        requestPermissions();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.left_button://Clicked the "Scan Left Earphone" button
                if (null != manager && manager.isConnected())
                    manager.disconnect();
                showDialog();
                manager = new ESenseManager(Constants.leftEarbudName, MainActivity.this.getApplicationContext(), this);
                connect();
                Log.d(Constants.TAG, "------ you start to find " + Constants.leftEarbudName + " ------");
                break;
            case R.id.read_settin_button://Before reading the configuration information and starting to record the data, be sure to read the configuration information and save it to the config object, which is used to convert the IMU data collected later
                readSetting();
                break;

            case R.id.start_record_button:
                if (null == this.config) {
                    Toast.makeText(MainActivity.this, "\n" + "Please click \"Read and Set Configuration Information \\\"", Toast.LENGTH_SHORT).show();
                    return;
                }
                CsvUtil.open(userType, voiceType);
                csvThread = new CsvUtil.LooperThread();
                csvThread.start();
                BluetoothUtil.getInstance(this).openSco(new BluetoothUtil.IBluetoothConnectListener() {
                    @Override
                    public void onError(String error) {
                        Log.e(Constants.TAG, "openSco onError  error=" + error);
                        Toast.makeText(MainActivity.this, "Failed to open Bluetooth recording, is the right earphone not connected?", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess() {
                        //Start recording task here
                        Toast.makeText(MainActivity.this, "Start Bluetooth recording", Toast.LENGTH_SHORT).show();
                        startRecording();
                    }
                });
                break;

            case R.id.stop_record_button:
                long endTime = System.currentTimeMillis();
                float time = (endTime - startTime) / 1000.0f;//Calculate acquisition time
                stopRecording(time);
                timer.stop();
                break;

            default:
                break;
        }
    }

    private void showDialog() {
        if (null == dialog) {
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
        }
        dialog.show();
    }

    // Read headset configuration information
    private void readSetting() {
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean test1 = manager.getSensorConfig();//Before reading the configuration information and starting to record the data, be sure to read the configuration information and save it to the config object, which is used to convert the IMU data collected later
                Log.d(Constants.TAG, "getSensorConfig = " + test1);
/*                //Pause 400ms and call manager.getAdvertisementAndConnectionInterval (), otherwise the get may fail。
                // Because you need to wait for the last get operation to return data before you can exchange data with eSense, otherwise the following get may fail.
                // For more information, please visit esense.io for official documents
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean test = manager.getAdvertisementAndConnectionInterval();//Can be deleted
                Log.d(Constants.TAG, "getAdvertisementAndConnectionInterval = " + test);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null != dialog && dialog.isShowing()) {
                            dialog.dismiss();
                        }

                    }
                });*/
            }
        }).start();
    }

    //Call the connect () method to connect the left headset failed callback
    @Override
    public void onDeviceFound(ESenseManager manager) {
        Log.d(Constants.TAG, "------ the eSense earbud is found ------");
        //Toast.makeText(this, "------ the eSense earbud is found ------", Toast.LENGTH_SHORT).show();
    }

    // Call the connect () method to connect the callback of the left headset successfully
    @Override
    public void onDeviceNotFound(ESenseManager manager) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != dialog && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "No left earphone scanned", Toast.LENGTH_SHORT).show();
            }
        });
        Log.d(Constants.TAG, "------ the eSense earbud is not found ------");
    }


    //Call the connect () method to connect the callback of the left headset successfully
    @Override
    public void onConnected(ESenseManager manager) {
        Log.d(Constants.TAG, "------ the eSense earbud is successfully connected ------");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != dialog && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "\n" +"Successfully connected to the left headset", Toast.LENGTH_SHORT).show();
            }
        });

        manager.registerSensorListener(this, Constants.sampleingRate);//Set the listener to listen to the callback of IMU data

        manager.registerEventListener(new ESenseEventListenerImpl(this));//Set callbacks for other methods

        //manager.getSensorConfig();
        unregisterReceiver(bluetoothReceiver);
    }

    //transfer registerSensorListener() After the method is successful, this callback will be triggered automatically when the headset is disconnected
    @Override
    public void onDisconnected(ESenseManager manager) {
        Log.d(Constants.TAG, "------ the eSense earbud is disconnected ------");
    }

    //After calling registerSensorListener () method successfully, this method will be called back according to the set sampling rate.
    @Override
    public void onSensorChanged(ESenseEvent evt) {

        // IO read and write is very slow, if you directly write the IMU data into the file, there will be data loss. Therefore, use sub-threads to read and write data, and use handlers to send data to sub-threads to write to files        if (Constants.recordImuData) {
        if (Constants.recordImuData) {
            Message msg = Message.obtain();
            Bundle data = new Bundle();
            data.putParcelable("ESENSE_DATA", new ESenseData(evt.getTimestamp(), evt.convertAccToG(config, new int[]{offsetX, offsetY, offsetZ}), evt.convertGyroToDegPerSecond(config)));
            msg.setData(data);
            msg.what = Constants.MSG_TYPE_RECEIVE_DATA;
            csvThread.handler.sendMessage(msg);
        }


    }

    /**
     * Android 6.0 and above Dynamically apply for authorization information permission
     */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // BLE connection requires location permission
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                //Request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        Constants.REQUEST_ACCESS_COARSE_LOCATION_PERMISSION);
            }

            // Save data to CSV requires write permission
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_EXTERNAL_STORAGE);
            }

            // You need to apply for recording permission
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                //Request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        Constants.REQUEST_RECORD_AUDIO);
            }

        }
    }


    //todo
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_ACCESS_COARSE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User authorization
            } else {
                finish();
            }
        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    // Turn on Bluetooth
    private void connect() {
        if (mBluetoothAdapter.isEnabled()) {
            manager.connect(10000);
        } else {
            mBluetoothAdapter.enable();
        }
    }

    // Register to monitor Bluetooth status change broadcast
    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = mBluetoothAdapter.getState();
                if (state == BluetoothAdapter.STATE_ON) {
                    manager.connect(10000);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        manager.unregisterEventListener();
        manager.unregisterSensorListener();
        if (manager.isConnected())
            manager.disconnect();
        super.onDestroy();
    }


    // Click the start button to trigger
    private void startRecording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RecordUtil.recordTask(MainActivity.this, voiceType, userType);
            }
        }).start();

    }

    private void stopRecording(float time) {
        Message msg = Message.obtain();
        msg.what = Constants.MSG_TYPE_STOP;
        msg.obj = time;
        msg.arg1 = Constants.sampleingRate;
        csvThread.handler.sendMessage(msg);
        Constants.recordImuData = false;
    }

    public void setConfig(ESenseConfig config) {
        this.config = config;
        boolean result = manager.setAdvertisementAndConnectiontInterval(1000, 2000, 20, 40);
        Log.d(Constants.TAG, "setAdvertisementAndConnectiontInterval = " + result);
        if (result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (null != dialog && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Reading and setting configuration information succeeded ", Toast.LENGTH_SHORT).show();
                }
            });
        } else
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "\n" +"Failed to read and set configuration information", Toast.LENGTH_SHORT).show();
                }
            });

    }

    public void setOffSet(int offsetX, int offsetY, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }


    public void notifyRecording() {

        startTime = System.currentTimeMillis();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timer.setBase(SystemClock.elapsedRealtime());// The timer is cleared
                timer.start();
            }
        });
    }
}