package com.example.virustrackerapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationRequest;
import android.widget.Toast;

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

}
