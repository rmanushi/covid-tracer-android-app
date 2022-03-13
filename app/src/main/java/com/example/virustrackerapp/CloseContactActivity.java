package com.example.virustrackerapp;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CloseContactActivity extends AppCompatActivity {
    private BluetoothClient bluetoothClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_contact);

        Button submitBtnCloseContact = (Button) findViewById(R.id.submitCloseContactBtn);
        Button cancelBtnCloseContact = (Button) findViewById(R.id.cancelCloseContactBtn);
        TextView closeContactActivityTv = (TextView) findViewById(R.id.closeContactTv);

        //Getting device passed by main activity.
        Intent intent = getIntent();
        BluetoothDevice device = intent.getExtras().getParcelable("Device");
        closeContactActivityTv.setText("Are you sure you want to establish close contact with:" + device.getAddress() + " ?");
        submitBtnCloseContact.setOnClickListener(v -> {
            UtilityClass.toast(this,"Close Contact Established with: " + device.getAddress());
            bluetoothClient = new BluetoothClient(this, device, getString(R.string.service_uuid), getString(R.string.characteristic_uuid));
            finish();
        });

        cancelBtnCloseContact.setOnClickListener(v -> finish());
    }
}