package com.example.virustrackerapp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CloseContactActivity extends AppCompatActivity {
    private BluetoothService bluetoothService;
    private BluetoothDevice device;
    private String data;
    private Button submitBtnCloseContact;
    private Button cancelBtnCloseContact;
    private TextView closeContactActivityTv;
    private Handler connectionHandler = new Handler();
    public static final String CONNECTION_TO_DEVICE_FAILED = "com.example.virustrackerapp.CONNECTION_TO_DEVICE_FAILED";
    private Runnable connectionTimeoutOperation = new Runnable() {
        @Override
        public void run() {
            //In case the app is not able to connect to the other user,
            // the activity is stopped and the connection is closed.
            //A broadcast it to be sent that will be received by the main activity in order to update the available device list.
            Intent connectionFailedIntent = new Intent(CONNECTION_TO_DEVICE_FAILED);
            connectionFailedIntent.putExtra("unavailableDevice",device);
            UtilityClass.toast(getApplicationContext(),"This user is no longer connectable.");
            sendBroadcast(connectionFailedIntent);
            finish();
        }
    };
    private final int TIMEOUT_LIMIT = 5000;


    //Receiver waiting for broadcasts send by the service.
    private final BroadcastReceiver bluetoothServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_CONNECTED.equals(action)) {
                UtilityClass.toast(getApplicationContext(),"Connected to remote user.");
                //In case the connection is achieved the handler timeout operation is discarded.
                connectionHandler.removeCallbacks(connectionTimeoutOperation);
            } else if (BluetoothService.ACTION_DISCONNECTED.equals(action)) {
                UtilityClass.toast(getApplicationContext(),"Disconnected from remote user.");
            }else if(BluetoothService.ACTION_REQUIRED_CHARACTERISTIC_FOUND.equals(action)){
                bluetoothService.readCharacteristic(bluetoothService.getAppCharacteristic());
            }else if(BluetoothService.ACTION_CHARACTERISTIC_DATA_READ.equals(action)){
                data = intent.getStringExtra(BluetoothService.CHARACTERISTIC_DATA);
                closeContactActivityTv.setText("Are you sure you want to establish close contact with:" + device.getAddress() + " ?");
                submitBtnCloseContact.setEnabled(true);
            }
        }
    };

    //Establishing a connection for the required service.
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((BluetoothService.LocalBinder) service).getService();
            if(bluetoothService != null){
                if(!bluetoothService.initialize()){
                    UtilityClass.toast(getApplicationContext(),"Unable to initialize bluetooth.");
                    finish();
                }
                //Checking for timeout while attempting connection to the other user.
                connectionHandler.postDelayed(connectionTimeoutOperation, TIMEOUT_LIMIT);
                bluetoothService.connect(device);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_contact);

        //Getting device passed by main activity.
        Intent intent = getIntent();
        device = intent.getExtras().getParcelable("Device");

        submitBtnCloseContact = (Button) findViewById(R.id.submitCloseContactBtn);
        cancelBtnCloseContact = (Button) findViewById(R.id.cancelCloseContactBtn);
        closeContactActivityTv = (TextView) findViewById(R.id.closeContactTv);

        submitBtnCloseContact.setEnabled(false);
        closeContactActivityTv.setText("Please wait app is trying to note down remote user id.");


        submitBtnCloseContact.setOnClickListener(v -> {
            //UtilityClass.toast(this,"Close Contact Established with: " + device.getAddress());
            //
            UtilityClass.toast(this,"Close Contact Established with: " + data);
            finish();
        });

        //Disconnecting from the remote device once the user presses cancel.
        cancelBtnCloseContact.setOnClickListener(v -> {
            bluetoothService.disconnect();
            finish();
        });

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        registerReceiver(bluetoothServiceBroadcastReceiver, intentFilter());
    }

    //Operations that take place when the activity finishes.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregisterReceiver(bluetoothServiceBroadcastReceiver);
        bluetoothService = null;
    }

    //Filter set up to differentiate between the broadcasts sent by the service.
    private IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_REQUIRED_CHARACTERISTIC_FOUND);
        intentFilter.addAction(BluetoothService.ACTION_CHARACTERISTIC_DATA_READ);
        return intentFilter;
    }
}