package io.github.patpatchpatrick.nflseasonsim;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;

public class MainActivity extends AppCompatActivity implements SimulatorMvpContract.SimulatorView {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onSimulateWeekButtonClicked() {
        
    }

    @Override
    public void onSimulateSeasonButtonClicked() {

    }
}
