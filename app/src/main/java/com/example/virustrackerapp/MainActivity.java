package com.example.virustrackerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.os.Build;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  {
    private final int APP_STATE_ON = 1;
    private int currentAppState;
    //Components in the main page.
    private Button scanBtn, updateBtn;
    private ListView deviceListView;
    private TextView header;
    private TextView appNotActiveTv;
    private final int LOCATION_REQUEST_CODE = 3;
    private final int INTERNET_REQUEST_CODE = 4;
    private final int PERMISSIONS_REQUEST_CODE = 10;

    private ArrayList<BluetoothDevice> devicesFound;
    private BluetoothDevicesListAdapter adapter;
    private BluetoothScanner myBleScanner;
    private BluetoothAdvertiser myBleAdvertiser;
    private BluetoothServer myBluetoothServer;
    private static final String TAG = "MyActivity";


    //Components that are going to be shown in the update pop up.
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog updateDialog;
    private Button updatePopUpSubmitBtn, updatePopUpCancelBtn;
    private RadioButton trueUpdateInfectionBtn, falseUpdateInfectionBtn, trueUpdateVaccineBtn, falseUpdateVaccineBtn;

    //Components that are going to be shown in the registration pop up.
    private AlertDialog registrationDialog;
    private Button registrationPopUpSubmitBtn, registrationPopUpCancelBtn;
    private RadioButton trueRegistrationInfectionBtn, falseRegistrationInfectionBtn, trueRegistrationVaccineBtn, falseRegistrationVaccineBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Checking if user device supports bluetooth.
        if(!UtilityClass.checkBluetoothSupport()){
            UtilityClass.toast(this, "Bluetooth is not supported, the application can not work!");
            finish();
        }

        //Only initiating the registration dialog the first time the user opens the app.
        boolean firstRun = getSharedPreferences("PREFERENCE",MODE_PRIVATE).getBoolean("firstRun",true);
        if(true){
            createRegistrationDialog();
            //Saving the state for the first time the app has been opened.
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("firstRun", false)
                    .apply();
        }

        //Initiating current app state value for the scanning function.
        currentAppState = 1;

        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        updateBtn = (Button) findViewById(R.id.updateBtn);
        deviceListView = (ListView) findViewById(R.id.deviceList);
        header = (TextView) findViewById(R.id.nearByBanner);
        appNotActiveTv = (TextView) findViewById(R.id.appNotActive);
        header.setVisibility(View.INVISIBLE);
        deviceListView.setVisibility(View.INVISIBLE);

        myBleScanner = new BluetoothScanner(this,1000000,-90, getString(R.string.service_uuid));
        myBleAdvertiser = new BluetoothAdvertiser(getString(R.string.service_uuid),this);
        myBluetoothServer = new BluetoothServer(this, getString(R.string.service_uuid), getString(R.string.characteristic_uuid), (BluetoothManager)getSystemService(BLUETOOTH_SERVICE));
        devicesFound = new ArrayList<>();
        adapter = new BluetoothDevicesListAdapter(this,R.layout.list_item_view, devicesFound);
        deviceListView.setAdapter(adapter);
        //Setting listener for event to occur when a specific list item is clicked.
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchCloseContactActivity(devicesFound.get(position));
            }
        });

        updateBtn.setOnClickListener(v -> createUpdateDialog());

        scanBtn.setOnClickListener(v -> {
            if(currentAppState == APP_STATE_ON){
                scanBtn.setText("STOP");
                appNotActiveTv.setVisibility(View.INVISIBLE);
                startScan();
                myBleAdvertiser.startAdvertiser();
                myBluetoothServer.startServer();
                currentAppState = 0;
                scanBtn.setBackgroundColor(Color.RED);
            }else{
                scanBtn.setText("SCAN");
                stopScan();
                myBleAdvertiser.stopAdvertiser();
                myBluetoothServer.stopServer();
                appNotActiveTv.setVisibility(View.VISIBLE);
                currentAppState = 1;
                scanBtn.setBackgroundColor(Color.parseColor("#333CE5"));
            }
        });
    }

    public void startScan(){
        //Requesting dangerous permissions for new android update.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            if(getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //Showing a explanation why permission needed.
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("The application requires location access.");
                    dialogBuilder.setMessage("Grant location access so the app can scan for nearby users.");
                    //If ok button is pressed request permission.
                    dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_CODE);
                        }
                    });
                    dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UtilityClass.toast(MainActivity.this, "Permissions denied.");
                            dialog.dismiss();
                        }
                    });
                    dialogBuilder.show();
                }
            }else{
                //No explanation, request permission immediately.
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_CODE);
            }
        }else{
            //No permission required on older versions.
            deviceListView.setVisibility(View.VISIBLE);
            myBleScanner.startScanner();
            UtilityClass.toast(this,"Permissions granted.");
        }
    }

    //Overriding the method to handle permission requests and start operations when permissions are granted.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSIONS_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                header.setVisibility(View.VISIBLE);
                deviceListView.setVisibility(View.VISIBLE);
                myBleScanner.startScanner();
            }else{
                UtilityClass.toast(this,"Permissions denied.");
            }

        }

    }

    //Method called by the scanner in the scan callback in order to update the list of devices found and display them to the user.
    public void add(BluetoothDevice d){
        if(!devicesFound.contains(d)){
            devicesFound.add(d);
            adapter.notifyDataSetChanged();
        }
    }

    public void stopScan(){
        devicesFound.clear();
        myBleScanner.stopScanner();
        adapter.notifyDataSetChanged();
        deviceListView.setVisibility(View.INVISIBLE);
        header.setVisibility(View.INVISIBLE);
    }

    //Method that creates the update dialog view.
    public void createUpdateDialog(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View updatePopUpView = getLayoutInflater().inflate(R.layout.update_activity_popup,null);

        //Setting up submit and cancel buttons to their components in the update layout.
        updatePopUpSubmitBtn = (Button) updatePopUpView.findViewById(R.id.submitRegistrationBtn);
        updatePopUpCancelBtn = (Button) updatePopUpView.findViewById(R.id.cancelRegistrationBtn);

        //Setting up radio buttons to their components in the update layout.
        trueUpdateInfectionBtn = (RadioButton) updatePopUpView.findViewById(R.id.infectionTrueBtnReg);
        falseUpdateInfectionBtn = (RadioButton) updatePopUpView.findViewById(R.id.infectionFalseBtnReg);
        trueUpdateVaccineBtn = (RadioButton) updatePopUpView.findViewById(R.id.trueVaccineBtnReg);
        falseUpdateVaccineBtn = (RadioButton) updatePopUpView.findViewById(R.id.falseRegistrationVaccineBtn);

        //Setting up update dialog view with new assigned components in the main activity.
        dialogBuilder.setView(updatePopUpView);
        updateDialog = dialogBuilder.create();
        updateDialog.show();

        //Action to take place when new data is added and the submit button is pressed.
        updatePopUpSubmitBtn.setOnClickListener(v -> {
            //The rest is to be defined with the backend to actually update the specific user profile.

            //Checking that user input is valid while selecting new values to update their profile.
            if(!trueUpdateVaccineBtn.isChecked() && !falseUpdateVaccineBtn.isChecked()){
                UtilityClass.toast(this,"Error: Please select a new value for vaccination in order to submit!");
            }else if(!trueUpdateInfectionBtn.isChecked() && !falseUpdateInfectionBtn.isChecked()){
                UtilityClass.toast(this,"Error: Please select a  new value for infection in order to submit!");
            }
        });

        //Action to be performed when cancel button in the pop up is pressed.
        updatePopUpCancelBtn.setOnClickListener(v -> updateDialog.dismiss());

        ////////////////////Radio button changes when they are pressed////////////////////
        trueUpdateVaccineBtn.setOnClickListener(v -> {
            trueUpdateVaccineBtn.setChecked(true);
            falseUpdateVaccineBtn.setChecked(false);
        });

        falseUpdateVaccineBtn.setOnClickListener(v -> {
            falseUpdateVaccineBtn.setChecked(true);
            trueUpdateVaccineBtn.setChecked(false);
        });

        trueUpdateInfectionBtn.setOnClickListener(v -> {
            trueUpdateInfectionBtn.setChecked(true);
            falseUpdateInfectionBtn.setChecked(false);
        });
        falseUpdateInfectionBtn.setOnClickListener(v -> {
            falseUpdateInfectionBtn.setChecked(true);
            trueUpdateInfectionBtn.setChecked(false);
        });
        ///////////////////////End of radio button view changes//////////////////////
    }

    public void createRegistrationDialog(){
        final int INFECTION_TRUE = 1;
        final int INFECTION_FALSE = 0;
        final int VACCINE_TRUE = 1;
        final int VACCINE_FALSE = 0;
        dialogBuilder = new AlertDialog.Builder(this);
        final View registrationPopUpView = getLayoutInflater().inflate(R.layout.registration_activity_popup,null);

        //Setting up submit and cancel buttons to their components in the registration layout.
        registrationPopUpCancelBtn = (Button) registrationPopUpView.findViewById(R.id.cancelRegistrationBtn);
        registrationPopUpSubmitBtn = (Button) registrationPopUpView.findViewById(R.id.submitRegistrationBtn);

        //Setting up radio buttons to their components in the registration layout.
        trueRegistrationInfectionBtn = (RadioButton) registrationPopUpView.findViewById(R.id.infectionTrueBtnReg);
        falseRegistrationInfectionBtn = (RadioButton) registrationPopUpView.findViewById(R.id.infectionFalseBtnReg);
        trueRegistrationVaccineBtn = (RadioButton) registrationPopUpView.findViewById(R.id.trueVaccineBtnReg);
        falseRegistrationVaccineBtn = (RadioButton) registrationPopUpView.findViewById(R.id.falseRegistrationVaccineBtn);

        //Setting up registration dialog view with new assigned components in the main activity.
        dialogBuilder.setView(registrationPopUpView);
        registrationDialog = dialogBuilder.create();
        registrationDialog.show();

        registrationPopUpCancelBtn.setOnClickListener(v -> {
            UtilityClass.toast(this,"Registration is required to use the application.");
        });

        ////////////////////Radio button changes when they are pressed registration////////////////////
        trueRegistrationVaccineBtn.setOnClickListener(v -> {
            trueRegistrationVaccineBtn.setChecked(true);
            falseRegistrationVaccineBtn.setChecked(false);
        });

        falseRegistrationVaccineBtn.setOnClickListener(v -> {
            falseRegistrationVaccineBtn.setChecked(true);
            trueRegistrationVaccineBtn.setChecked(false);
        });

        trueRegistrationInfectionBtn.setOnClickListener(v -> {
            trueRegistrationInfectionBtn.setChecked(true);
            falseRegistrationInfectionBtn.setChecked(false);
        });

        falseRegistrationInfectionBtn.setOnClickListener(v -> {
            falseRegistrationInfectionBtn.setChecked(true);
            trueRegistrationInfectionBtn.setChecked(false);
        });
        ///////////////////////End of radio button view changes registration//////////////////////

        //Checking that user input is valid while selecting values for registration.
        registrationPopUpSubmitBtn.setOnClickListener(v -> {
            if(!trueRegistrationVaccineBtn.isChecked() && !falseRegistrationVaccineBtn.isChecked()){
                UtilityClass.toast(this,"Error: Please select a value for vaccination in order to submit!");
            }else if(!trueRegistrationInfectionBtn.isChecked() && !falseRegistrationInfectionBtn.isChecked()){
                UtilityClass.toast(this,"Error: Please select a value for infection in order to submit!");
            }else{
                if(trueRegistrationVaccineBtn.isChecked() && trueRegistrationInfectionBtn.isChecked()){
                    enterToDB(0,VACCINE_TRUE,INFECTION_TRUE);//Needs to change to actual db connection.
                }else if(!trueRegistrationVaccineBtn.isChecked() && !trueRegistrationInfectionBtn.isChecked()){
                    enterToDB(0,VACCINE_FALSE,INFECTION_FALSE);//Needs to change to actual db connection.
                }else if(trueRegistrationVaccineBtn.isChecked() && !trueRegistrationInfectionBtn.isChecked()){
                    enterToDB(0,VACCINE_TRUE,INFECTION_FALSE);//Needs to change to actual db connection.
                }else{
                    enterToDB(0,VACCINE_FALSE,INFECTION_TRUE);//Needs to change to actual db connection.
                }
                registrationDialog.dismiss();
            }
        });

    }

    public void enterToDB(int uid, int x, int y){
        UtilityClass.toast(this,"Entered to db values vaccine: "+x+", infection: "+y+" for user: "+uid+".");
    }

    //Method to be called by the click of a list item.
    public void launchCloseContactActivity(BluetoothDevice bleD){
        //Creating an intent and passing the selected device to the next activity.
        Intent intent = new Intent(getApplicationContext(), CloseContactActivity.class);
        intent.putExtra("Device", bleD);
        startActivity(intent);
    }
}