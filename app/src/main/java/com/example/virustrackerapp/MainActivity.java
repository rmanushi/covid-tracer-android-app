package com.example.virustrackerapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    //Components in the main page.
    private Button scanBtn, updateBtn;

    //Components that are going to be shown in the pop up.
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog updateDialog;
    private Button updatePopUpSubmitBtn, updatePopUpCancelBtn;
    private RadioButton trueUpdateInfectionBtn, falseUpdateInfectionBtn, trueUpdateVaccineBtn, falseUpdateVaccineBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanBtn = (Button) findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(v -> {
            if(scanBtn.getText()=="SCAN"){
                scanBtn.setText("STOP");
            }else{
                scanBtn.setText("SCAN");
            }

        });

        updateBtn = (Button) findViewById(R.id.updateBtn);
        updateBtn.setOnClickListener(v -> createUpdateDialog());
    }



    public void createUpdateDialog(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View updatePopUpView = getLayoutInflater().inflate(R.layout.update_activity_popup,null);

        //Setting up submit and cancel buttons to their components in the update layout.
        updatePopUpSubmitBtn = (Button) updatePopUpView.findViewById(R.id.submitUpdateBtn);
        updatePopUpCancelBtn = (Button) updatePopUpView.findViewById(R.id.cancelUpdateBtn);

        //Setting up radio buttons to their components in the update layout.
        trueUpdateInfectionBtn = (RadioButton) updatePopUpView.findViewById(R.id.infectionTrueBtn);
        falseUpdateInfectionBtn = (RadioButton) updatePopUpView.findViewById(R.id.falseInfectionBtn);
        trueUpdateVaccineBtn = (RadioButton) updatePopUpView.findViewById(R.id.vaccineTruerBtn);
        falseUpdateVaccineBtn = (RadioButton) updatePopUpView.findViewById(R.id.vaccineFalseBtn);

        //Setting up update dialog view with new assigned components in the main activity.
        dialogBuilder.setView(updatePopUpView);
        updateDialog = dialogBuilder.create();
        updateDialog.show();

        //Action to take place when new data is added and the submit button is pressed.
        updatePopUpSubmitBtn.setOnClickListener(v -> {
            //The rest is to be defined with the backend to actually update the specific user profile.

            //Checking that user input is valid while selecting new values to update their profile.
            if(!trueUpdateVaccineBtn.isChecked() && !falseUpdateVaccineBtn.isChecked()){
                Toast.makeText(MainActivity.this,"Error: Please select a new value for vaccination in order to submit!",Toast.LENGTH_SHORT).show();
            }else if(!trueUpdateInfectionBtn.isChecked() && !falseUpdateInfectionBtn.isChecked()){
                Toast.makeText(MainActivity.this,"Error: Please select a  new value for infection in order to submit!",Toast.LENGTH_SHORT).show();
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
}