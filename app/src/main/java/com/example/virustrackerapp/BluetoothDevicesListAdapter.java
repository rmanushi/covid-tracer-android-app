package com.example.virustrackerapp;

import static android.R.layout.simple_list_item_2;

import android.annotation.SuppressLint;
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

    private Activity myActivity;
    private ArrayList<BluetoothDevice> deviceList;
    private int resourceID;

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
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        //Need to update with uuid for service.
        TextView textView = null;
        //Displaying device name.
        //To be removed in the future for privacy reasons.
        textView = (TextView) convertView.findViewById(R.id.tv_name);
        if(deviceName != null && deviceName.length() > 0){
            textView.setText(deviceName);
        }else{
            textView.setText("No name.");
        }

        //Displaying device address.
        textView = (TextView) convertView.findViewById(R.id.tv_macaddr);
        if(deviceName != null && deviceName.length() > 0){
            textView.setText(deviceAddress);
        }else{
            textView.setText("No address.");
        }

        return convertView;
    }
}
