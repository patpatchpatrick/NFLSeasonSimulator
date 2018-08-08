package io.github.patpatchpatrick.nflseasonsim;

import android.content.ContentResolver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;

public class EloValuesActivity extends AppCompatActivity {
    //Activity to edit team ELO values

    @Inject
    SimulatorModel mModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elo_values);
    }
}
