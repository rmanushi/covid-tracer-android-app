package com.example.virustrackerapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;
import java.util.UUID;

public class BluetoothAdvertiser {
    private ParcelUuid serviceUUID;
    private BluetoothLeAdvertiser bleAdvertiser;
    private MainActivity mainActivity;
    private AdvertiseSettings advertiseSettings;
    private AdvertiseData advertiseData;

    public BluetoothAdvertiser(String uuid, MainActivity mainActivity) {
        serviceUUID = new ParcelUuid(UUID.fromString(uuid));
        //Initiating default advertiser.
        bleAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        this.mainActivity = mainActivity;

        BluetoothAdapter.getDefaultAdapter().setName("VirusTrackerAppUser");

        advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(serviceUUID)
                .build();
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
        bleAdvertiser.stopAdvertising(advertiseCallback);
    }
}
