package com.example.virustrackerapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

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
    private int vaccinationValueFromUpdateForm;
    private int infectionValueFromUpdateForm;
    private SharedPreferences sharedPreferences;
    private TextView vaccineTv, infectionTv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preference_key),MODE_PRIVATE);

        vaccineTv = (TextView)findViewById(R.id.vaccineTv);
        infectionTv = (TextView)findViewById(R.id.infectionTv);

        int vaccineValue = sharedPreferences.getInt("vaccine",0);
        int infectionValue = sharedPreferences.getInt("infection",0);

        if(vaccineValue == 1){
            vaccineTv.setText("Fully Vaccinated: True");
        }else{
            vaccineTv.setText("Fully Vaccinated: False");
        }

        if(infectionValue == 1){
            infectionTv.setText("Infected: True");
        }else{
            infectionTv.setText("Infected: False");
        }

        Button backBtn = (Button) findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v->finish());

        Button updateBtn = (Button) findViewById(R.id.updateBtn);
        updateBtn.setOnClickListener(v->createUpdateDialog());

        Button searchBtn = (Button) findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(v-> new InfectionSearch().execute());

        Button deleteBtn = (Button) findViewById(R.id.deleteBtn);

    }

    //Method that creates the update dialog view.
    private void createUpdateDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View updatePopUpView = getLayoutInflater().inflate(R.layout.update_activity_popup,null);

        //Setting up submit and cancel buttons to their components in the update layout.
        Button updatePopUpSubmitBtn = (Button) updatePopUpView.findViewById(R.id.submitRegistrationBtn);
        Button updatePopUpCancelBtn = (Button) updatePopUpView.findViewById(R.id.cancelRegistrationBtn);

        //Setting up radio buttons to their components in the update layout.
        RadioButton trueUpdateInfectionBtn = (RadioButton) updatePopUpView.findViewById(R.id.infectionTrueBtnReg);
        RadioButton falseUpdateInfectionBtn = (RadioButton) updatePopUpView.findViewById(R.id.infectionFalseBtnReg);
        RadioButton trueUpdateVaccineBtn = (RadioButton) updatePopUpView.findViewById(R.id.trueVaccineBtnReg);
        RadioButton falseUpdateVaccineBtn = (RadioButton) updatePopUpView.findViewById(R.id.falseRegistrationVaccineBtn);

        //Setting up update dialog view with new assigned components in the main activity.
        dialogBuilder.setView(updatePopUpView);
        AlertDialog updateDialog = dialogBuilder.create();
        updateDialog.show();

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

        //Action to take place when new data is added and the submit button is pressed.
        updatePopUpSubmitBtn.setOnClickListener(v -> {
            //Checking that user input is valid while selecting new values to update their profile.
            if(!trueUpdateVaccineBtn.isChecked() && !falseUpdateVaccineBtn.isChecked()){
                UtilityClass.toast(this,"Error: Please select a new value for vaccination in order to submit.");
            }else if(!trueUpdateInfectionBtn.isChecked() && !falseUpdateInfectionBtn.isChecked()){
                UtilityClass.toast(this,"Error: Please select a new value for infection in order to submit.");
            }else{
                if(trueUpdateVaccineBtn.isChecked() && trueUpdateInfectionBtn.isChecked()){
                    vaccinationValueFromUpdateForm = 1;
                    infectionValueFromUpdateForm = 1;
                }else if(falseUpdateVaccineBtn.isChecked() && falseUpdateInfectionBtn.isChecked()){
                    vaccinationValueFromUpdateForm = 0;
                    infectionValueFromUpdateForm = 0;
                }else if(trueUpdateVaccineBtn.isChecked() && falseUpdateInfectionBtn.isChecked()){
                    vaccinationValueFromUpdateForm = 1;
                    infectionValueFromUpdateForm = 0;
                }else if(falseUpdateVaccineBtn.isChecked() && trueUpdateInfectionBtn.isChecked()){
                    vaccinationValueFromUpdateForm = 0;
                    infectionValueFromUpdateForm = 1;
                }
                //Operations after values are set by the user input.
                new UpdateProfile().execute();
                updateDialog.dismiss();
                updateTextViews(vaccinationValueFromUpdateForm,infectionValueFromUpdateForm);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("vaccine",vaccinationValueFromUpdateForm);
                editor.putInt("infection",infectionValueFromUpdateForm);
                editor.apply();
            }

        });
        //Action to be performed when cancel button in the pop up is pressed.
        updatePopUpCancelBtn.setOnClickListener(v -> updateDialog.dismiss());
    }

    private void updateTextViews(int status_tv1, int status_tv2){
        if(status_tv1 == 1 && status_tv2 == 1){
            vaccineTv.setText("Fully Vaccinated: True");
            infectionTv.setText("Infected: True");
        }else if(status_tv1 == 0 && status_tv2 == 0){
            vaccineTv.setText("Fully Vaccinated: False");
            infectionTv.setText("Infected: False");
        }else if(status_tv1 == 0 && status_tv2 == 1){
            vaccineTv.setText("Fully Vaccinated: False");
            infectionTv.setText("Infected: True");
        }else if(status_tv1 == 1 && status_tv2 == 0){
            vaccineTv.setText("Fully Vaccinated: True");
            infectionTv.setText("Infected: False");
        }
    }

    //Private class representing a background task sending a request
    // to the webserver to run the select query in order to find out if
    // the user has been in close contact with another infected user.
    private class InfectionSearch extends AsyncTask<String, String, String>{

        ProgressDialog progressDialog = new ProgressDialog(MyProfileActivity.this);

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
            progressDialog.setTitle("Infectious Contact Search");
            progressDialog.setMessage("Search if you have been in contact with infected users, Please Wait...");
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
            sharedPreferences = getSharedPreferences(getString(R.string.shared_preference_key),MODE_PRIVATE);
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
                    thread = new Thread() {
                        public void run() {
                            runOnUiThread(() -> UtilityClass.toast(getApplicationContext(), message));
                        }
                    };
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

    private class UpdateProfile extends AsyncTask<String, String, String>{
        ProgressDialog progressDialog = new ProgressDialog(MyProfileActivity.this);
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
            progressDialog.setTitle("Profile Update");
            progressDialog.setMessage("Updating your profile with the new values, Please Wait...");
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
            sharedPreferences = getSharedPreferences(getString(R.string.shared_preference_key),MODE_PRIVATE);
            String idOfUser = sharedPreferences.getString("uid",null);
            String updateOperationURL = getString(R.string.update_user_profile_url);

            try {
                URL url = new URL(updateOperationURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                //Setting timeouts for server response and connection.
                con.setReadTimeout(15000);
                con.setConnectTimeout(15000);
                //Setting method to send over data.
                con.setRequestMethod("POST");
                con.setDoInput(true);
                con.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder();
                builder.appendQueryParameter("userID",idOfUser);
                builder.appendQueryParameter("isVaccinated", String.valueOf(vaccinationValueFromUpdateForm));
                builder.appendQueryParameter("isInfected", String.valueOf(infectionValueFromUpdateForm));
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
                    thread = new Thread() {
                        public void run() {
                            runOnUiThread(() -> UtilityClass.toast(getApplicationContext(), message));
                        }
                    };
                    thread.start();
                    //Disconnecting once the message is received.
                }else{
                    //Error message to be shown in the UI thread.
                    Thread thread = new Thread(){
                        public void run(){
                            runOnUiThread(() -> UtilityClass.toast(getApplicationContext(),"Profile could not be updated, Internal server error."));
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