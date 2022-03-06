package com.example.virustrackerapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import java.nio.charset.Charset;

public class BluetoothAdvertiser {
    private ParcelUuid serviceUUID;
    private String advData;
    private BluetoothLeAdvertiser bleAdvertiser;
    private MainActivity mainActivity;
    private AdvertiseSettings advertiseSettings;
    private AdvertiseData advertiseData;

    public BluetoothAdvertiser(String uuid, String advData, MainActivity mainActivity) {
        serviceUUID = new ParcelUuid(UUID.fromString(uuid));
        this.advData = advData;
        //Initiating default advertiser.
        bleAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        this.mainActivity = mainActivity;

        BluetoothAdapter.getDefaultAdapter().setName("X");

        advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(serviceUUID)
                .build();
        //To be fixed later.
        //.addServiceData(serviceUUID,"1".getBytes())
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        /**
         * Callback triggered in response to {@link BluetoothLeAdvertiser#startAdvertising} indicating
         * that the advertising has been started successfully.
         *
         * @param settingsInEffect The actual settings used for advertising, which may be different from
         *                         what has been requested.
         */
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            UtilityClass.toast(mainActivity.getApplicationContext(),"Advertising started successfully.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            UtilityClass.toast(mainActivity.getApplicationContext(),"Advertising failed, error code: " + errorCode + ".");
        }
    };

    public void startAdvertiser(){
        bleAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
    }

    public void stopAdvertiser(){
        UtilityClass.toast(mainActivity.getApplicationContext(),"Advertising stopped by user.");
        bleAdvertiser.stopAdvertising(advertiseCallback);
    }
}
