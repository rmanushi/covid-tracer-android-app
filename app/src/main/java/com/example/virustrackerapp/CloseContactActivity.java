package com.example.virustrackerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.TextView;

public class CloseContactActivity extends AppCompatActivity {
    private Button submitBtnCloseContact, cancelBtnCloseContact;
    private TextView closeContactActivityTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_contact);

        submitBtnCloseContact = (Button) findViewById(R.id.submitCloseContactBtn);
        cancelBtnCloseContact = (Button) findViewById(R.id.cancelCloseContactBtn);
        closeContactActivityTv = (TextView) findViewById(R.id.closeContactTv);

        //Getting device passed by main activity.
        Intent intent = getIntent();
        BluetoothDevice device = intent.getExtras().getParcelable("Device");
        closeContactActivityTv.setText("Are you sure you want to establish close contact with:" + device.getAddress() + " ?");
        submitBtnCloseContact.setOnClickListener(v -> {
            UtilityClass.toast(this,"Close Contact Established with: " + device.getAddress());
            finish();
        });

        cancelBtnCloseContact.setOnClickListener(v -> {
           finish();
        });

    }
}