package com.example.virustrackerapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity  {
    private final int APP_STATE_ON = 1;
    private int currentAppState;
    private Handler appOperationHandler = new Handler();
    //Components in the main page.
    private Button scanBtn;
    private ListView deviceListView;
    private TextView header;
    private TextView appNotActiveTv;
    private TextView noUsersTv;
    private final int PERMISSIONS_REQUEST_CODE = 10;
    //private ProgressDialog progressDialog;


    private ArrayList<BluetoothDevice> devicesFound;
    private BluetoothDevicesListAdapter adapter;
    private BluetoothScanner myBleScanner;
    private BluetoothAdvertiser myBleAdvertiser;
    private BluetoothServer myBluetoothServer;

    private SharedPreferences sharedPreferences;


    //Components that are going to be shown in the update pop up.
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog updateDialog;
    private RadioButton trueUpdateInfectionBtn, falseUpdateInfectionBtn, trueUpdateVaccineBtn, falseUpdateVaccineBtn;

    //Receiver used to detect the broadcast sent by the CloseContact activity in case the connection attempt to the selected device fails
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(CloseContactActivity.CONNECTION_TO_DEVICE_FAILED.equals(action)){
                BluetoothDevice unavailableDevice = intent.getExtras().getParcelable("unavailableDevice");
                removeDevice(unavailableDevice);
            }else if(CloseContactActivity.CLOSE_CONTACT_ALREADY_ESTABLISHED.equals(action)){
                BluetoothDevice alreadyConnectedUserDevice = intent.getExtras().getParcelable("alreadyConnectedUserDevice");
                removeDevice(alreadyConnectedUserDevice);
                UtilityClass.toast(getApplicationContext(), "Close contact has already been established with this user before.");
            }else if(CloseContactActivity.CLOSE_CONTACT_SUCCESS.equals(action)){
                BluetoothDevice closeContactSuccessDevice = intent.getExtras().getParcelable("connectedDeviceSuccess");
                removeDevice(closeContactSuccessDevice);
                UtilityClass.toast(getApplicationContext(), "Close contact established successfully.");
            }else if(CloseContactActivity.CLOSE_CONTACT_INTERNAL_SQL_ERROR.equals(action) || CloseContactActivity.CLOSE_CONTACT_SERVER_CONNECTION_ERROR.equals(action)){
                UtilityClass.toast(getApplicationContext(), "Internal server error: Unable to establish close contact with this user.");
            }
        }
    };

    //Adding the broadcast action to an intent filter;
    private IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CloseContactActivity.CONNECTION_TO_DEVICE_FAILED);
        intentFilter.addAction(CloseContactActivity.CLOSE_CONTACT_ALREADY_ESTABLISHED);
        intentFilter.addAction(CloseContactActivity.CLOSE_CONTACT_SUCCESS);
        intentFilter.addAction(CloseContactActivity.CLOSE_CONTACT_INTERNAL_SQL_ERROR);
        intentFilter.addAction(CloseContactActivity.CLOSE_CONTACT_SERVER_CONNECTION_ERROR);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Checking if user device supports bluetooth.
        if(!UtilityClass.checkBluetoothSupport()){
            UtilityClass.toast(this, "Bluetooth is not supported, the application can not work!");
            finish();
        }

        //Only initiating the registration dialog the first time the user opens the app.
        sharedPreferences = getSharedPreferences(getString(R.string.shared_preference_key),MODE_PRIVATE);
        sharedPreferences.getInt("vaccination",0);
        sharedPreferences.getInt("infection",0);
        String userIdentifier = sharedPreferences.getString("uid",null);
        boolean isUserRegistered = sharedPreferences.getBoolean("registration",false);

        if(!isUserRegistered && userIdentifier == null){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            UUID userUUID = UUID.randomUUID();
            editor.putString("uid",userUUID.toString());
            editor.putBoolean("registration", true);
            editor.apply();
            registrationProcedure();
        }

        //Initiating current app state value for the scanning function.
        currentAppState = 1;

        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        Button updateBtn = (Button) findViewById(R.id.updateBtn);
        deviceListView = (ListView) findViewById(R.id.deviceList);
        header = (TextView) findViewById(R.id.nearByBanner);
        appNotActiveTv = (TextView) findViewById(R.id.appNotActive);
        noUsersTv = (TextView) findViewById(R.id.emptyView);
        deviceListView.setVisibility(View.INVISIBLE);
        header.setVisibility(View.INVISIBLE);

        myBleScanner = new BluetoothScanner(this, getString(R.string.service_uuid));
        myBleAdvertiser = new BluetoothAdvertiser(getString(R.string.service_uuid),this);
        myBluetoothServer = new BluetoothServer(this, getString(R.string.service_uuid), getString(R.string.characteristic_uuid), sharedPreferences.getString("uid",null), (BluetoothManager)getSystemService(BLUETOOTH_SERVICE));
        devicesFound = new ArrayList<>();
        adapter = new BluetoothDevicesListAdapter(this,R.layout.list_item_view, devicesFound);
        deviceListView.setAdapter(adapter);
        //Setting listener for event to occur when a specific list item is clicked.
        deviceListView.setOnItemClickListener((parent, view, position, id) -> launchCloseContactActivity(devicesFound.get(position)));

        updateBtn.setOnClickListener(v -> createUpdateDialog());

        scanBtn.setOnClickListener(v -> {
            deviceListView.setEmptyView(noUsersTv);
            if(currentAppState == APP_STATE_ON){
                //Handler is used to stop the app operation after
                // a certain amount of time. This is done to save battery and
                // to close bluetooth communication channels in the user's device
                // should they leave the app run in the background by mistake without stopping it.
                //Delay is set to 3 hours in milliseconds in order to allow
                // user to scan while in public spaces for long amount of time.
                appOperationHandler.postDelayed(() -> {
                    stopAppOperation();
                    currentAppState = 1;
                },10800000);

                startAppOperation();
                currentAppState = 0;
            }else{
                stopAppOperation();
                currentAppState = 1;
            }
        });
    }

    private void startAppOperation(){
        registerReceiver(receiver,intentFilter());
        scanBtn.setText("STOP");
        appNotActiveTv.setVisibility(View.INVISIBLE);
        noUsersTv.setVisibility(View.VISIBLE);
        startScan();
        myBleAdvertiser.startAdvertiser();
        myBluetoothServer.startServer();
        scanBtn.setBackgroundColor(Color.RED);
    }

    private void stopAppOperation(){
        unregisterReceiver(receiver);
        scanBtn.setText("SCAN");
        stopScan();
        myBleAdvertiser.stopAdvertiser();
        myBluetoothServer.stopServer();
        appNotActiveTv.setVisibility(View.VISIBLE);
        noUsersTv.setVisibility(View.INVISIBLE);
        scanBtn.setBackgroundColor(Color.parseColor("#333CE5"));
    }

    private void startScan(){
        //Requesting dangerous permissions for new android update.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            if(getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //Showing a explanation why permission needed.
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("The application requires location access.");
                    dialogBuilder.setMessage("Grant location access so the app can scan for nearby users.");
                    //If ok button is pressed request permission.
                    dialogBuilder.setPositiveButton("Ok", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_CODE));
                    dialogBuilder.setNegativeButton("Cancel", (dialog, which) -> {
                        UtilityClass.toast(MainActivity.this, "Permissions denied.");
                        dialog.dismiss();
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
    public void addDevice(BluetoothDevice device){
        if(!devicesFound.contains(device)){
            devicesFound.add(device);
            adapter.notifyDataSetChanged();
        }
    }

    //Method called by the onReceive method in case a broadcast is received stating that a device is no longer connectable.
    private void removeDevice(BluetoothDevice device){
        devicesFound.remove(device);
        adapter.notifyDataSetChanged();
    }

    private void stopScan(){
        devicesFound.clear();
        myBleScanner.stopScanner();
        adapter.notifyDataSetChanged();
        deviceListView.setVisibility(View.INVISIBLE);
        header.setVisibility(View.INVISIBLE);
    }

    //Method that creates the update dialog view.
    private void createUpdateDialog(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View updatePopUpView = getLayoutInflater().inflate(R.layout.update_activity_popup,null);

        //Setting up submit and cancel buttons to their components in the update layout.
        Button updatePopUpSubmitBtn = (Button) updatePopUpView.findViewById(R.id.submitRegistrationBtn);
        Button updatePopUpCancelBtn = (Button) updatePopUpView.findViewById(R.id.cancelRegistrationBtn);

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

    private void registrationProcedure(){
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Welcome to the Virus Tracker App");
        dialogBuilder.setMessage("A new user profile has been automatically created for you. " +
                " Both your infection and full vaccination status have been set to false by default." +
                " Feel free to update your profile accordingly using the profile section of the application.");
        dialogBuilder.setOnDismissListener((dialog)-> new RegistrationInsert().execute());
        dialogBuilder.setPositiveButton("Ok",(dialog, which) -> dialog.dismiss());
        dialogBuilder.show();
    }

    //Method to be called by the click of a list item.
    private void launchCloseContactActivity(BluetoothDevice bleD){
        //Creating an intent and passing the selected device to the next activity.
        Intent intent = new Intent(getApplicationContext(), CloseContactActivity.class);
        intent.putExtra("Device", bleD);
        startActivity(intent);
    }

    //Private class representing a background task for user registration (insertion into the database).
    private class RegistrationInsert extends AsyncTask <String, String, String> {

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

        /**
         * Runs on the UI thread before {@link #doInBackground}.
         * Invoked directly by {@link #execute} or {@link #executeOnExecutor}.
         * The default version does nothing.
         *
         * @see #onPostExecute
         * @see #doInBackground
         */
        @Override
        protected void onPreExecute() {
            //Showing progress dialog to the user to inform of the running task during registration.
            super.onPreExecute();
            //ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            //progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("User Registration");
            progressDialog.setMessage("Registering user Please Wait..");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p>
         * This will normally run on a background thread. But to better
         * support testing frameworks, it is recommended that this also tolerates
         * direct execution on the foreground thread, as part of the {@link #execute} call.
         * <p>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param strings The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected String doInBackground(String... strings) {
            //Getting user id stored into the shared preferences.
            String id = sharedPreferences.getString("uid",null);
            //Getting pre-determined url from the string resource file.
            String userRegistrationURL = getString(R.string.registration_insert_url);
            try {
                URL url = new URL(userRegistrationURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                //Setting timeouts for server response and connection.
                con.setReadTimeout(15000);
                con.setConnectTimeout(15000);
                //Setting method to send over data.
                con.setRequestMethod("POST");
                con.setDoInput(true);
                con.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder().appendQueryParameter("userID", id);
                String query = builder.build().getEncodedQuery();

                //Connection channel to send data to server.
                OutputStream outputStream = con.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(
                        new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                bufferedWriter.write(query);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();
                con.connect();

                int responseCode = con.getResponseCode();

                //Checking server response code.
                if(responseCode == HttpURLConnection.HTTP_OK){
                    //Getting server response.
                    InputStream input = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder serverResponse = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        serverResponse.append(line);
                    }

                    //Turning response into json format.
                    JSONObject jsonResponse = new JSONObject(String.valueOf(serverResponse));
                    //Extracting success value from the json format.
                    int success = jsonResponse.getInt("success");

                    //Checking json response in case the operation was successful or not.
                    Thread thread; //New thread for displaying messages to the UI.
                    if(success == 1) {
                        //Success message shown on the UI thread.
                        thread = new Thread() {
                            public void run() {
                                runOnUiThread(() -> UtilityClass.toast(getApplicationContext(), "User registration completed."));
                            }
                        };
                    }else{
                        //Error message to be shown in the UI thread.
                        thread = new Thread() {
                            public void run() {
                                runOnUiThread(() -> UtilityClass.toast(getApplicationContext(), "Unable to register user."));
                            }
                        };
                    }
                    thread.start();
                    //Disconnecting once the message is received.
                    con.disconnect();
                }else{
                    //Error message to be shown in the UI thread.
                    Thread thread = new Thread(){
                        public void run(){
                            runOnUiThread(() -> UtilityClass.toast(getApplicationContext(),"Failed to establish connection to the server."));
                        }
                    };
                    thread.start();
                    con.disconnect();
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * <p>Runs on the UI thread after {@link #doInBackground}. The
         * specified result is the value returned by {@link #doInBackground}.
         * To better support testing frameworks, it is recommended that this be
         * written to tolerate direct execution as part of the execute() call.
         * The default version does nothing.</p>
         *
         * <p>This method won't be invoked if the task was cancelled.</p>
         *
         * @param s The result of the operation computed by {@link #doInBackground}.
         * @see #onPreExecute
         * @see #doInBackground
         */
        @Override
        protected void onPostExecute(String s) {
            //Removing progress dialog once the registration task is completed.
            super.onPostExecute(s);
            progressDialog.dismiss();
        }
    }
}