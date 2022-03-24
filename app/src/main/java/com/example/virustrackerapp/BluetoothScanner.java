package com.example.virustrackerapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.UUID;

//Class to represent the bluetooth scanner for the application.
public class BluetoothScanner {
    private MainActivity mainActivity;
    //Used for distance measurement purposes.
    private int rssiValue;
    private ParcelUuid filterUUID;
    private BluetoothLeScanner bleScanner;
    private ArrayList<ScanFilter> filters;
    private ScanFilter scanFilter;
    private ScanSettings scanSettings;

    public BluetoothScanner(MainActivity mainActivity, String filterStringUUID) {
        this.mainActivity = mainActivity;
        rssiValue = -90;
        filterUUID = new ParcelUuid(UUID.fromString(filterStringUUID));
        filters = new ArrayList<ScanFilter>();
        //Filtering results by service UUID.
        scanFilter = new ScanFilter
                .Builder()
                .setServiceUuid(filterUUID)
                .build();

        filters.add(scanFilter);

        scanSettings = new ScanSettings
                .Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        //Initiating default bluetooth le scanner.
        bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    }

    private ScanCallback scanCallback = new ScanCallback() {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered. Could be one of {@link
         *                     ScanSettings#CALLBACK_TYPE_ALL_MATCHES}, {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mainActivity.addDevice(result.getDevice());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            UtilityClass.toast(mainActivity.getApplicationContext(),"Scanner failed, error code: "+ errorCode +".");
        }
    };

    public void startScanner(){
        if(!UtilityClass.checkBluetoothStatus()){
            UtilityClass.requestBluetoothActivation(mainActivity);
        }else{
            bleScanner.startScan(filters,scanSettings,scanCallback);
        }
    }

    public void stopScanner(){
        bleScanner.stopScan(scanCallback);
    }

}
