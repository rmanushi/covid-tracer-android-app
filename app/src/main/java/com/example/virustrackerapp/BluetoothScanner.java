package com.example.virustrackerapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

//Class to represent the bluetooth scanner for the application.
public class BluetoothScanner {
    private boolean scanning;
    private Handler handler;
    private MainActivity mainActivity;
    private long scanPeriod;
    //Used for distance measurement purposes.
    private int rssiValue;
    private BluetoothLeScanner bleScanner;

    public BluetoothScanner(MainActivity mainActivity, long scanPeriod, int rssiValue) {
        this.mainActivity = mainActivity;
        this.scanPeriod = scanPeriod;
        this.rssiValue = rssiValue;

        handler = new Handler();
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
            mainActivity.add(result.getDevice());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            UtilityClass.toast(mainActivity.getApplicationContext(),"Scanner failed, error code: "+ errorCode +".");
        }
    };

    public void scanForDevices(boolean activate){
        if(activate && !scanning){
            UtilityClass.toast(mainActivity.getApplicationContext(),"Scanning started...");

            handler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    UtilityClass.toast(mainActivity.getApplicationContext(),"Scanning stopped by handler!");
                    scanning = false;
                    bleScanner.stopScan(scanCallback);
                }
            },scanPeriod);

            scanning = true;
            bleScanner.startScan(scanCallback);
        }else{
            scanning = false;
            bleScanner.stopScan(scanCallback);
        }
    }


    public void startScanner(){
        if(!UtilityClass.checkBluetoothStatus()){
            UtilityClass.requestBluetoothActivation(mainActivity);
        }else{
            scanForDevices(true);
        }


    }

    public void stopScanner(){
        UtilityClass.toast(mainActivity.getApplicationContext(),"Scanning stopped by User.");
        scanForDevices(false);
    }

}
