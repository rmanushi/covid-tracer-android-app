package com.example.virustrackerapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

public class CloseContactActivity extends AppCompatActivity {
    private BluetoothService bluetoothService;
    private BluetoothDevice device;
    private String data;
    private Button submitBtnCloseContact;
    private Button cancelBtnCloseContact;
    private TextView closeContactActivityTv;
    private Handler connectionHandler = new Handler();
    public static final String CONNECTION_TO_DEVICE_FAILED = "com.example.virustrackerapp.CONNECTION_TO_DEVICE_FAILED";
    public static final String CLOSE_CONTACT_SUCCESS = "com.example.virustrackerapp.CLOSE_CONTACT_SUCCESS";
    public static final String CLOSE_CONTACT_ALREADY_ESTABLISHED = "com.example.virustrackerapp.CLOSE_CONTACT_ALREADY_ESTABLISHED";
    public static final String CLOSE_CONTACT_INTERNAL_SQL_ERROR = "com.example.virustrackerapp.CLOSE_CONTACT_INTERNAL_SQL_ERROR";
    public static final String CLOSE_CONTACT_SERVER_CONNECTION_ERROR = "com.example.virustrackerapp.CLOSE_CONTACT_SERVER_CONNECTION_ERROR";
    private final Runnable connectionTimeoutOperation = new Runnable() {
        @Override
        public void run() {
            //In case the app is not able to connect to the other user,
            // the activity is stopped and the connection is closed.
            //A broadcast it to be sent that will be received by the main activity in order to update the available device list.
            Intent connectionFailedIntent = new Intent(CONNECTION_TO_DEVICE_FAILED);
            connectionFailedIntent.putExtra("unavailableDevice",device);
            UtilityClass.toast(getApplicationContext(),"This user is no longer connectable.");
            sendBroadcast(connectionFailedIntent);
            finish();
        }
    };
    private final int TIMEOUT_LIMIT = 5000;


    //Receiver waiting for broadcasts send by the service.
    private final BroadcastReceiver bluetoothServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_CONNECTED.equals(action)) {
                UtilityClass.toast(getApplicationContext(),"Connected to remote user.");
                //In case the connection is achieved the handler timeout operation is discarded.
                connectionHandler.removeCallbacks(connectionTimeoutOperation);
            } else if (BluetoothService.ACTION_DISCONNECTED.equals(action)) {
                UtilityClass.toast(getApplicationContext(),"Disconnected from remote user.");
            }else if(BluetoothService.ACTION_REQUIRED_CHARACTERISTIC_FOUND.equals(action)){
                bluetoothService.readCharacteristic(bluetoothService.getAppCharacteristic());
            }else if(BluetoothService.ACTION_CHARACTERISTIC_DATA_READ.equals(action)){
                data = intent.getStringExtra(BluetoothService.CHARACTERISTIC_DATA);
                closeContactActivityTv.setText("Are you sure you want to establish close contact with:" + device.getAddress() + " ?");
                submitBtnCloseContact.setEnabled(true);
            }
        }
    };

    //Establishing a connection for the required service.
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((BluetoothService.LocalBinder) service).getService();
            if(bluetoothService != null){
                if(!bluetoothService.initialize()){
                    UtilityClass.toast(getApplicationContext(),"Unable to initialize bluetooth.");
                    finish();
                }
                //Checking for timeout while attempting connection to the other user.
                connectionHandler.postDelayed(connectionTimeoutOperation, TIMEOUT_LIMIT);
                bluetoothService.connect(device);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_contact);

        //Getting device passed by main activity.
        Intent intent = getIntent();
        device = intent.getExtras().getParcelable("Device");

        submitBtnCloseContact = (Button) findViewById(R.id.submitCloseContactBtn);
        cancelBtnCloseContact = (Button) findViewById(R.id.cancelCloseContactBtn);
        closeContactActivityTv = (TextView) findViewById(R.id.closeContactTv);

        submitBtnCloseContact.setEnabled(false);
        closeContactActivityTv.setText("Please wait app is trying to note down remote user id.");


        submitBtnCloseContact.setOnClickListener(v -> new CloseContactInsert().execute());

        //Disconnecting from the remote device once the user presses cancel.
        cancelBtnCloseContact.setOnClickListener(v -> {
            bluetoothService.disconnect();
            finish();
        });

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        registerReceiver(bluetoothServiceBroadcastReceiver, intentFilter());
    }

    //Operations that take place when the activity finishes.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregisterReceiver(bluetoothServiceBroadcastReceiver);
        bluetoothService = null;
    }

    //Filter set up to differentiate between the broadcasts sent by the service.
    private IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_REQUIRED_CHARACTERISTIC_FOUND);
        intentFilter.addAction(BluetoothService.ACTION_CHARACTERISTIC_DATA_READ);
        return intentFilter;
    }

    //Private class representing the establishment of close contact between 2 users, inserting a new entry into the database contacts table.
    private class CloseContactInsert extends AsyncTask<String, String, String>{

        ProgressDialog progressDialog = new ProgressDialog(CloseContactActivity.this);

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
            super.onPreExecute();
            progressDialog.setTitle("Close Contact Establishment");
            progressDialog.setMessage("Establishing close contact Please Wait...");
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
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preference_key),MODE_PRIVATE);
            String idOfCurrentUser = sharedPreferences.getString("uid",null);
            String idOfRemoteUser = data;
            String closeContactInsertURL = getString(R.string.close_contact_insert_url);

            try {
                URL url = new URL(closeContactInsertURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                //Setting timeouts for server response and connection.
                con.setReadTimeout(15000);
                con.setConnectTimeout(15000);
                //Setting method to send over data.
                con.setRequestMethod("POST");
                con.setDoInput(true);
                con.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder();
                builder.appendQueryParameter("user_x",idOfCurrentUser);
                builder.appendQueryParameter("user_y", idOfRemoteUser);
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
                    String message = jsonResponse.getString("message");

                    //Checking json response in case the operation was successful or not.
                    if(success == 1) {
                        //Sending message to MainActivity to delete device from discovery after the user establishes close contact with them.
                        broadcastToMainActivity(CLOSE_CONTACT_SUCCESS,"connectedDeviceSuccess",device);
                    }else if(success == 0 || message.contains(getString(R.string.error_message_json_response))){
                        //Sending broadcast to MainActivity to delete from the discovery list to which the user is already connected.
                        broadcastToMainActivity(CLOSE_CONTACT_ALREADY_ESTABLISHED,"alreadyConnectedUserDevice",device);
                    }else{
                        broadcastToMainActivity(CLOSE_CONTACT_INTERNAL_SQL_ERROR);
                    }
                    //Disconnecting once the message is received and the broadcast is sent.
                    con.disconnect();
                }else{
                    broadcastToMainActivity(CLOSE_CONTACT_SERVER_CONNECTION_ERROR);
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
            super.onPostExecute(s);
            progressDialog.dismiss();
            //Finish activity after progress dialog is dismissed in order to prevent leaks.
            finish();
        }

        //Method to send broadcasts containing the device for deletion in MainActivity
        private void broadcastToMainActivity(String broadCastMessage, String extraKey, BluetoothDevice device){
            Intent messageToMainActivity = new Intent(broadCastMessage);
            messageToMainActivity.putExtra(extraKey,device);
            sendBroadcast(messageToMainActivity);
        }

        //Method to send broadcasts only in form of messages.
        private void broadcastToMainActivity(String broadCastMessage){
            Intent messageToMainActivity = new Intent(broadCastMessage);
            sendBroadcast(messageToMainActivity);
        }
    }
}