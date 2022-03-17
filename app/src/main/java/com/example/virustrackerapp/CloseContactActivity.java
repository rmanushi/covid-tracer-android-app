package com.example.virustrackerapp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CloseContactActivity extends AppCompatActivity {
    private BluetoothService bluetoothService;
    private final String TAG = "Close Contact:";
    private boolean connected;
    private BluetoothDevice device;

    //Receiver waiting for broadcasts send by the service.
    private final BroadcastReceiver bluetoothServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothService.ACTION_CONNECTED.equals(action)) {
                UtilityClass.toast(getApplicationContext(),"Connected to remote user.");
                connected = true;
            } else if (BluetoothService.ACTION_DISCONNECTED.equals(action)) {
                UtilityClass.toast(getApplicationContext(),"Disconnected from remote user.");
                connected = false;
            }else if(BluetoothService.ACTION_REQUIRED_CHARACTERISTIC_FOUND.equals(action)){
                UtilityClass.toast(getApplicationContext(),"Found Characteristic");
                bluetoothService.readCharacteristic(bluetoothService.getAppCharacteristic());
            }else if(BluetoothService.ACTION_CHARACTERISTIC_DATA_READ.equals(action)){
                UtilityClass.toast(getApplicationContext(),"ID: " + intent.getStringExtra(BluetoothService.CHARACTERISTIC_DATA));
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

        Button submitBtnCloseContact = (Button) findViewById(R.id.submitCloseContactBtn);
        Button cancelBtnCloseContact = (Button) findViewById(R.id.cancelCloseContactBtn);
        TextView closeContactActivityTv = (TextView) findViewById(R.id.closeContactTv);

        closeContactActivityTv.setText("Are you sure you want to establish close contact with:" + device.getAddress() + " ?");
        submitBtnCloseContact.setOnClickListener(v -> {
            //UtilityClass.toast(this,"Close Contact Established with: " + device.getAddress());
            //finish();
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
    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_REQUIRED_CHARACTERISTIC_FOUND);
        intentFilter.addAction(BluetoothService.ACTION_CHARACTERISTIC_DATA_READ);
        return intentFilter;
    }


}