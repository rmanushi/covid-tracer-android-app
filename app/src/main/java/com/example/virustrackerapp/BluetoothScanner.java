package com.example.virustrackerapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
    private ArrayList<String> alreadyDiscoveredMacAddresses;

    public BluetoothScanner(MainActivity mainActivity, String filterStringUUID) {
        this.mainActivity = mainActivity;
        rssiValue = -55;
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
        alreadyDiscoveredMacAddresses = new ArrayList<String>();
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
            /*
             The array list containing the mac addresses of discovered devices
             during a bluetooth scan session will be used when specific broadcasts
             are sent from the async background task of CloseContactInsert found
             inside the CloseContactActivity class. Such broadcasts will identify
             other remote users to which the current user has:
             a) Already connected before.
             a) Has connected successfully at the present time.

             The BroadcastReceiver in the MainActivity will make
             use of the intent filter in order to find such broadcasts and remove them
             from the UI and list of discovered devices during the specific bluetooth instance.
             When the scanning process is stopped the list of mac addresses in reset meaning that
             these user will be shown again in the UI, but in case the user will attempt
             close contact establishment with them again same procedure will follow.
             */

            if(rssiValue <= result.getRssi()){
                BluetoothDevice device = result.getDevice();
                String address = device.getAddress();
                if(!alreadyDiscoveredMacAddresses.contains(address)){
                    alreadyDiscoveredMacAddresses.add(address);
                    mainActivity.addDevice(device);
                }
            }
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
        alreadyDiscoveredMacAddresses.clear();
        bleScanner.stopScan(scanCallback);
    }

}
