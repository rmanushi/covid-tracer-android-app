package com.example.virustrackerapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.os.ParcelUuid;

import java.util.UUID;

public class BluetoothServer {
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private MainActivity mainActivity;
    private BluetoothManager bluetoothManager;

    public BluetoothServer(MainActivity mainActivity, String serviceUUID, String characteristicUUID, BluetoothManager bluetoothManager) {
        this.mainActivity = mainActivity;
        this.bluetoothManager = bluetoothManager;
        bluetoothGattService = new BluetoothGattService(UUID.fromString(serviceUUID),BluetoothGattService.SERVICE_TYPE_PRIMARY);
        bluetoothGattCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(characteristicUUID), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        String id = "9366DB97-B301-46CE-B5CF-65FDF1C8839F";
        byte[] data = id.getBytes();
        bluetoothGattCharacteristic.setValue(data);
        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);
    }

    public void startServer(){
        bluetoothGattServer = bluetoothManager.openGattServer(mainActivity.getApplicationContext(),gattCallback);
        if(bluetoothGattServer == null){
            UtilityClass.toast(mainActivity.getApplicationContext(),"Unable to create gatt server");
        }
        bluetoothGattServer.addService(bluetoothGattService);

    }

    public void stopServer(){
        bluetoothGattServer.clearServices();
        bluetoothGattServer.close();
    }

    private BluetoothGattServerCallback gattCallback = new BluetoothGattServerCallback() {
        /**
         * A remote client has requested to read a local characteristic.
         *
         * <p>An application must call {@link BluetoothGattServer#sendResponse}
         * to complete the request.
         *
         * @param device         The remote device that has requested the read operation
         * @param requestId      The Id of the request
         * @param offset         Offset into the value of the characteristic
         * @param characteristic Characteristic to be read
         */
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            if(bluetoothGattCharacteristic.getUuid().equals(characteristic.getUuid())) {
                bluetoothGattServer.sendResponse(device,requestId, BluetoothGatt.GATT_SUCCESS,0, bluetoothGattCharacteristic.getValue());
            }else{
                bluetoothGattServer.sendResponse(device,requestId, BluetoothGatt.GATT_FAILURE,0,null);
                bluetoothGattServer.cancelConnection(device);
            }
        }
    };
}
