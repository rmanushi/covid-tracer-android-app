package com.example.virustrackerapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.UUID;

//Class to help with simple helper services.

public class UtilityClass {

   public static boolean checkBluetoothSupport(){
       if(BluetoothAdapter.getDefaultAdapter() == null) {
           return false;
       }
       return true;
   }

    //Check either if bluetooth is available or if bluetooth is disabled.
    public static boolean checkBluetoothStatus(){
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
            return false;
        }
        return true;
    }

    //Requesting user to enable bluetooth.
    public static void requestBluetoothActivation(Activity a){
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        a.startActivityForResult(enableBluetooth, 1);
    }

    //Method to print messages to the user.
    public static void toast(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static byte[] convertUUIDtoBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        Long high = byteBuffer.getLong();
        Long low = byteBuffer.getLong();
        return new UUID(high, low);
    }

}
