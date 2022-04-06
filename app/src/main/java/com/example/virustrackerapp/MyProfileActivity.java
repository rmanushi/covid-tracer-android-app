package com.example.virustrackerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;

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

public class MyProfileActivity extends AppCompatActivity {
    private Button deleteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        Button backBtn = (Button) findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v->finish());

        Button updateBtn = (Button) findViewById(R.id.updateBtn);
        Button searchBtn = (Button) findViewById(R.id.searchBtn);

        searchBtn.setOnClickListener(v-> new InfectionSearch().execute());
    }

    private class InfectionSearch extends AsyncTask<String, String, String>{

        /**
         * Runs on the UI thread before {@link #doInBackground}.
         * Invoked directly by {@link #execute} or {@link #executeOnExecutor}.
         * The default version does nothing.
         *
         * @see #onPostExecute
         * @see #doInBackground
         */
        ProgressDialog progressDialog = new ProgressDialog(MyProfileActivity.this);

        @Override
        protected void onPreExecute() {
            progressDialog.setTitle("Infectious Contact Search");
            progressDialog.setMessage("Search if you have been in contact with infected users, Please Wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
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
            String idOfUser = sharedPreferences.getString("uid",null);
            String selectOperationURL = getString(R.string.infection_contact_search_url);

            try {
                URL url = new URL(selectOperationURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                //Setting timeouts for server response and connection.
                con.setReadTimeout(15000);
                con.setConnectTimeout(15000);
                //Setting method to send over data.
                con.setRequestMethod("POST");
                con.setDoInput(true);
                con.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder().appendQueryParameter("userID", idOfUser);
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
                    Thread thread; //New thread for displaying messages to the UI.
                    if(success == 1) {
                        //Success message shown on the UI thread.
                        thread = new Thread() {
                            public void run() {
                                runOnUiThread(() -> UtilityClass.toast(getApplicationContext(), message));
                            }
                        };
                    }else{
                        //Error message to be shown in the UI thread.
                        thread = new Thread() {
                            public void run() {
                                runOnUiThread(() -> UtilityClass.toast(getApplicationContext(), message));
                            }
                        };
                    }
                    thread.start();
                    //Disconnecting once the message is received.
                }else{
                    //Error message to be shown in the UI thread.
                    Thread thread = new Thread(){
                        public void run(){
                            runOnUiThread(() -> UtilityClass.toast(getApplicationContext(),"Internal server error."));
                        }
                    };
                    thread.start();
                }
                con.disconnect();
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
        }
    }
}