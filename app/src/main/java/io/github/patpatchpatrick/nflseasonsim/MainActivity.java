package io.github.patpatchpatrick.nflseasonsim;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

public class MainActivity extends AppCompatActivity implements SimulatorMvpContract.SimulatorView {

    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SimulatorMvpContract.SimulatorPresenter pres = new SimulatorPresenter(this);
        pres.initializeSeason();


    }

    @Override
    public void onSimulateWeekButtonClicked() {

    }

    @Override
    public void onSimulateSeasonButtonClicked() {

    }



}
