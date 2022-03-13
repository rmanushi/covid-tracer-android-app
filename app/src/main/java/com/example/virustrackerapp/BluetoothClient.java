package com.example.virustrackerapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.util.Log;


import java.util.UUID;


public class BluetoothClient {

    private final BluetoothGatt bluetoothGatt;
    private UUID serviceUUID, characteristicUUID;
    private final static String TAG = "GATT Cli";

    public BluetoothClient(Activity activity, BluetoothDevice remoteDevice, String serviceUUID, String characteristicUUID) {
        this.serviceUUID = UUID.fromString(serviceUUID);
        this.characteristicUUID = UUID.fromString(characteristicUUID);
        bluetoothGatt = remoteDevice.connectGatt(activity,false, bluetoothGattCallback);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt     GATT client
         * @param status   Status of the connect or disconnect operation. {@link
         *                 BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState Returns the new connection state. Can be one of {@link
         *                 BluetoothProfile#STATE_DISCONNECTED} or {@link BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from remote server.");
            }else if(newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to remote server.");
                bluetoothGatt.discoverServices();
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device has been explored
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.i(TAG,"Services found.");
                //bluetoothGatt.getService(serviceUUID);
                if(bluetoothGatt.getService(serviceUUID)==null) {
                    Log.i(TAG, "Could not find required service.");
                }else{
                    BluetoothGattCharacteristic idCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
                    bluetoothGatt.readCharacteristic(idCharacteristic);
                    Log.i(TAG,idCharacteristic.toString());
                }
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *  @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated remote device.
         * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            //DB method to enter info to db will be added here probably.
            //UtilityClass.toast(activity,characteristic.toString());
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "ID read successfully.");
                Log.i(TAG, "GATT client closed.");
                Log.i(TAG, characteristic.getValue().toString());
                bluetoothGatt.close();
            }

        }

    };

}
