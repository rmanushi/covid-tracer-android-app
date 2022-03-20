package com.example.virustrackerapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.app.Activity;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class BluetoothDevicesListAdapter extends ArrayAdapter<BluetoothDevice> {

    private final Activity myActivity;
    private final ArrayList<BluetoothDevice> deviceList;
    private final int resourceID;

    /**
     * Constructor
     *
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     */
    public BluetoothDevicesListAdapter(@NonNull Activity activity, int resource, @NonNull ArrayList<BluetoothDevice> objects) {
        super(activity.getApplicationContext(), resource, objects);
        myActivity = activity;
        resourceID = resource;
        deviceList = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) myActivity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(resourceID, parent, false);
        }

        BluetoothDevice device = deviceList.get(position);
        String deviceAddress = device.getAddress();
        TextView textView;
        //Displaying device bluetooth mac address that is generated each time advertising is initiated.
        textView = (TextView) convertView.findViewById(R.id.tv_macaddrs);
        if(deviceAddress != null && deviceAddress.length() > 0){
            textView.setText(deviceAddress);
        }else{
            textView.setText("No address.");
        }
        return convertView;
    }

}
