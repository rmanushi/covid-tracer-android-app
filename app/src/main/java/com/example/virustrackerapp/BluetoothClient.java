package com.example.virustrackerapp;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


import androidx.annotation.Nullable;

import java.util.UUID;


public class BluetoothClient extends Service  {

    private BluetoothGatt bluetoothGatt;
    private final UUID serviceUUID = UUID.fromString("BB1A3410-B7CA-412C-8A13-AE9D912981AD");
    private final UUID characteristicUUID = UUID.fromString("AD4FC837-06CB-476B-8CB3-DFC6876F187E");

    private final static String TAG = "GATT Cli";
    //Action to be broadcasted in specific event occurrences;
    public final static String ACTION_CONNECTED = "com.example.virustrackerapp.ACTION_GATT_CONNECTED";
    public final static String ACTION_DISCONNECTED = "com.example.virustrackerapp.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_REQUIRED_CHARACTERISTIC_FOUND = "com.example.virustrackerapp.ACTION_REQUIRED_CHARACTERISTIC_FOUND";
    public final static String ACTION_CHARACTERISTIC_DATA_READ = "com.example.virustrackerapp.ACTION_CHARACTERISTIC_DATA_READ";
    public final static String CHARACTERISTIC_DATA = "com.example.virustrackerapp.CHARACTERISTIC_DATA";

    class LocalBinder extends Binder {
        public BluetoothClient getService(){
            return BluetoothClient.this;
        }
    }

    private final Binder binder = new LocalBinder();

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     *
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (!UtilityClass.checkBluetoothStatus()) {
            Log.i(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean connect(BluetoothDevice device) {
        try {
            bluetoothGatt = device.connectGatt(this,false, bluetoothGattCallback);
            return true;
        } catch (IllegalArgumentException exception) {
            Log.i(TAG, "Device not found with provided address.");
            return false;
        }
    }

    public void disconnect(){
        bluetoothGatt.disconnect();
    }

    private void close() {
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    //Method for sending broadcasts to the close contact activity.
    private void broadcastUpdate(final String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //Method for sending broadcasts to the close contact activity, including the required characteristic data.
    private void broadcastUpdate(final String action, BluetoothGattCharacteristic myCharacteristic){
        Intent intent = new Intent(action);
        final byte[] data = myCharacteristic.getValue();
        //Converting byte data into a UUID.
        final UUID dataInUUID = UtilityClass.convertBytesToUUID(data);
        //Converting UUID data to string for other operations.
        final String dataInString = dataInUUID.toString();
        intent.putExtra(CHARACTERISTIC_DATA,dataInString);
        sendBroadcast(intent);
    }

    public BluetoothGattCharacteristic getAppCharacteristic(){
        return bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic){
        bluetoothGatt.readCharacteristic(characteristic);
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
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_CONNECTED);
                //Discovering services once connected to the gatt server.
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(ACTION_DISCONNECTED);
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
                //Checking if the required app characteristic is available in the remote gatt server.
                if(bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID) != null){
                    broadcastUpdate(ACTION_REQUIRED_CHARACTERISTIC_FOUND);
                }
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated remote device.
         * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_CHARACTERISTIC_DATA_READ,characteristic);
            }
        }
    };

}
